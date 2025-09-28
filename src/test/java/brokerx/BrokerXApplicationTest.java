package brokerx;

import brokerx.entity.Client;
import brokerx.entity.MfaConfig;
import brokerx.service.ClientService;
import brokerx.service.MfaService;
import brokerx.service.SecurityService;
import brokerx.repository.ClientRepository;
import brokerx.repository.MfaConfigRepository;
import brokerx.repository.TentativeConnexionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests complets pour les cas d'utilisation UC-01 (Inscription) et UC-02 (Authentification/MFA)
 * Couvre les fonctionnalités critiques de BrokerX
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BrokerXApplicationTest {

    /** Année de naissance pour les tests. */
    private static final int BIRTH_YEAR = 1990;
    
    /** Jour de naissance pour les tests. */
    private static final int BIRTH_DAY = 15;
    
    /** Email de test standard. */
    private static final String TEST_EMAIL = "test@brokerx.com";
    
    /** Téléphone de test standard. */
    private static final String TEST_TELEPHONE = "5141234567";
    
    /** Mot de passe de test standard. */
    private static final String TEST_PASSWORD = "MotDePasse123!";

    @Autowired
    private ClientService clientService;
    
    @Autowired
    private MfaService mfaService;
    
    @Autowired
    private SecurityService securityService;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private MfaConfigRepository mfaConfigRepository;
    
    @Autowired
    private TentativeConnexionRepository tentativeConnexionRepository;

    @BeforeEach
    void setUp() {
        // Nettoyer les données de test avant chaque test
        tentativeConnexionRepository.deleteAll();
        mfaConfigRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    @DisplayName("Le contexte Spring Boot se charge correctement")
    void contextLoads() {
        assertNotNull(clientService, "ClientService doit être injecté");
        assertNotNull(mfaService, "MfaService doit être injecté");
        assertNotNull(securityService, "SecurityService doit être injecté");
        assertTrue(true, "Le contexte Spring Boot se charge sans erreur");
    }
    
    // ===============================
    // TESTS UC-01: INSCRIPTION CLIENT
    // ===============================
    
    @Nested
    @DisplayName("UC-01: Tests d'inscription client")
    class InscriptionClientTests {
        
        @Test
        @DisplayName("UC-01.1: Création d'un client avec données valides")
        void testCreationClientAvecDonneesValides() {
            // Arrange
            Client client = creerClientDeTest();
            
            // Act
            ClientService.CreationResult resultat = clientService.creerClientAvecVerification(client);
            
            // Assert
            assertNotNull(resultat, "Le résultat de création ne doit pas être null");
            assertNotNull(resultat.getClient(), "Le client créé ne doit pas être null");
            assertNotNull(resultat.getCodeEmail(), "Le code email ne doit pas être null");
            assertNotNull(resultat.getCodeOTP(), "Le code OTP ne doit pas être null");
            
            Client clientCree = resultat.getClient();
            assertEquals("Jean", clientCree.getPrenom());
            assertEquals("Dupont", clientCree.getNom());
            assertEquals(TEST_EMAIL, clientCree.getEmail());
            assertEquals(TEST_TELEPHONE, clientCree.getTelephone());
            assertEquals("PENDING", clientCree.getStatusInscription());
            assertFalse(clientCree.isEmailVerifie(), "L'email ne doit pas être vérifié initialement");
            assertFalse(clientCree.isTelephoneVerifie(), "Le téléphone ne doit pas être vérifié initialement");
            assertNotNull(clientCree.getDateInscription(), "La date d'inscription doit être définie");
        }
        
        @Test
        @DisplayName("UC-01.2: Rejet d'inscription avec email déjà existant")
        void testRejetsInscriptionEmailExistant() {
            // Arrange
            Client clientExistant = creerClientDeTest();
            clientService.creerClientAvecVerification(clientExistant);
            
            Client nouveauClient = creerClientDeTest();
            nouveauClient.setPrenom("Marie");
            nouveauClient.setNom("Martin");
            
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                clientService.creerClientAvecVerification(nouveauClient);
            }, "Une exception doit être levée pour un email déjà existant");
        }
        
        @Test
        @DisplayName("UC-01.3: Rejet d'inscription avec téléphone déjà existant")
        void testRejetInscriptionTelephoneExistant() {
            // Arrange
            Client clientExistant = creerClientDeTest();
            clientService.creerClientAvecVerification(clientExistant);
            
            Client nouveauClient = creerClientDeTest();
            nouveauClient.setEmail("autre@email.com");
            nouveauClient.setPrenom("Marie");
            nouveauClient.setNom("Martin");
            
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                clientService.creerClientAvecVerification(nouveauClient);
            }, "Une exception doit être levée pour un téléphone déjà existant");
        }
        
        @Test
        @DisplayName("UC-01.4: Vérification des méthodes utilitaires de Client")
        void testMethodesUtilitairesClient() {
            // Arrange
            Client client = creerClientDeTest();
            ClientService.CreationResult resultat = clientService.creerClientAvecVerification(client);
            Client clientCree = resultat.getClient();
            
            // Act & Assert
            assertEquals("Jean Dupont", clientCree.getNomComplet());
            assertFalse(clientCree.isInscriptionComplete(), "L'inscription ne doit pas être complète sans vérification");
            assertFalse(clientCree.isCompteActif(), "Le compte ne doit pas être actif avec statut PENDING");
            
            // Activer le compte
            clientCree.setStatusInscription("ACTIVE");
            clientCree.setEmailVerifie(true);
            clientCree.setTelephoneVerifie(true);
            
            assertTrue(clientCree.isInscriptionComplete(), "L'inscription doit être complète après vérification");
            assertTrue(clientCree.isCompteActif(), "Le compte doit être actif avec statut ACTIVE");
        }
        
        @Test
        @DisplayName("UC-01.5: Validation des données client")
        void testValidationDonneesClient() {
            // Test de l'entité Client avec diverses validations
            Client client = new Client();
            client.setPrenom("Jean");
            client.setNom("Dupont");
            client.setEmail("jean.dupont@email.com");
            client.setTelephone("5141234567");
            client.setMotDePasse("MotDePasse123!");
            client.setDateNaissance(LocalDate.of(BIRTH_YEAR, 5, BIRTH_DAY));
            client.setAdresse("123 Rue de la Paix, Montréal");
            
            // Vérifications basiques
            assertNotNull(client);
            assertEquals("Jean", client.getPrenom());
            assertEquals("Dupont", client.getNom());
            assertEquals("jean.dupont@email.com", client.getEmail());
            assertTrue(client.getEmail().contains("@"));
            assertEquals("5141234567", client.getTelephone());
            assertNotNull(client.getDateInscription(), "La date d'inscription doit être automatiquement définie");
        }
    }
    
    // ========================================
    // TESTS UC-02: AUTHENTIFICATION ET MFA
    // ========================================
    
    @Nested
    @DisplayName("UC-02: Tests d'authentification et MFA")
    class AuthentificationMfaTests {
        
        @Test
        @DisplayName("UC-02.1: Recherche client par email")
        void testRechercheClientParEmail() {
            // Arrange
            Client client = creerClientDeTest();
            ClientService.CreationResult resultat = clientService.creerClientAvecVerification(client);
            Client clientCree = resultat.getClient();
            
            // Act
            Optional<Client> clientTrouve = clientService.trouverParEmail(TEST_EMAIL);
            Optional<Client> clientInexistant = clientService.trouverParEmail("inexistant@email.com");
            
            // Assert
            assertTrue(clientTrouve.isPresent(), "Le client doit être trouvé par son email");
            assertEquals(clientCree.getId(), clientTrouve.get().getId());
            assertEquals(TEST_EMAIL, clientTrouve.get().getEmail());
            assertFalse(clientInexistant.isPresent(), "Un client inexistant ne doit pas être trouvé");
        }
        
        @Test
        @DisplayName("UC-02.2: Vérification existence email et téléphone")
        void testVerificationExistenceEmailTelephone() {
            // Arrange
            Client client = creerClientDeTest();
            clientService.creerClientAvecVerification(client);
            
            // Act & Assert
            assertTrue(clientService.emailExiste(TEST_EMAIL), "L'email doit exister après création");
            assertFalse(clientService.emailExiste("inexistant@email.com"), "Un email inexistant ne doit pas exister");
            
            assertTrue(clientService.telephoneExiste(TEST_TELEPHONE), "Le téléphone doit exister après création");
            assertFalse(clientService.telephoneExiste("9999999999"), "Un téléphone inexistant ne doit pas exister");
            assertFalse(clientService.telephoneExiste(null), "Un téléphone null ne doit pas exister");
            assertFalse(clientService.telephoneExiste(""), "Un téléphone vide ne doit pas exister");
        }
        
        @Test
        @DisplayName("UC-02.3: Configuration MFA pour un client")
        void testConfigurationMfaClient() {
            // Arrange
            Client client = creerClientDeTest();
            ClientService.CreationResult resultat = clientService.creerClientAvecVerification(client);
            Client clientCree = resultat.getClient();
            
            // Act
            MfaConfig configMfa = mfaService.obtenirOuCreerConfigMfa(clientCree);
            
            // Assert
            assertNotNull(configMfa, "La configuration MFA ne doit pas être null");
            assertEquals(clientCree.getId(), configMfa.getClient().getId());
            assertFalse(configMfa.isMfaActive(), "MFA ne doit pas être actif par défaut");
            assertFalse(configMfa.isSmsActive(), "SMS MFA ne doit pas être actif par défaut");
            assertNotNull(configMfa.getDateCreation(), "La date de création doit être définie");
        }
        
        @Test
        @DisplayName("UC-02.4: Activation MFA SMS")
        void testActivationMfaSms() {
            // Arrange
            Client client = creerClientDeTest();
            ClientService.CreationResult resultat = clientService.creerClientAvecVerification(client);
            Client clientCree = resultat.getClient();
            
            MfaConfig configMfa = mfaService.obtenirOuCreerConfigMfa(clientCree);
            
            // Act
            configMfa.setSmsActive(true);
            configMfa.setSmsNumero(TEST_TELEPHONE);
            configMfa.setMfaActive(true);
            MfaConfig configSauvegardee = mfaService.sauvegarderConfigMfa(configMfa);
            
            // Assert
            assertNotNull(configSauvegardee, "La configuration sauvegardée ne doit pas être null");
            assertTrue(configSauvegardee.isMfaActive(), "MFA doit être actif");
            assertTrue(configSauvegardee.isSmsActive(), "SMS MFA doit être actif");
            assertEquals(TEST_TELEPHONE, configSauvegardee.getSmsNumero());
            assertNotNull(configSauvegardee.getDateDerniereModification(), "La date de modification doit être définie");
        }
        
        @Test
        @DisplayName("UC-02.5: Génération et validation code SMS OTP")
        void testGenerationValidationCodeSmsOtp() {
            // Arrange
            Client client = creerClientDeTest();
            ClientService.CreationResult resultat = clientService.creerClientAvecVerification(client);
            Client clientCree = resultat.getClient();
            String adresseIp = "192.168.1.100";
            
            // Act
            String codeOtp = mfaService.genererEtEnvoyerSmsOtp(clientCree, adresseIp);
            
            // Assert
            assertNotNull(codeOtp, "Le code OTP ne doit pas être null");
            assertEquals(6, codeOtp.length(), "Le code OTP doit faire 6 caractères");
            assertTrue(codeOtp.matches("\\d{6}"), "Le code OTP doit contenir uniquement des chiffres");
            
            // Test validation
            assertTrue(mfaService.validerCodeSmsOtp(clientCree, codeOtp, adresseIp), "Le code OTP généré doit être valide");
            assertFalse(mfaService.validerCodeSmsOtp(clientCree, "000000", adresseIp), "Un code OTP incorrect doit être invalide");
        }
        
        @Test
        @DisplayName("UC-02.6: Vérification autorisation connexion")
        void testVerificationAutorisationConnexion() {
            // Arrange
            String adresseIp = "192.168.1.100";
            String email = TEST_EMAIL;
            
            // Act
            SecurityService.ResultatSecurite resultat = securityService.verifierAutorisationConnexion(adresseIp, email);
            
            // Assert
            assertNotNull(resultat, "Le résultat de sécurité ne doit pas être null");
            assertTrue(resultat.isAcceAutorise(), "L'accès doit être autorisé pour une première tentative");
            // Note: la classe ResultatSecurite n'a pas de getMessage(), seulement getRaisonRefus()
            if (!resultat.isAcceAutorise()) {
                assertNotNull(resultat.getRaisonRefus(), "La raison de refus doit être définie si accès refusé");
            }
        }
        
        @Test
        @DisplayName("UC-02.7: Enregistrement tentative de connexion")
        void testEnregistrementTentativeConnexion() {
            // Arrange
            String adresseIp = "192.168.1.100";
            String email = TEST_EMAIL;
            String userAgent = "Mozilla/5.0 Test Browser";
            
            // Act
            securityService.enregistrerTentativeConnexion(adresseIp, email, true, userAgent, "Connexion réussie");
            securityService.enregistrerTentativeConnexion(adresseIp, email, false, userAgent, "Mot de passe incorrect");
            
            // Assert
            long nombreTentatives = tentativeConnexionRepository.count();
            assertTrue(nombreTentatives >= 2, "Au moins 2 tentatives doivent être enregistrées");
        }
    }
    
    // ============================
    // TESTS D'INTÉGRATION
    // ============================
    
    @Nested
    @DisplayName("Tests d'intégration")
    class TestsIntegration {
        
        @Test
        @DisplayName("Intégration complète: Inscription puis authentification avec MFA")
        void testIntegrationCompleteInscriptionAuthentificationMfa() {
            // Étape 1: Inscription
            Client client = creerClientDeTest();
            ClientService.CreationResult resultatInscription = clientService.creerClientAvecVerification(client);
            
            assertNotNull(resultatInscription, "L'inscription doit réussir");
            Client clientCree = resultatInscription.getClient();
            assertNotNull(clientCree.getId(), "Le client doit avoir un ID après sauvegarde");
            
            // Étape 2: Activation du compte (simulation vérification email)
            clientCree.setStatusInscription("ACTIVE");
            clientCree.setEmailVerifie(true);
            clientCree.setTelephoneVerifie(true);
            Client clientActive = clientService.sauvegarderClient(clientCree);
            
            assertTrue(clientActive.isCompteActif(), "Le compte doit être actif");
            
            // Étape 3: Configuration MFA
            MfaConfig configMfa = mfaService.obtenirOuCreerConfigMfa(clientActive);
            configMfa.setSmsActive(true);
            configMfa.setSmsNumero(TEST_TELEPHONE);
            configMfa.setMfaActive(true);
            mfaService.sauvegarderConfigMfa(configMfa);
            
            // Étape 4: Simulation processus d'authentification
            Optional<Client> clientAuthentifie = clientService.trouverParEmail(TEST_EMAIL);
            assertTrue(clientAuthentifie.isPresent(), "Le client doit être trouvé pour authentification");
            
            // Étape 5: Génération et validation code MFA SMS
            String codeOtp = mfaService.genererEtEnvoyerSmsOtp(clientAuthentifie.get(), "192.168.1.100");
            assertNotNull(codeOtp, "Le code OTP doit être généré");
            assertTrue(mfaService.validerCodeSmsOtp(clientAuthentifie.get(), codeOtp, "192.168.1.100"), "Le code OTP doit être valide");
            
            // Étape 6: Enregistrement tentative de connexion réussie
            securityService.enregistrerTentativeConnexion("192.168.1.100", TEST_EMAIL, true, "Test Browser", "Authentification complète avec MFA");
            
            assertTrue(true, "Le processus complet d'inscription et authentification avec MFA doit réussir");
        }
    }
    
    // ============================
    // MÉTHODES UTILITAIRES
    // ============================
    
    /**
     * Crée un client de test avec des données valides standard
     */
    private Client creerClientDeTest() {
        Client client = new Client();
        client.setPrenom("Jean");
        client.setNom("Dupont");
        client.setEmail(TEST_EMAIL);
        client.setTelephone(TEST_TELEPHONE);
        client.setMotDePasse(TEST_PASSWORD);
        client.setDateNaissance(LocalDate.of(BIRTH_YEAR, 5, BIRTH_DAY));
        client.setAdresse("123 Rue de la Paix, Montréal, QC, H1A 1A1");
        return client;
    }
}