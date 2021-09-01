CREATE TABLE camel (id INT GENERATED BY DEFAULT AS IDENTITY NOT NULL, species VARCHAR(50) NOT NULL)

-- for consumer
CREATE TABLE projects (id INT GENERATED BY DEFAULT AS IDENTITY NOT NULL, project VARCHAR(25), license VARCHAR(25), processed BOOLEAN)

-- idempotent repo
CREATE TABLE CAMEL_MESSAGEPROCESSED (processorName VARCHAR(255), messageId VARCHAR(100), createdAt TIMESTAMP)

-- aggregation repo
CREATE TABLE aggregation (id VARCHAR(255) NOT NULL, exchange BLOB, version BIGINT)
CREATE TABLE aggregation_completed (id VARCHAR(255) NOT NULL, exchange BLOB, version BIGINT)

