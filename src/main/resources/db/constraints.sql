-- 동시성 안전망(테이블 간 제약) — 경합이 제어를 뚫어도 틀린 모양의 데이터가 저장되지 못하게 DB 가 최종 거부한다.
-- 연관관계 대신 Long 컬럼을 쓰는 설계라 어노테이션에는 외래키를 선언할 자리가 없어, ddl-auto 가 만든 스키마 위에
-- 이 스크립트를 SchemaConstraintInitializer 가 기동 시 실행한다(설정 파일이 아니라 코드에 매어 둔다 — application.yml 은 버전 관리에서 빠져 있다).
-- 매 기동마다 다시 실행되므로 DROP IF EXISTS 로 멱등을 지킨다 — ddl-auto 를 validate 로 바꿔도 그대로 동작한다.

-- 대상은 프로젝트에 딸린 데이터 — 프로젝트가 사라지면 함께 사라진다(서비스의 명시 삭제를 DB 가 보증).
alter table bcs.survey_targets drop constraint if exists fk_survey_targets_project;
alter table bcs.survey_targets add constraint fk_survey_targets_project
    foreign key (project_id) references bcs.survey_projects (id) on delete cascade;

-- 조사가 참조 중인 기준점은 지울 수 없다 — 화면의 사전 확인(usage)이 놓친 경합 창을 닫는다.
alter table bcs.survey_targets drop constraint if exists fk_survey_targets_point;
alter table bcs.survey_targets add constraint fk_survey_targets_point
    foreign key (point_id) references bcs.control_points (id);

-- 기록은 대상으로 지정한 점에만 존재한다 — 재지정과 엇갈린 기록은 거부되거나(삽입) 함께 지워진다(대상 삭제).
-- 프로젝트 존재는 대상을 거쳐 전이적으로 보증되므로 기록→프로젝트 외래키는 따로 두지 않는다.
alter table bcs.survey_records drop constraint if exists fk_survey_records_target;
alter table bcs.survey_records add constraint fk_survey_records_target
    foreign key (project_id, point_id) references bcs.survey_targets (project_id, point_id) on delete cascade;

-- 사람 참조 — 회원이 지워져도 업무 데이터(프로젝트·기록·성과)는 남고 사람 칸만 비운다.
alter table bcs.survey_projects drop constraint if exists fk_survey_projects_author;
alter table bcs.survey_projects add constraint fk_survey_projects_author
    foreign key (author_id) references bcs.members (id) on delete set null;

alter table bcs.survey_records drop constraint if exists fk_survey_records_surveyed_by;
alter table bcs.survey_records add constraint fk_survey_records_surveyed_by
    foreign key (surveyed_by) references bcs.members (id) on delete set null;

alter table bcs.control_points drop constraint if exists fk_control_points_last_surveyed_by;
alter table bcs.control_points add constraint fk_control_points_last_surveyed_by
    foreign key (last_surveyed_by) references bcs.members (id) on delete set null;
