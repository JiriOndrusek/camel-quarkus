/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.quartz;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource.DataSourceLiteral;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import org.quartz.utils.PoolingConnectionProvider;

public class CamelQuarkusQuartzConnectionProvider implements PoolingConnectionProvider {
    private AgroalDataSource dataSource;
    private PropertiesDS propertiesDS = new PropertiesDS();
    private boolean initialized;

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
        // Do nothing as the connection will be closed inside the Agroal extension
    }

    @Override
    public void initialize() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>> initialize");

        if (!initialized) {
            final ArcContainer container = Arc.container();
            final InstanceHandle<AgroalDataSource> instanceHandle;
            final boolean useDefaultDataSource = propertiesDS.getDataSourceName() == null
                    || "".equals(propertiesDS.getDataSourceName().trim());
            if (useDefaultDataSource) {
                instanceHandle = container.instance(AgroalDataSource.class);
            } else {
                instanceHandle = container.instance(AgroalDataSource.class,
                        new DataSourceLiteral(propertiesDS.getDataSourceName()));
            }
            if (instanceHandle.isAvailable()) {
                this.dataSource = instanceHandle.get();
            } else {
                String message = String.format(
                        "JDBC Store configured but '%s' datasource is missing. You can configure your datasource by following the guide available at: https://quarkus.io/guides/datasource",
                        useDefaultDataSource ? "default" : propertiesDS.getDataSourceName());
                throw new IllegalStateException(message);
            }
        }
        initialized = true;
    }

    public CamelQuarkusQuartzConnectionProvider() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>.. create");

    }

    @Override
    public DataSource getDataSource() {
        if (!initialized && propertiesDS.getDataSourceName() != null) {
            initialize();
        }
        if (dataSource != null) {
            return dataSource;
        }
        return propertiesDS;
    }

    public static class PropertiesDS extends UnconfiguredDataSource {

        public PropertiesDS() {
            super(">>>>>>>>>>>>>>>>>>>");
        }

        private String dataSourceName;

        public String getDataSourceName() {
            return dataSourceName;
        }

        public void setDataSourceName(String dataSourceName) {
            this.dataSourceName = dataSourceName;
        }
    }
}
