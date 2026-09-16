--liquibase formatted sql

--changeset auciellos:004-create-audit-logs-table
--comment: Creazione della tabella audit_logs per tracciamento eventi e sicurezza con indice dedicato
CREATE TABLE audit_logs (
    id BIGINT NOT NULL,
    user_id BIGINT,
    action VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    details VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
--rollback DROP TABLE audit_logs;
