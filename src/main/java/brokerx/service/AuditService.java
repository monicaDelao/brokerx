package brokerx.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AuditService {
    
    private static final DateTimeFormatter AUDIT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Journalise l'activation d'un compte avec audit complet
     * @param email Email du client
     * @param actionType Type d'action (ex: "COMPTE_ACTIVE")
     * @param details Détails de l'action
     * @return ID de l'audit pour traçabilité
     */
    public String journaliserActivationCompte(String email, String actionType, String details) {
        LocalDateTime timestamp = LocalDateTime.now();
        String auditId = genererIdAudit(email, timestamp);
        
        // Création de l'empreinte des documents/données
        String empreinteDocuments = genererEmpreinteDocuments(email, actionType, details, timestamp);
        
        // Journalisation dans la console (en production, utiliser un vrai système de logging)
        System.out.println("=== AUDIT D'ACTIVATION DE COMPTE ===");
        System.out.println("ID Audit: " + auditId);
        System.out.println("Email: " + email);
        System.out.println("Action: " + actionType);
        System.out.println("Horodatage: " + timestamp.format(AUDIT_DATE_FORMAT));
        System.out.println("Empreinte documents: " + empreinteDocuments);
        System.out.println("Details: " + details);
        System.out.println("Statut: SUCCES");
        System.out.println("====================================");
        
        return auditId;
    }
    
    /**
     * Génère un ID unique pour l'audit
     */
    private String genererIdAudit(String email, LocalDateTime timestamp) {
        return "AUDIT_" + timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + 
               "_" + Math.abs(email.hashCode());
    }
    
    /**
     * Génère une empreinte cryptographique des documents/données
     * pour assurer l'intégrité et la traçabilité
     */
    private String genererEmpreinteDocuments(String email, String action, String details, LocalDateTime timestamp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Données à hasher pour l'empreinte
            String dataToHash = email + "|" + action + "|" + details + "|" + timestamp.toString();
            byte[] hash = digest.digest(dataToHash.getBytes("UTF-8"));
            
            // Conversion en hexadécimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 16).toUpperCase(); // Prendre les 16 premiers caractères
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération de l'empreinte: " + e.getMessage());
            return "EMPREINTE_ERREUR_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Journalise l'étape de vérification email
     */
    public void journaliserVerificationEmail(String email, String codeVerification, boolean succes) {
        LocalDateTime timestamp = LocalDateTime.now();
        String empreinte = genererEmpreinteDocuments(email, "VERIFICATION_EMAIL", 
                                                   "Code: " + codeVerification + " | Succès: " + succes, timestamp);
        
        System.out.println("=== AUDIT VERIFICATION EMAIL ===");
        System.out.println("Email: " + email);
        System.out.println("Code verifie: " + codeVerification);
        System.out.println("Horodatage: " + timestamp.format(AUDIT_DATE_FORMAT));
        System.out.println("Empreinte: " + empreinte);
        System.out.println("Resultat: " + (succes ? "SUCCES" : "ECHEC"));
        System.out.println("================================");
    }
}
