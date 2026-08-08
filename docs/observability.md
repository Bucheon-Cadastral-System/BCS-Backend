# 성능 관측

느려졌을 때 "어디가" 느린지 답할 수 있게 해 두는 장치들이다. 층위가 셋이고, 각각 답하는 질문이 다르다.

| 층위 | 도구 | 답하는 질문 |
| --- | --- | --- |
| 엔드포인트 | Actuator + Micrometer (`http.server.requests`) | 어느 API 가 느린가, 얼마나 자주 불리는가 |
| DB 문장(누적) | `pg_stat_statements` | 어느 SQL 이 총 시간을 가장 많이 먹었는가 |
| DB 문장(개별) | `auto_explain` | 그 느린 실행이 어떤 계획으로 돌았는가 |

개발 중에 한 문장을 재는 것은 이 셋과 별개다. `EXPLAIN (ANALYZE, BUFFERS)` 로 계획을 보고,
JPA 경로가 몇 문장을 내보내는지는 하이버네이트 `Statistics.getPrepareStatementCount()` 로 못을 박는다
(예: `SurveyPersistenceAdapterTest.findRecords_doesNotQueryPerRow`). 문장 수는 결정적이라 CI 에서
단언할 수 있지만 시간은 머신마다 흔들려 단언할 수 없다. 회귀를 막는 것은 문장 수 쪽이다.

## 1. 앱 지표

관리 엔드포인트는 서비스 포트(8080)에서 갈라 **8081** 에 띄운다. compose 에서 8081 을 publish 하지
않으므로 바깥에서는 닿지 않고, 같은 도커 네트워크에 있는 수집기만 긁는다. 인증을 걸지 않는 것도 그래서다 —
Prometheus 는 우리 JWT 를 붙일 수단이 없고, 그것 하나만 열어 주면 애초에 권한을 건 의미가 없어진다.
`SecurityConfig.managementSecurityFilterChain` 이 이 경계를 잡고 있고, 본 체인을 나중에 닫을 때
함께 손볼 필요는 없다.

로컬에서 확인:

```bash
./gradlew bootRun
curl -s localhost:8081/actuator/health
curl -s localhost:8081/actuator/metrics/http.server.requests
curl -s localhost:8081/actuator/prometheus | grep last-survey
```

`8080/actuator/**` 는 404 다. 관리 엔드포인트는 그 포트에 아예 붙지 않는다.

### 수집기 붙이기

기본으로는 뜨지 않는 프로필로 넣어 두었다.

```bash
docker compose --profile obs up -d prometheus   # http://localhost:9090
```

`docker/prometheus/prometheus.yml` 이 `host.docker.internal:8081` 을 긁는다. 앱이 `bootRun` 으로
호스트에서 도는 것을 전제로 한 설정이다.

### PromQL

`http.server.requests` 의 URI 태그는 실제 경로가 아니라 **경로 틀**(`/api/control-points/{pointId}/last-survey`)
이라 시계열 수가 라우트 수로 묶인다. 점 id 마다 시계열이 생기지 않는다.

```promql
# 엔드포인트별 p95 (최근 5분)
histogram_quantile(0.95,
  sum by (uri, le) (rate(http_server_requests_seconds_bucket{application="bcs"}[5m])))

# 엔드포인트별 초당 요청 수
sum by (uri) (rate(http_server_requests_seconds_count{application="bcs"}[5m]))

# 엔드포인트별 평균 지연
  sum by (uri) (rate(http_server_requests_seconds_sum{application="bcs"}[5m]))
/ sum by (uri) (rate(http_server_requests_seconds_count{application="bcs"}[5m]))

# 오류율
  sum(rate(http_server_requests_seconds_count{application="bcs", outcome="SERVER_ERROR"}[5m]))
/ sum(rate(http_server_requests_seconds_count{application="bcs"}[5m]))

# 1초 안에 답한 비율 — slo 로 박아 둔 경계라 버킷이 정확히 그 값에 있다
  sum(rate(http_server_requests_seconds_bucket{application="bcs", le="1.0"}[5m]))
/ sum(rate(http_server_requests_seconds_count{application="bcs"}[5m]))
```

분위수를 앱에서 미리 계산해 내보내지 않는다(`distribution.percentiles` 를 쓰지 않는다). 이유가 둘인데,
하나는 그 값이 인스턴스마다 따로 계산된 것이라 여러 대를 합칠 수 없다는 것이고, 다른 하나는 Prometheus
노출 형식상 히스토그램과 분위수가 한 지표에 같이 실리지 못한다는 것이다 — 히스토그램을 켜면 분위수 쪽이
조용히 버려진다. 대신 버킷째로 내보내고 수집기에서 `histogram_quantile` 로 센다.

수집기 없이 `/actuator/metrics/http.server.requests` 를 직접 열면 호출 수·누적 시간·최댓값만 나온다.
그 순간의 누적값이라 추이는 보이지 않는다.

## 2. DB — pg_stat_statements

`shared_preload_libraries` 는 기동할 때만 읽히므로 나중에 붙일 수 없다. compose 의 `command` 에 박아 두었다.

확장은 라이브러리와 별개로 데이터베이스마다 한 번 만들어야 한다. 새 볼륨은
`docker/postgres/initdb/01-observability.sql` 이 처리하지만, **이미 만들어진 볼륨에는 한 번 직접 실행**해야 한다:

```bash
docker compose exec -T postgres \
  psql -U postgres_admin -d postgres -c 'create extension if not exists pg_stat_statements schema public;'
```

자주 쓰는 조회:

```sql
-- 총 시간을 많이 먹은 순 — 개별로는 빨라도 자주 불리는 문장이 여기서 잡힌다
select calls,
       round(total_exec_time::numeric, 1) as total_ms,
       round(mean_exec_time::numeric, 3)  as mean_ms,
       round(max_exec_time::numeric, 1)   as max_ms,
       rows,
       query
from pg_stat_statements
order by total_exec_time desc
limit 20;

-- 한 번이 느린 순
select calls, round(mean_exec_time::numeric, 3) as mean_ms, query
from pg_stat_statements
where calls > 10
order by mean_exec_time desc
limit 20;

-- 버퍼를 많이 만지는 순 — 시간보다 안정적인 지표다(캐시 상태·머신 부하에 안 흔들린다)
select calls, shared_blks_hit, shared_blks_read, query
from pg_stat_statements
order by shared_blks_hit + shared_blks_read desc
limit 20;

-- 창을 새로 연다(배포 직후·부하 시험 직전에)
select pg_stat_statements_reset();
```

문장은 상수가 `$1` 로 정규화되어 묶인다. `where id = 1` 과 `where id = 2` 는 한 줄로 쌓인다.

`hibernate.use_sql_comments: true` 를 켜 둔 덕에 JPQL 이 SQL 앞의 주석으로 함께 남는다. 즉
`pg_stat_statements` 에 잡힌 문장을 어느 리포지터리 메서드가 보낸 것인지 바로 되짚을 수 있다:

```
/* select r from SurveyRecordJpaEntity r where r.id.pointId = :pointId ... */ select ...
```

## 3. DB — auto_explain

임계값(`auto_explain.log_min_duration=200ms`)을 넘긴 문장의 실행 계획을 서버 로그에 남긴다.
우리 엔드포인트는 한 자릿수 ms 라 200ms 를 넘는 것은 충분히 드물다.

```bash
docker compose logs -f postgres | grep -A 20 "duration:"
```

남는 모양:

```
LOG:  duration: 1058.631 ms  plan:
        Query Text: select count(*) from generate_series(1, 8000000) g;
        Aggregate  (cost=100000.00..100000.01 rows=1 width=8) (actual rows=1 loops=1)
          Buffers: temp read=13672 written=13672
          ->  Function Scan on generate_series g  (actual rows=8000000 loops=1)
```

`log_timing=off` 로 둔 것은 노드별 시각 측정이 느린 문장만이 아니라 **모든** 문장에 붙는 비용이기 때문이다.
총 소요 시간과 실제 행 수, 버퍼 수는 이것 없이도 나온다. 노드별 시간까지 봐야 할 때만 임시로 켠다:

```bash
docker compose exec -T postgres psql -U postgres_admin -d postgres \
  -c "alter system set auto_explain.log_timing = on" -c "select pg_reload_conf()"
```

`log_min_duration` 도 같은 방법으로 임시로 낮출 수 있다. `shared_preload_libraries` 와 달리 이 둘은
재기동 없이 바뀐다. 확인을 마치면 되돌릴 것(`alter system reset ...` 후 `pg_reload_conf()`).

## 4. 운영 서버 적용

운영 compose 는 이 리포가 아니라 서버의 `~/docker/main/docker-compose.yml` 에 있다. 아래를 반영한다.

```yaml
services:
  bcs-api:
    # 8081(관리 포트)은 publish 하지 않는다. 같은 네트워크의 prometheus 만 닿으면 된다
    ports:
      - "8080:8080"

  postgres:
    command:
      - postgres
      - -c
      - shared_preload_libraries=pg_stat_statements,auto_explain
      - -c
      - pg_stat_statements.max=5000
      - -c
      - pg_stat_statements.track=top
      - -c
      - auto_explain.log_min_duration=200ms
      - -c
      - auto_explain.log_analyze=on
      - -c
      - auto_explain.log_buffers=on
      - -c
      - auto_explain.log_timing=off
      - -c
      - auto_explain.log_nested_statements=on

  prometheus:
    image: prom/prometheus:v3.13.2
    restart: unless-stopped
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    # 대시보드를 바깥에 열지 않는다. 볼 때는 ssh -L 9090:localhost:9090 으로 터널을 판다
    ports:
      - "127.0.0.1:9090:9090"

volumes:
  prometheus_data:
```

같은 자리에 둘 `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: bcs
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["bcs-api:8081"]
        labels:
          env: prod
```

`postgres` 의 `command` 를 바꾸면 DB 가 재기동한다. 배포 창을 잡고 반영할 것. 확장 만들기는
위 2절의 한 줄을 운영 DB 에도 한 번 실행해야 한다(볼륨이 이미 있으므로 init 스크립트는 돌지 않는다).

## 남겨 둔 것

- `/actuator/info` 는 노출 목록에서 뺐다. 배포된 빌드를 확인하고 싶으면 `springBoot { buildInfo() }` 를
  켜고 `info` 를 목록에 되돌린다. 빌드 시각이 산출물에 들어가 도커 레이어 캐시가 매번 깨지는 대가가 있다.
- 경보(alert)는 넣지 않았다. 볼 사람이 정해지고 기준선이 쌓인 뒤에 붙이는 편이 낫다.
- Grafana 도 넣지 않았다. Prometheus 자체 그래프로도 위 질의는 그려진다.
