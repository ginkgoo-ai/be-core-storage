-- add video columns to cloud_file table
ALTER TABLE storage.cloud_file
    ADD COLUMN video_thumbnail_id  VARCHAR(255),
    ADD COLUMN video_thumbnail_url VARCHAR(255),
    ADD COLUMN video_duration      BIGINT,
    ADD COLUMN video_resolution    VARCHAR(50);