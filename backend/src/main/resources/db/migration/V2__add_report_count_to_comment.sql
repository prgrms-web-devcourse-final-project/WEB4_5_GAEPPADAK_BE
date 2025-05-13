ALTER TABLE comment
ADD COLUMN report_count INT NOT NULL DEFAULT 0 AFTER deleted_at;