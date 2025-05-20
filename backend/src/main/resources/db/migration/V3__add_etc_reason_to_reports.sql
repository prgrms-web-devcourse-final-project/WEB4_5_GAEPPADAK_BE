-- comment_report 테이블에 etc_reason 컬럼 추가
ALTER TABLE comment_report
    ADD COLUMN etc_reason TEXT NULL;

-- post_report 테이블에 etc_reason 컬럼 추가
ALTER TABLE post_report
    ADD COLUMN etc_reason TEXT NULL;