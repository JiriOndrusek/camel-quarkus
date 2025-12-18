package org.apache.camel.quarkus.component.cyberark.vault.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class CyberarkVaultWireMockTestResource extends WireMockTestResourceLifecycleManager {

    private static String CYBERARK_ENV_URL = "cyberark_env_url";

    @Override
    protected String getRecordTargetBaseUrl() {
        //        return envOrDefault(CYBERARK_ENV_URL, null);
        return "http://localhost:8080/";
    }

    @Override
    protected boolean isMockingEnabled() {
        return true;
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> options = super.start();
        String jiraUrl = envOrDefault(CYBERARK_ENV_URL, "http://localhost:8080/");

        return options;
    }
}
