package com.urbanclean.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SSL/HTTPS Configuration for production deployment
 * Enables HTTPS and redirects HTTP traffic to HTTPS
 * 
 * Activated when server.ssl.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
@Slf4j
public class SSLConfiguration {

    @Value("${server.ssl.key-store}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${server.ssl.key-alias}")
    private String keyAlias;

    @Value("${server.port:8443}")
    private int httpsPort;

    /**
     * Configure Tomcat to redirect HTTP to HTTPS
     * Creates an additional HTTP connector on port 8080 that redirects to HTTPS port
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        log.info("SSL Configuration enabled - HTTPS will be available on port {}", httpsPort);
        log.info("HTTP to HTTPS redirect configured - HTTP port 8080 will redirect to HTTPS port {}", httpsPort);
        
        return factory -> {
            factory.addAdditionalTomcatConnectors(createHttpConnector());
        };
    }

    /**
     * Create HTTP connector that redirects to HTTPS
     */
    private Connector createHttpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        
        log.info("Created HTTP connector on port 8080 redirecting to HTTPS port {}", httpsPort);
        
        return connector;
    }
}
