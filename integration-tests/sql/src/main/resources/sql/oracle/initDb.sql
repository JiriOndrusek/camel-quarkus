--
-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
DROP TABLE camel
CREATE TABLE camel(id NUMBER GENERATED BY DEFAULT AS IDENTITY,species VARCHAR2(50) NOT NULL, PRIMARY KEY(id))
-- for consumer
DROP TABLE projects
CREATE TABLE projects (id NUMBER, project VARCHAR2(50) NOT NULL, license VARCHAR2(25) NOT NULL, processed NUMBER(1), PRIMARY KEY(id))

-- idempotent repo
DROP TABLE CAMEL_MESSAGEPROCESSED
CREATE TABLE CAMEL_MESSAGEPROCESSED ( processorName VARCHAR2(255), messageId VARCHAR2(100), createdAt TIMESTAMP )

-- aggregation repo
DROP TABLE aggregation
CREATE TABLE aggregation (id VARCHAR2(255) NOT NULL, exchange BLOB NOT NULL, version INT, PRIMARY KEY (id))
DROP TABLE aggregation_completed CASCADE CONSTRAINTS
CREATE TABLE aggregation_completed (id VARCHAR2(255) NOT NULL, exchange BLOB NOT NULL, version INT, PRIMARY KEY (id))
