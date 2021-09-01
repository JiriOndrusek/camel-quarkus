package org.apache.camel.quarkus.component.sql.it;

public class SqlHelper {

    static String convertBooleanToSqlDialec(String dbKind, boolean value) {
        if (value) {
            return "mssql".equals(dbKind) ? "1" : "true";
        }
        return "mssql".equals(dbKind) ? "0" : "false";
    }
}
