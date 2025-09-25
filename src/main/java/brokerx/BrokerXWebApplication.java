package brokerx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application principale BrokerX avec interface web
 * UC-01 : Inscription et Vérification d'Identité
 */
@SpringBootApplication
public class BrokerXWebApplication {
    
    public static void main(String[] args) {
        // Démarrage Spring Boot
        SpringApplication.run(BrokerXWebApplication.class, args);
    }
}