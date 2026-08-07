package com.groom.moigo.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.global.error.ErrorCode;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class SecurityErrorResponseWriter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityErrorResponseWriter() {

    }

    public static void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(
                response.getWriter(),
                CommonResponse.error(errorCode.name(), errorCode.getMessage())
        );

    }
}
