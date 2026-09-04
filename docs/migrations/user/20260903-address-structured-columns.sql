ALTER TABLE p_address
  ADD COLUMN road_address   VARCHAR(255) NULL,
  ADD COLUMN jibun_address  VARCHAR(255) NULL,
  ADD COLUMN detail_address VARCHAR(100) NULL,
  ADD COLUMN sido           VARCHAR(40)  NULL,
  ADD COLUMN sigungu        VARCHAR(40)  NULL;
