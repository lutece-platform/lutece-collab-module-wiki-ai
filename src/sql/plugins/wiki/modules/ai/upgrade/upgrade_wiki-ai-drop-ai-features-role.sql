-- Drop the wiki_ai_features role.
--
-- The editor AI features run the model on text posted by the client and touch no wiki item. Any
-- signed-in user already reaches the same model through the chat, on the same daily quota, so the
-- role guarded nothing and is removed along with the assignments that carried it.
--
-- The WIKI_AI_FEATURES back-office right, which governs the AI feature management screen, is a
-- different grant and is left untouched.
--
-- This script is idempotent. The mylutece_users_userrole statement requires module-mylutece-users.

DELETE FROM mylutece_users_userrole WHERE role_key = 'wiki_ai_features';

DELETE FROM core_role WHERE role = 'wiki_ai_features';
