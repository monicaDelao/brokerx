package brokerx;

import brokerx.entity.Client;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires simples pour BrokerX
 * Utilise les classes existantes de l'application
 */
@SpringBootTest
@ActiveProfiles("test")
class BrokerXApplicationTest {

    @Test
    void contextLoads() {
        // Test que le contexte Spring Boot se charge correctement
        assertTrue(true, "Le contexte Spring Boot se charge sans erreur");
    }

    @Test
    void testClientEntityCreation() {
        // Test de création d'un client avec les classes existantes
        Client client = new Client();
        client.setPrenom("Jean");
        client.setNom("Dupont");
        client.setEmail("jean.dupont@email.com");
        client.setTelephone("5141234567");
        client.setMotDePasse("MotDePasse123!");
        client.setDateNaissance(LocalDate.of(1990, 5, 15));
        client.setAdresse("123 Rue de la Paix, Montréal");
        
        // Vérifications basiques
        assertNotNull(client);
        assertEquals("Jean", client.getPrenom());
        assertEquals("Dupont", client.getNom());
        assertEquals("jean.dupont@email.com", client.getEmail());
        assertTrue(client.getEmail().contains("@"));
    }
}