-- comment 테이블에 report_count 컬럼 추가
ALTER TABLE comment
    ADD COLUMN report_count INT NOT NULL DEFAULT 0;

-- comment_report 테이블 생성
CREATE TABLE comment_report
(
    comment_report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id        BIGINT       NOT NULL,
    reporter_id       BINARY(16)   NOT NULL,
    reason            VARCHAR(255) NOT NULL DEFAULT 'BAD_CONTENT',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (comment_id) REFERENCES comment (comment_id),
    FOREIGN KEY (reporter_id) References member (member_id),

    -- 동일한 사용자가 동일한 댓글을 중복 신고하는 것을 방지하는 Unique 제약 조건
    CONSTRAINT uq_cr_comment_reporter UNIQUE (comment_id, reporter_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- post_report 테이블 생성
CREATE TABLE post_report
(
    post_report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id        BIGINT       NOT NULL,
    reporter_id    BINARY(16)   NOT NULL,
    reason         VARCHAR(255) NOT NULL DEFAULT 'BAD_CONTENT',
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (post_id) REFERENCES post (post_id),
    FOREIGN KEY (reporter_id) References member (member_id),

    -- 동일한 사용자가 동일한 포스트를 중복 신고하는 것을 방지하는 Unique 제약 조건
    CONSTRAINT uq_cr_post_reporter UNIQUE (post_id, reporter_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;