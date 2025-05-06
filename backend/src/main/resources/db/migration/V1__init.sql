CREATE TABLE member (
  member_id BINARY(16) PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(255) NOT NULL,
  birth_date DATE NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_member_email UNIQUE (email),
  CONSTRAINT uq_member_nickname UNIQUE (nickname)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE keyword (
  keyword_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  text VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_keyword_text UNIQUE (text)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE post (
  post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  summary TEXT NOT NULL,
  thumbnail_url TEXT,
  bucket_at DATETIME NOT NULL,
  report_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE keyword_metric_hourly (
  bucket_at DATETIME NOT NULL,
  platform VARCHAR(30) NOT NULL,
  keyword_id BIGINT NOT NULL,
  post_id BIGINT,
  volume INT NOT NULL DEFAULT 0,
  score INT NOT NULL DEFAULT 0,
  rank_delta DOUBLE DEFAULT 0.0,
  novelty_ratio DOUBLE DEFAULT -1.0,
  weighted_novelty DOUBLE DEFAULT -1.0,
  no_post_streak INT DEFAULT -1,
  low_variation BOOLEAN DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (bucket_at, platform, keyword_id),
  FOREIGN KEY (keyword_id) REFERENCES keyword(keyword_id)
      ON DELETE CASCADE,
  FOREIGN KEY (post_id) REFERENCES post(post_id)
      ON DELETE SET NULL,
  INDEX idx_kmh_bucket_at_score (bucket_at, score)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE post_keyword (
  post_keyword_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  keyword_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_pk_post_keyword UNIQUE (post_id, keyword_id),
  FOREIGN KEY (post_id) REFERENCES post(post_id)
      ON DELETE CASCADE,
  FOREIGN KEY (keyword_id) REFERENCES keyword(keyword_id)
      ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE post_metric_hourly (
  post_metric_hourly_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  bucket_at DATETIME NOT NULL,
  click_count INT NOT NULL DEFAULT 0,
  like_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_pmh_post_bucket UNIQUE (post_id, bucket_at),
  FOREIGN KEY (post_id) REFERENCES post(post_id)
      ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE source (
  fingerprint VARCHAR(64) PRIMARY KEY,
  normalized_url VARCHAR(500) NOT NULL,
  title VARCHAR(255),
  description TEXT,
  thumbnail_url TEXT,
  published_at DATETIME NOT NULL,
  platform VARCHAR(30) NOT NULL,
  video_id VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_source_platform_published (platform, published_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE keyword_source (
  keyword_source_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  keyword_id BIGINT NOT NULL,
  fingerprint VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_ps_keyword_fingerprint UNIQUE (keyword_id, fingerprint),
  FOREIGN KEY (keyword_id) REFERENCES keyword(keyword_id)
      ON DELETE CASCADE,
  FOREIGN KEY (fingerprint) REFERENCES source(fingerprint)
      ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE post_source (
  post_source_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  fingerprint VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_ps_post_fingerprint UNIQUE (post_id, fingerprint),
  FOREIGN KEY (post_id) REFERENCES post(post_id)
      ON DELETE CASCADE,
  FOREIGN KEY (fingerprint) REFERENCES source(fingerprint)
      ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE comment (
  comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  member_id BINARY(16) NOT NULL,
  body TEXT NOT NULL,
  like_count INT NOT NULL DEFAULT 0,
  is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (post_id) REFERENCES post(post_id)
      ON DELETE CASCADE,
  FOREIGN KEY (member_id) REFERENCES member(member_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE comment_like (
  comment_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  comment_id BIGINT NOT NULL,
  member_id BINARY(16) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_cl_comment_member UNIQUE (comment_id, member_id),
  FOREIGN KEY (comment_id) REFERENCES comment(comment_id)
      ON DELETE CASCADE,
  FOREIGN KEY (member_id) REFERENCES member(member_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
