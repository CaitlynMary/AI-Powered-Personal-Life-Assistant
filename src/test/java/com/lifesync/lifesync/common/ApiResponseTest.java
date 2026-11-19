package com.lifesync.lifesync.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {
    @Test
    void createsAndMutatesGenericSuccessEnvelope() {
        ApiResponse<String> response = ApiResponse.success("Loaded", "payload");
        assertTrue(response.isSuccess());
        assertEquals("Loaded", response.getMessage());
        assertEquals("payload", response.getData());

        response.setSuccess(false);
        response.setMessage("Failed");
        response.setData(null);
        assertFalse(response.isSuccess());
        assertEquals("Failed", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void failureEnvelopeIsAvailableForCallers() {
        ApiResponse<Void> response = ApiResponse.failure("No data", null);
        assertFalse(response.isSuccess());
        assertEquals("No data", response.getMessage());
        assertNull(response.getData());
    }
}
