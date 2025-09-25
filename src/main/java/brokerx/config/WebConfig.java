package brokerx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Web MVC
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // Configuration de base - formatage des dates géré par @DateTimeFormat
}