package com.lifesync.lifesync.integration;

import org.springframework.test.context.ActiveProfilesResolver;

public class TestProfileResolver implements ActiveProfilesResolver {
    @Override
    public String[] resolve(Class<?> testClass) {
        if (Boolean.parseBoolean(System.getenv("LIFESYNC_TEST_POSTGRES"))) {
            return new String[] {"test", "postgres-test"};
        }
        return new String[] {"test"};
    }
}
