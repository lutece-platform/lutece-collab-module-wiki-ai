--liquibase formatted sql
--changeset wiki-ai:init_db_wiki_ai.sql
--preconditions onFail:MARK_RAN onError:WARN

--
-- Data for table core_admin_right
--
DELETE FROM core_admin_right WHERE id_right = 'WIKI_AI_MANAGEMENT';
INSERT INTO core_admin_right (id_right, name, level_right, admin_url, description, is_updatable, plugin_name, id_feature_group, icon_url, documentation_url, id_order) VALUES
('WIKI_AI_MANAGEMENT', 'module.wiki.ai.adminFeature.pageTitle', 1, 'jsp/admin/plugins/wiki/modules/ai/ManageIndexation.jsp', 'module.wiki.ai.adminFeature.description', 0, 'wiki-ai', NULL, NULL, NULL, 4);

DELETE FROM core_admin_right WHERE id_right = 'WIKI_AI_FEATURES';
INSERT INTO core_admin_right (id_right, name, level_right, admin_url, description, is_updatable, plugin_name, id_feature_group, icon_url, documentation_url, id_order) VALUES
('WIKI_AI_FEATURES', 'module.wiki.ai.manage_features.pageTitle', 1, 'jsp/admin/plugins/wiki/modules/ai/ManageAiFeatures.jsp', 'module.wiki.ai.adminFeature.features.description', 0, 'wiki-ai', NULL, NULL, NULL, 5);

--
-- Data for table core_user_right
--
DELETE FROM core_user_right WHERE id_right = 'WIKI_AI_MANAGEMENT';
INSERT INTO core_user_right (id_right, id_user) VALUES ('WIKI_AI_MANAGEMENT', 1);

DELETE FROM core_user_right WHERE id_right = 'WIKI_AI_FEATURES';
INSERT INTO core_user_right (id_right, id_user) VALUES ('WIKI_AI_FEATURES', 1);

--
-- Data for table wiki_ai_feature
--
DELETE FROM wiki_ai_feature;
INSERT INTO wiki_ai_feature (name, type, prompt_template, is_active, order_num, display_mode) VALUES
('Résumer', 'replace', 'Résume le texte suivant de manière concise:\n\n{{text}}', TRUE, 1, 'dropdown'),
('Corriger', 'replace', 'Corrige les fautes d''orthographe et de grammaire dans le texte suivant:\n\n{{text}}', TRUE, 2, 'dropdown'),
('Traduire', 'replace', 'Traduis le texte suivant en anglais:\n\n{{text}}', TRUE, 3, 'dropdown'),
('Améliorer', 'replace', 'Améliore le style et la clarté du texte suivant:\n\n{{text}}', TRUE, 4, 'dropdown'),
('Continuer', 'insertAfter', 'Continue le texte suivant de manière cohérente:\n\n{{text}}', TRUE, 5, 'dropdown'),
('Générer un titre', 'insertBefore', 'Génère un titre pertinent pour le texte suivant:\n\n{{text}}', TRUE, 6, 'dropdown');
