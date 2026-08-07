package com.groom.moigo.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth")
public record OAuthCookieProperties(
        boolean cookieSecure
) {
}
