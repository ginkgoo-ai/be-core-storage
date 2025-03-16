-- Initialize Database Settings
------------------------------------------
DROP TABLE IF EXISTS storage.cloud_file;
-- 建表语句
CREATE TABLE storage.cloud_file (
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

CREATE INDEX idx_cloud_file_bucket ON storage.cloud_file USING brin (bucket_name);
CREATE INDEX idx_cloud_file_uploader ON storage.cloud_file USING hash (uploader_id);
CREATE INDEX idx_cloud_file_created ON storage.cloud_file USING btree (created_at DESC);