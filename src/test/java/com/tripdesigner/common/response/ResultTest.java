package com.tripdesigner.common.response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_wraps_data() {
        Result<String> r = Result.success("hi");
        assertEquals(0, r.getCode());
        assertEquals("success", r.getMessage());
        assertEquals("hi", r.getData());
    }

    @Test
    void fail_uses_resultcode() {
        Result<?> r = Result.fail(ResultCode.AUTH_INVALID_CREDENTIALS);
        assertEquals(1003, r.getCode());
        assertEquals(ResultCode.AUTH_INVALID_CREDENTIALS.getMessage(), r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void fail_with_custom_message_overrides() {
        Result<?> r = Result.fail(ResultCode.COMMON_INTERNAL_ERROR, "boom");
        assertEquals(500, r.getCode());
        assertEquals("boom", r.getMessage());
    }
}
