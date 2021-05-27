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

DROP TABLE IF EXISTS camel
CREATE TABLE camel (id int AUTO_INCREMENT, species VARCHAR(255))
CREATE ALIAS ADD_NUMS FOR "org.apache.camel.quarkus.component.sql.it.storedproc.NumberAddStoredProcedure.addNumbers"

-- for consumer
DROP TABLE IF EXISTS projects
create table projects (id integer primary key, project varchar(25), license varchar(5), processed BOOLEAN);


-- idempotent repo
DROP TABLE IF EXISTS CAMEL_MESSAGEPROCESSED
-- CREATE TABLE camel_messageProcessed ( processorName VARCHAR(255), messageId VARCHAR(100), createdAt TIMESTAMP )
