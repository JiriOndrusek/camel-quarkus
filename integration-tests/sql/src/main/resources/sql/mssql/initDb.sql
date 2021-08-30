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

IF EXISTS(SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'camel' AND TABLE_SCHEMA = 'dbo')
DROP TABLE [dbo].camel;

CREATE TABLE [dbo].camel (
   id int NOT NULL    IDENTITY    PRIMARY KEY,
   species varchar(50)
);

-- for consumer
IF EXISTS(SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'projects' AND TABLE_SCHEMA = 'dbo')
DROP TABLE [dbo].projects;
CREATE TABLE projects (
       id int,
       project varchar(25),
       license varchar(5),
       processed BIT
);

-- idempotent repo
IF EXISTS(SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CAMEL_MESSAGEPROCESSED' AND TABLE_SCHEMA = 'dbo')
DROP TABLE [dbo].CAMEL_MESSAGEPROCESSED;

-- aggregation repo
IF EXISTS(SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'aggregation' AND TABLE_SCHEMA = 'dbo')
DROP TABLE [dbo].aggregation;
CREATE TABLE aggregation (
      id varchar(255),
      exchange Image,
      version bigint
);
IF EXISTS(SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'aggregation_completed' AND TABLE_SCHEMA = 'dbo')
DROP TABLE [dbo].aggregation_completed;
CREATE TABLE aggregation_completed (
    id varchar(255),
    exchange Image,
    version bigint
);