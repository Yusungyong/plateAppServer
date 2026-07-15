package com.plateapp.plate_main.common.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void preservesAValidClientRequestId() throws Exception {
        String requestId = "mobile.ABC_123-request";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_REQUEST_ID, requestId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                requestIdInsideChain.set(MDC.get(RequestIdFilter.MDC_KEY_REQUEST_ID)));

        assertThat(requestIdInsideChain.get()).isEqualTo(requestId);
        assertThat(response.getHeader(RequestIdFilter.HEADER_REQUEST_ID)).isEqualTo(requestId);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY_REQUEST_ID)).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidRequestIds")
    void replacesAnInvalidClientRequestId(String incoming) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_REQUEST_ID, incoming);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                requestIdInsideChain.set(MDC.get(RequestIdFilter.MDC_KEY_REQUEST_ID)));

        String generated = response.getHeader(RequestIdFilter.HEADER_REQUEST_ID);
        assertThat(generated).isNotEqualTo(incoming).matches("[a-f0-9]{16}");
        assertThat(requestIdInsideChain.get()).isEqualTo(generated);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY_REQUEST_ID)).isNull();
    }

    @Test
    void createsARequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (request, ignoredResponse) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER_REQUEST_ID)).matches("[a-f0-9]{16}");
    }

    private static Stream<String> invalidRequestIds() {
        return Stream.of(
                "",
                "contains space",
                "contains/slash",
                "contains\tcontrol",
                "x".repeat(RequestIdFilter.MAX_REQUEST_ID_LENGTH + 1)
        );
    }
}
