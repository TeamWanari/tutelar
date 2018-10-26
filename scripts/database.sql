CREATE TABLE IF NOT EXISTS USERS (
  id         VARCHAR(36) NOT NULL PRIMARY KEY,
  created_at BIGINT      NOT NULL
);

CREATE TABLE IF NOT EXISTS ACCOUNTS (
  auth_type   VARCHAR(20)   NOT NULL,
  external_id VARCHAR(1000) NOT NULL,
  user_id     VARCHAR(36)   NOT NULL,
  custom_data TEXT          NOT NULL,
  PRIMARY KEY(auth_type, external_id)
);
