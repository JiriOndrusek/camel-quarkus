package org.apache.camel.quarkus.component.sql.it;

public class SqlHelper {

    static String convertBooleanToSqlDialect(String dbKind, boolean value) {
        return convertBooleanToSqlResult(dbKind, value).toString();
    }

    static Object convertBooleanToSqlResult(String dbKind, boolean value) {
        if (value) {
            return "mssql".equals(dbKind) || "oracle".equals(dbKind) ? 1 : true;
        }
        return "mssql".equals(dbKind) || "oracle".equals(dbKind) ? 0 : false;
    }
}
