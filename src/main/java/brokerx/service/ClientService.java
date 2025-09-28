package brokerx.service;

import brokerx.entity.Client;
import brokerx.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class ClientService {
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    private final Random random = new Random();
    
    public Client sauvegarderClient(Client client) {
        return clientRepository.save(client);
    }
    
    /**
     * Crée un nouveau client avec statut PENDING et envoie les notifications de vérification
     * @param client le client à créer
     * @return le résultat de la création avec le client et les codes générés
     * @throws IllegalArgumentException si l'email ou le téléphone existe déjà
     */
    public CreationResult creerClientAvecVerification(Client client) {
        // 1. Vérifier si l'email existe déjà
        if (emailExiste(client.getEmail())) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà");
        }
        
        // 2. Vérifier si le téléphone existe déjà (si fourni)
        if (client.getTelephone() != null && !client.getTelephone().trim().isEmpty()) {
            if (telephoneExiste(client.getTelephone())) {
                throw new IllegalArgumentException("Un compte avec ce numéro de téléphone existe déjà");
            }
        }
        
        // 3. Définir les données d'inscription
        client.setDateInscription(LocalDateTime.now());
        client.setStatusInscription("PENDING");
        client.setEmailVerifie(false);
        client.setTelephoneVerifie(false);
        
        // 4. Sauvegarder le client
        Client clientSauvegarde = clientRepository.save(client);
        
        // 5. Générer les codes de vérification
        String codeEmail = genererCodeVerification();
        String codeOTP = genererCodeOTP();
        
        // 6. Envoyer les notifications
        boolean emailEnvoye = notificationService.envoyerEmailVerification(
            clientSauvegarde.getEmail(), 
            codeEmail, 
            clientSauvegarde.getPrenom()
        );
        
        if (clientSauvegarde.getTelephone() != null && !clientSauvegarde.getTelephone().trim().isEmpty()) {
            notificationService.envoyerSMSOTP(
                clientSauvegarde.getTelephone(), 
                codeOTP, 
                clientSauvegarde.getPrenom()
            );
        }
        
        System.out.println("Client cree avec succes - ID: " + clientSauvegarde.getId());
        System.out.println("Code email: " + codeEmail);
        System.out.println("Code OTP: " + codeOTP);
        
        return new CreationResult(clientSauvegarde, codeEmail, codeOTP);
    }
    
    public Optional<Client> trouverParEmail(String email) {
        return clientRepository.findByEmail(email);
    }
    
    public boolean emailExiste(String email) {
        return clientRepository.existsByEmail(email);
    }
    
    public boolean telephoneExiste(String telephone) {
        if (telephone == null || telephone.trim().isEmpty()) {
            return false;
        }
        return clientRepository.existsByTelephone(telephone);
    }
    
    public void marquerEmailVerifie(String email) {
        Optional<Client> clientOpt = clientRepository.findByEmail(email);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            client.setEmailVerifie(true);
            // Si pas de téléphone ou téléphone déjà vérifié, inscription complète - STATUT ACTIVE
            if (client.getTelephone() == null || client.getTelephone().trim().isEmpty() || client.isTelephoneVerifie()) {
                client.setStatusInscription("ACTIVE");
            }
            clientRepository.save(client);
        }
    }
    
    /**
     * Active complètement le compte après vérification email avec journalisation d'audit
     * Implémente la requirement: Le Système passe le compte à Active et journalise l'audit 
     * (horodatage, empreinte des documents)
     */
    public String activerCompteAvecAudit(String email, String codeVerification) {
        // Journaliser d'abord la vérification email
        auditService.journaliserVerificationEmail(email, codeVerification, true);
        
        Optional<Client> clientOpt = clientRepository.findByEmail(email);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            
            // Marquer email comme vérifié
            client.setEmailVerifie(true);
            
            // Passer le compte à ACTIVE (requirement du cahier des charges)
            client.setStatusInscription("ACTIVE");
            
            // Sauvegarder les changements
            Client clientActive = clientRepository.save(client);
            
            // Journaliser l'activation du compte avec audit complet
            String auditId = auditService.journaliserActivationCompte(
                email, 
                "COMPTE_ACTIVE",
                "Activation après vérification email réussie. " +
                "Client ID: " + clientActive.getId() + 
                ", Date inscription: " + clientActive.getDateInscription() +
                ", Email vérifié: " + clientActive.isEmailVerifie() +
                ", Statut: " + clientActive.getStatusInscription()
            );
            
            System.out.println("COMPTE ACTIVE avec audit - Email: " + email + " | Audit ID: " + auditId);
            return auditId;
        }
        return null;
    }
    
    public void marquerTelephoneVerifie(String email) {
        Optional<Client> clientOpt = clientRepository.findByEmail(email);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            client.setTelephoneVerifie(true);
            if (client.isEmailVerifie()) {
                client.setStatusInscription("COMPLETE");
            }
            clientRepository.save(client);
        }
    }
    
    public String genererCodeVerification() {
        return String.format("%06d", random.nextInt(1000000));
    }
    
    public String genererCodeOTP() {
        return String.format("%04d", random.nextInt(10000));
    }
    
    /**
     * Classe pour encapsuler le résultat de la création d'un client
     */
    public static class CreationResult {
        private final Client client;
        private final String codeEmail;
        private final String codeOTP;
        
        public CreationResult(Client client, String codeEmail, String codeOTP) {
            this.client = client;
            this.codeEmail = codeEmail;
            this.codeOTP = codeOTP;
        }
        
        public Client getClient() { return client; }
        public String getCodeEmail() { return codeEmail; }
        public String getCodeOTP() { return codeOTP; }
    }
}
