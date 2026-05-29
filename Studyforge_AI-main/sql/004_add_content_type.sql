-- Content type system migration
-- Adds content_type column to posts table
-- Supported types: GUIDE, NOTE, REVIEW, QUESTION, RESOURCE, ROADMAP, MOMENT

USE studyforge_ai;

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(30) NOT NULL DEFAULT 'GUIDE' AFTER category_id;

CREATE INDEX idx_posts_content_type ON posts (content_type, status);
