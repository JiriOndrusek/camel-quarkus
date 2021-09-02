package org.apache.camel.quarkus.component.sql.it;

import java.util.HashSet;
import java.util.Set;

public class SqlHelper {

    static String convertBooleanToSqlDialect(String dbKind, boolean value) {
        return convertBooleanToSqlResult(dbKind, value).toString();
    }

    static Object convertBooleanToSqlResult(String dbKind, boolean value) {
        Set<String> booleanAsNumber = new HashSet<>() {
            {
                add("db2");
                add("mssql");
                add("oracle");

            }
        };
        if (value) {
            return booleanAsNumber.contains(dbKind) ? 1 : true;
        }
        return booleanAsNumber.contains(dbKind) ? 0 : false;
    }
}
