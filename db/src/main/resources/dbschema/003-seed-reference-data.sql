--liquibase formatted sql

--changeset auciellos:003-seed-reference-data
--comment: Popolamento dati iniziali di sistema (ruoli standard, utenti di test e assegnazioni ruoli)
INSERT INTO roles (id, name, description) VALUES (1, 'ROLE_ADMIN', 'Amministratore di sistema con privilegi completi');
INSERT INTO roles (id, name, description) VALUES (2, 'ROLE_DEVELOPER', 'Sviluppatore con permessi di staging e monitoring');
INSERT INTO roles (id, name, description) VALUES (3, 'ROLE_USER', 'Utente standard con accesso ai servizi applicativi');

INSERT INTO users (id, username, email, first_name, last_name, status) VALUES (1, 'alovelace', 'ada.lovelace@example.com', 'Ada', 'Lovelace', 'ACTIVE');
INSERT INTO users (id, username, email, first_name, last_name, status) VALUES (2, 'aturing', 'alan.turing@example.com', 'Alan', 'Turing', 'ACTIVE');
INSERT INTO users (id, username, email, first_name, last_name, status) VALUES (3, 'ghopper', 'grace.hopper@example.com', 'Grace', 'Hopper', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);
INSERT INTO user_roles (user_id, role_id) VALUES (2, 2);
INSERT INTO user_roles (user_id, role_id) VALUES (3, 3);
--rollback DELETE FROM user_roles WHERE user_id IN (1, 2, 3);
--rollback DELETE FROM users WHERE id IN (1, 2, 3);
--rollback DELETE FROM roles WHERE id IN (1, 2, 3);
