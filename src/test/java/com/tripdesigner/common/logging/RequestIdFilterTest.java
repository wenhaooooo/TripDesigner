package com.tripdesigner.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestIdFilterTest {

    @Test
    void sets_traceId_in_mdc_and_response_header() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AtomicReference<String> mdcCapture = new AtomicReference<>();

        doAnswer(inv -> {
            mdcCapture.set(MDC.get("traceId"));
            return null;
        }).when(chain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        new RequestIdFilter().doFilter(req, res, chain);

        assertNotNull(mdcCapture.get());
        assertEquals(mdcCapture.get(), res.getHeader("X-Trace-Id"));
    }

    @Test
    void reuses_inbound_trace_id_header() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Trace-Id", "abc-123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new RequestIdFilter().doFilter(req, res, chain);

        assertEquals("abc-123", res.getHeader("X-Trace-Id"));
    }
}
