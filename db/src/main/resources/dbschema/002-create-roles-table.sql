--liquibase formatted sql

--changeset auciellos:002-create-roles-and-permissions
--comment: Creazione tabella ruoli e tabella di associazione Many-to-Many user_roles
CREATE TABLE roles (
    id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);
--rollback DROP TABLE user_roles;
--rollback DROP TABLE roles;
