package com.noteshare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * NoteShare is designed to be reached from any device on the same local
 * network (phone, laptop, etc. all get different LAN IPs), so we can't pin
 * CORS to one fixed origin the way a normal hosted app would. Instead we
 * allow the common private-network ranges plus localhost, which is far
 * narrower than the previous "allow literally any origin" (*) setting.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://192.168.*.*:*",
                        "http://10.*.*.*:*",
                        "http://172.16.*.*:*",
                        "http://172.17.*.*:*",
                        "http://172.18.*.*:*",
                        "http://172.19.*.*:*",
                        "http://172.2*.*.*:*",
                        "http://172.30.*.*:*",
                        "http://172.31.*.*:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
