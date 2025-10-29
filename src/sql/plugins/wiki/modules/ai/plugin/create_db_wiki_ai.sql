--liquibase formatted sql
--changeset wiki-ai:create_db_wiki_ai.sql
--preconditions onFail:MARK_RAN onError:WARN

DROP TABLE IF EXISTS wiki_ai_feature;
CREATE TABLE wiki_ai_feature (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    prompt_template TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    order_num INT DEFAULT 0,
    display_mode VARCHAR(20) DEFAULT 'dropdown'
);

DROP TABLE IF EXISTS wiki_ai_user_rate_limit;
CREATE TABLE wiki_ai_user_rate_limit (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    message_count INT NOT NULL DEFAULT 0,
    date_first_message TIMESTAMP NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_date_first_message (date_first_message),
    UNIQUE KEY uk_user_id (user_id)
);

-- Quiz Generation Workflow table
CREATE TABLE wiki_ai_quiz_generation_workflow (
    id_workflow INT AUTO_INCREMENT PRIMARY KEY,
    id_quiz INT NOT NULL,
    id_user VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    selected_page_ids LONG VARCHAR,
    error_message LONG VARCHAR,
    total_pages INT DEFAULT 0,
    processed_pages INT DEFAULT 0,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_completion TIMESTAMP NULL,
    locale VARCHAR(10) DEFAULT 'en'
);

CREATE INDEX idx_workflow_status ON wiki_ai_quiz_generation_workflow(status);
CREATE INDEX idx_workflow_quiz ON wiki_ai_quiz_generation_workflow(id_quiz);

-- Quiz Generated Question table
CREATE TABLE wiki_ai_quiz_generated_question (
    id_question INT AUTO_INCREMENT PRIMARY KEY,
    id_workflow INT NOT NULL,
    id_source_page INT NOT NULL,
    question_text LONG VARCHAR NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    explanation LONG VARCHAR,
    is_imported SMALLINT DEFAULT 0,
    is_discarded SMALLINT DEFAULT 0,
    FOREIGN KEY (id_workflow) REFERENCES wiki_ai_quiz_generation_workflow(id_workflow) ON DELETE CASCADE
);

CREATE INDEX idx_question_workflow ON wiki_ai_quiz_generated_question(id_workflow);

-- Quiz Generated Answer table
CREATE TABLE wiki_ai_quiz_generated_answer (
    id_answer INT AUTO_INCREMENT PRIMARY KEY,
    id_question INT NOT NULL,
    answer_text LONG VARCHAR NOT NULL,
    is_correct SMALLINT DEFAULT 0,
    match_target VARCHAR(1000),
    correct_order INT,
    display_order INT DEFAULT 0,
    FOREIGN KEY (id_question) REFERENCES wiki_ai_quiz_generated_question(id_question) ON DELETE CASCADE
);

CREATE INDEX idx_generated_answer_question ON wiki_ai_quiz_generated_answer(id_question);

-- Indexer Action table for incremental AI indexation
DROP TABLE IF EXISTS wiki_ai_indexer_action;
CREATE TABLE wiki_ai_indexer_action (
    id_action INT AUTO_INCREMENT PRIMARY KEY,
    id_document VARCHAR(255) NOT NULL,
    id_task INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_wiki_ai_indexer_action_task ON wiki_ai_indexer_action(id_task);
