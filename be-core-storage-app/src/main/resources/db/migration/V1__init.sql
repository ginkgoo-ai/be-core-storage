-- Initialize Database Settings
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

------------------------------------------
-- Core Cloud file Tables
------------------------------------------
DROP TABLE IF EXISTS cloud_file;
-- 建表语句
CREATE TABLE cloud_file (
                            id VARCHAR(36) PRIMARY KEY NOT NULL,
                            original_name VARCHAR(255) NOT NULL,
                            storage_name VARCHAR(255) NOT NULL UNIQUE,
                            file_type VARCHAR(50) NOT NULL,
                            file_size BIGINT NOT NULL,
                            storage_path TEXT,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP WITH TIME ZONE,
                            uploader_id BIGINT,
                            bucket_name VARCHAR(100) NOT NULL,
                            is_deleted BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX idx_cloud_file_bucket ON cloud_file USING brin (bucket_name);
CREATE INDEX idx_cloud_file_uploader ON cloud_file USING hash (uploader_id);
CREATE INDEX idx_cloud_file_created ON cloud_file USING btree (created_at DESC);