package brokerx.service;

import org.springframework.stereotype.Service;

/**
 * Service de notification pour l'envoi d'emails et SMS
 * Implémentation simulée pour le développement
 */
@Service
public class NotificationService {

    /**
     * Envoie un email de vérification avec un lien
     * @param email l'adresse email du destinataire
     * @param codeVerification le code de vérification à inclure
     * @param prenom le prénom du client pour personnalisation
     * @return true si l'envoi a réussi, false sinon
     */
    public boolean envoyerEmailVerification(String email, String codeVerification, String prenom) {
        // Simulation d'envoi d'email
        String lienVerification = "http://localhost:8080/verification-email?code=" + codeVerification;
        
        System.out.println("=== EMAIL DE VÉRIFICATION ===");
        System.out.println("À: " + email);
        System.out.println("Objet: BrokerX - Vérifiez votre adresse email");
        System.out.println("---");
        System.out.println("Bonjour " + prenom + ",");
        System.out.println("");
        System.out.println("Bienvenue sur BrokerX ! Pour finaliser votre inscription,");
        System.out.println("veuillez vérifier votre adresse email en cliquant sur le lien ci-dessous :");
        System.out.println("");
        System.out.println("🔗 " + lienVerification);
        System.out.println("");
        System.out.println("Ou saisissez ce code de vérification : " + codeVerification);
        System.out.println("");
        System.out.println("Ce lien est valide pendant 24 heures.");
        System.out.println("");
        System.out.println("Cordialement,");
        System.out.println("L'équipe BrokerX");
        System.out.println("=============================");
        
        // Simulation: toujours réussi en développement
        return true;
    }

    /**
     * Envoie un SMS avec code OTP
     * @param telephone le numéro de téléphone (format 10 chiffres)
     * @param codeOTP le code OTP à envoyer
     * @param prenom le prénom du client
     * @return true si l'envoi a réussi, false sinon
     */
    public boolean envoyerSMSOTP(String telephone, String codeOTP, String prenom) {
        if (telephone == null || telephone.trim().isEmpty()) {
            System.out.println("⚠️ Pas de numéro de téléphone fourni - SMS non envoyé");
            return true; // Considéré comme succès car optionnel
        }
        
        // Simulation d'envoi de SMS
        System.out.println("=== SMS OTP ===");
        System.out.println("À: +1" + telephone);
        System.out.println("---");
        System.out.println("Bonjour " + prenom + ",");
        System.out.println("");
        System.out.println("Votre code de vérification BrokerX :");
        System.out.println("🔢 " + codeOTP);
        System.out.println("");
        System.out.println("Ce code expire dans 10 minutes.");
        System.out.println("===============");
        
        // Simulation: toujours réussi en développement
        return true;
    }

    /**
     * Envoie un email de bienvenue après inscription complète
     * @param email l'adresse email
     * @param prenom le prénom du client
     * @return true si l'envoi a réussi
     */
    public boolean envoyerEmailBienvenue(String email, String prenom) {
        System.out.println("=== EMAIL DE BIENVENUE ===");
        System.out.println("À: " + email);
        System.out.println("Objet: Bienvenue sur BrokerX !");
        System.out.println("---");
        System.out.println("Félicitations " + prenom + " ! 🎉");
        System.out.println("");
        System.out.println("Votre inscription sur BrokerX est maintenant complète.");
        System.out.println("Vous pouvez désormais accéder à votre compte et commencer");
        System.out.println("à utiliser nos services de courtage.");
        System.out.println("");
        System.out.println("Connexion: http://localhost:8080/connexion");
        System.out.println("");
        System.out.println("Merci de votre confiance !");
        System.out.println("L'équipe BrokerX");
        System.out.println("==========================");
        
        return true;
    }
}