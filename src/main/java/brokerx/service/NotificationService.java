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
        
        System.out.println("=== EMAIL DE VERIFICATION ===");
        System.out.println("A: " + email);
        System.out.println("Objet: BrokerX - Verifiez votre adresse email");
        System.out.println("---");
        System.out.println("Bonjour " + prenom + ",");
        System.out.println("");
        System.out.println("Bienvenue sur BrokerX ! Pour finaliser votre inscription,");
        System.out.println("veuillez verifier votre adresse email en cliquant sur le lien ci-dessous :");
        System.out.println("");
        System.out.println("LIEN: " + lienVerification);
        System.out.println("");
        System.out.println("Ou saisissez ce code de verification : " + codeVerification);
        System.out.println("");
        System.out.println("Ce lien est valide pendant 24 heures.");
        System.out.println("");
        System.out.println("Cordialement,");
        System.out.println("L'equipe BrokerX");
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
            System.out.println("ATTENTION: Pas de numero de telephone fourni - SMS non envoye");
            return true; // Considéré comme succès car optionnel
        }
        
        // Simulation d'envoi de SMS
        System.out.println("=== SMS OTP ===");
        System.out.println("A: +1" + telephone);
        System.out.println("---");
        System.out.println("Bonjour " + prenom + ",");
        System.out.println("");
        System.out.println("Votre code de verification BrokerX :");
        System.out.println("CODE: " + codeOTP);
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
        System.out.println("A: " + email);
        System.out.println("Objet: Bienvenue sur BrokerX !");
        System.out.println("---");
        System.out.println("Felicitations " + prenom + " ! SUCCES");
        System.out.println("");
        System.out.println("Votre inscription sur BrokerX est maintenant complete.");
        System.out.println("Vous pouvez desormais acceder a votre compte et commencer");
        System.out.println("a utiliser nos services de courtage.");
        System.out.println("");
        System.out.println("Connexion: http://localhost:8080/connexion");
        System.out.println("");
        System.out.println("Merci de votre confiance !");
        System.out.println("L'equipe BrokerX");
        System.out.println("==========================");
        
        return true;
    }
}
