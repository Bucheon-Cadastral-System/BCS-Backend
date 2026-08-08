-- 새 볼륨을 만들 때 한 번만 도는 스크립트(/docker-entrypoint-initdb.d).
-- auto_explain 은 라이브러리만 올리면 되지만 pg_stat_statements 는 뷰·함수를 만드는 확장이라
-- shared_preload_libraries 에 올리는 것과 별개로 데이터베이스마다 한 번 만들어 주어야 한다.
--
-- 앱 스키마(bcs)는 하이버네이트가 기동할 때 만들므로 이 시점에는 없다. public 에 둔다.
create extension if not exists pg_stat_statements schema public;
