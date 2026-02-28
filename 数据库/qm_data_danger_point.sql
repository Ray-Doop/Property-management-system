/*
 Navicat Premium Data Transfer

 Source Server         : localhost_5433
 Source Server Type    : PostgreSQL
 Source Server Version : 140020 (140020)
 Source Host           : localhost:5432
 Source Catalog        : yunxunhu
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 140020 (140020)
 File Encoding         : 65001

 Date: 08/01/2026 11:47:14
*/


-- ----------------------------
-- Table structure for qm_data_danger_point
-- ----------------------------
DROP TABLE IF EXISTS "public"."qm_data_danger_point";
CREATE TABLE "public"."qm_data_danger_point" (
  "gid" int4 NOT NULL DEFAULT nextval('danger_gid_seq'::regclass),
  "__gid" float8,
  "id" float8,
  "sszj_name" varchar(254) COLLATE "pg_catalog"."default",
  "lz_dept_id" float8,
  "cm_name" varchar(254) COLLATE "pg_catalog"."default",
  "name" varchar(254) COLLATE "pg_catalog"."default",
  "point_type" varchar(254) COLLATE "pg_catalog"."default",
  "operate_type" varchar(254) COLLATE "pg_catalog"."default",
  "area_times" varchar(254) COLLATE "pg_catalog"."default",
  "staff_size" varchar(254) COLLATE "pg_catalog"."default",
  "address" varchar(254) COLLATE "pg_catalog"."default",
  "danger" varchar(254) COLLATE "pg_catalog"."default",
  "duty_user" varchar(254) COLLATE "pg_catalog"."default",
  "phonenumber" varchar(254) COLLATE "pg_catalog"."default",
  "notes" varchar(254) COLLATE "pg_catalog"."default",
  "del_flag" float8,
  "lng_lat" varchar(254) COLLATE "pg_catalog"."default",
  "lat" float8,
  "lng" float8,
  "dept_id" int8,
  "wg_id" varchar(254) COLLATE "pg_catalog"."default",
  "geom" geometry(POINT, 4326),
  "no_id" int4,
  "risk_level" varchar(254) COLLATE "pg_catalog"."default",
  "type" int4
)
;
COMMENT ON COLUMN "public"."qm_data_danger_point"."name" IS '名字';
COMMENT ON COLUMN "public"."qm_data_danger_point"."lat" IS '1';
COMMENT ON COLUMN "public"."qm_data_danger_point"."lng" IS '1';
COMMENT ON COLUMN "public"."qm_data_danger_point"."dept_id" IS '1';
COMMENT ON COLUMN "public"."qm_data_danger_point"."wg_id" IS '1';
COMMENT ON COLUMN "public"."qm_data_danger_point"."geom" IS '1';
COMMENT ON COLUMN "public"."qm_data_danger_point"."no_id" IS '1';
COMMENT ON COLUMN "public"."qm_data_danger_point"."type" IS '110：低风险，1：中风险，2：高风险';
