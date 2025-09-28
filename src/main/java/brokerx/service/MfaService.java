package brokerx.service;

import brokerx.entity.Client;
import brokerx.entity.MfaConfig;
import brokerx.entity.MfaOtpCode;
import brokerx.repository.MfaConfigRepository;
import brokerx.repository.MfaOtpCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * UC-02 Step 3: Service MFA pour TOTP, SMS et codes OTP
 * Gestion complète de l'authentification multifacteur
 */
@Service
public class MfaService {

    @Autowired
    private MfaConfigRepository mfaConfigRepository;

    @Autowired
    private MfaOtpCodeRepository otpCodeRepository;

    // Configuration TOTP
    private static final int TOTP_WINDOW_SECONDS = 30; // Fenêtre de 30 secondes
    private static final int TOTP_DIGITS = 6;          // 6 chiffres
    private static final int TOTP_TOLERANCE = 1;       // +/- 1 fenêtre de temps
    
    // Configuration SMS OTP
    private static final int SMS_OTP_LENGTH = 6;       // Code 6 chiffres
    private static final int SMS_OTP_VALIDITY_MINUTES = 10; // Validité 10 minutes
    private static final int MAX_SMS_PER_HOUR = 5;     // Maximum 5 SMS par heure

    /**
     * Résultat de vérification MFA
     */
    public static class ResultatMfa {
        private boolean mfaRequise;
        private boolean verificationReussie;
        private String methodeMfaUtilisee;
        private String messageErreur;
        private MfaConfig configMfa;
        
        public ResultatMfa(boolean mfaRequise, boolean verificationReussie, String methodeMfa, String messageErreur) {
            this.mfaRequise = mfaRequise;
            this.verificationReussie = verificationReussie;
            this.methodeMfaUtilisee = methodeMfa;
            this.messageErreur = messageErreur;
        }
        
        // Getters
        public boolean isMfaRequise() { return mfaRequise; }
        public boolean isVerificationReussie() { return verificationReussie; }
        public String getMethodeMfaUtilisee() { return methodeMfaUtilisee; }
        public String getMessageErreur() { return messageErreur; }
        public MfaConfig getConfigMfa() { return configMfa; }
        public void setConfigMfa(MfaConfig configMfa) { this.configMfa = configMfa; }
    }

    /**
     * UC-02 Step 3: Vérifie si MFA est requise pour un client
     * @param client Le client
     * @return True si MFA requise
     */
    public boolean isMfaRequise(Client client) {
        Optional<MfaConfig> configOpt = mfaConfigRepository.findByClient(client);
        return configOpt.map(MfaConfig::isMfaRequise).orElse(false);
    }

    /**
     * UC-02 Step 3: Obtient ou crée la configuration MFA d'un client
     * @param client Le client
     * @return Configuration MFA
     */
    public MfaConfig obtenirOuCreerConfigMfa(Client client) {
        Optional<MfaConfig> configOpt = mfaConfigRepository.findByClient(client);
        
        if (configOpt.isPresent()) {
            return configOpt.get();
        }
        
        // Créer nouvelle configuration par défaut
        MfaConfig nouvelleConfig = new MfaConfig(client);
        // Par défaut, MFA n'est pas obligatoire mais peut être activée par l'utilisateur
        nouvelleConfig.setMfaObligatoire(false);
        nouvelleConfig.setMfaActive(false);
        
        return mfaConfigRepository.save(nouvelleConfig);
    }

    /**
     * UC-02 Step 3: Génère une clé secrète TOTP pour un client
     * @param client Le client
     * @return Clé secrète encodée en Base32
     */
    public String genererCleSecreteTOTP(Client client) {
        // Générer 20 bytes aléatoires pour la clé secrète
        SecureRandom random = new SecureRandom();
        byte[] secretKey = new byte[20];
        random.nextBytes(secretKey);
        
        // Encoder en Base32 (standard TOTP)
        String secretBase32 = Base64.getEncoder().encodeToString(secretKey);
        
        // Sauvegarder dans la configuration MFA
        MfaConfig config = obtenirOuCreerConfigMfa(client);
        config.setTotpSecretKey(secretBase32);
        config.setTotpActive(true);
        config.setMfaActive(true);
        mfaConfigRepository.save(config);
        
        System.out.println("UC-02 Step 3 - Cle TOTP generee pour client: " + client.getEmail());
        return secretBase32;
    }

    /**
     * UC-02 Step 3: Valide un code TOTP
     * @param client Le client
     * @param codeUtilisateur Le code saisi par l'utilisateur
     * @return True si code valide
     */
    public boolean validerCodeTOTP(Client client, String codeUtilisateur) {
        Optional<MfaConfig> configOpt = mfaConfigRepository.findByClient(client);
        if (configOpt.isEmpty() || !configOpt.get().isTotpActive()) {
            return false;
        }
        
        MfaConfig config = configOpt.get();
        String secretKey = config.getTotpSecretKey();
        
        if (secretKey == null || secretKey.isEmpty()) {
            return false;
        }
        
        try {
            // Calculer le code TOTP actuel et ceux dans la fenêtre de tolérance
            long timeWindow = System.currentTimeMillis() / 1000 / TOTP_WINDOW_SECONDS;
            
            for (int i = -TOTP_TOLERANCE; i <= TOTP_TOLERANCE; i++) {
                String codeCalcule = calculerTOTP(secretKey, timeWindow + i);
                if (codeUtilisateur.equals(codeCalcule)) {
                    config.setDerniereVerificationReussie(LocalDateTime.now());
                    mfaConfigRepository.save(config);
                    System.out.println("UC-02 Step 3 - Code TOTP valide pour: " + client.getEmail());
                    return true;
                }
            }
            
            System.out.println("UC-02 Step 3 - Code TOTP invalide pour: " + client.getEmail());
            return false;
            
        } catch (Exception e) {
            System.err.println("Erreur validation TOTP: " + e.getMessage());
            return false;
        }
    }

    /**
     * UC-02 Step 3: Génère et envoie un code SMS OTP
     * @param client Le client
     * @param adresseIp L'adresse IP pour audit
     * @return Code généré (ou null si erreur)
     */
    public String genererEtEnvoyerSmsOtp(Client client, String adresseIp) {
        // Vérifier limite SMS par heure
        LocalDateTime uneHeureAgo = LocalDateTime.now().minusHours(1);
        long tentativesRecentes = otpCodeRepository.countRecentAttempts(client, uneHeureAgo);
        
        if (tentativesRecentes >= MAX_SMS_PER_HOUR) {
            System.out.println("UC-02 Step 3 - Limite SMS depassee pour: " + client.getEmail());
            return null;
        }
        
        // Générer code SMS
        String codeSms = genererCodeNumerique(SMS_OTP_LENGTH);
        
        // Sauvegarder le code avec expiration
        MfaOtpCode otpCode = new MfaOtpCode(client, codeSms, MfaOtpCode.TypeMfa.SMS, SMS_OTP_VALIDITY_MINUTES);
        otpCode.setAdresseIpCreation(adresseIp);
        otpCodeRepository.save(otpCode);
        
        // Simuler l'envoi SMS (dans un vrai système, intégrer avec provider SMS)
        envoyerSmsSimule(client, codeSms);
        
        // Activer SMS dans la configuration si pas déjà fait
        MfaConfig config = obtenirOuCreerConfigMfa(client);
        if (!config.isSmsActive()) {
            config.setSmsActive(true);
            config.setSmsNumero(client.getTelephone());
            config.setMfaActive(true);
            mfaConfigRepository.save(config);
        }
        
        System.out.println("UC-02 Step 3 - Code SMS genere pour: " + client.getEmail());
        return codeSms;
    }

    /**
     * UC-02 Step 3: Valide un code SMS OTP
     * @param client Le client
     * @param codeUtilisateur Le code saisi
     * @param adresseIp L'adresse IP pour audit
     * @return True si code valide
     */
    public boolean validerCodeSmsOtp(Client client, String codeUtilisateur, String adresseIp) {
        Optional<MfaOtpCode> codeOpt = otpCodeRepository.findValidCode(
            client, codeUtilisateur, MfaOtpCode.TypeMfa.SMS, LocalDateTime.now()
        );
        
        if (codeOpt.isEmpty()) {
            System.out.println("UC-02 Step 3 - Code SMS invalide ou expire pour: " + client.getEmail());
            return false;
        }
        
        MfaOtpCode code = codeOpt.get();
        code.marquerUtilise(adresseIp);
        otpCodeRepository.save(code);
        
        // Mettre à jour dernière vérification réussie
        MfaConfig config = obtenirOuCreerConfigMfa(client);
        config.setDerniereVerificationReussie(LocalDateTime.now());
        mfaConfigRepository.save(config);
        
        System.out.println("UC-02 Step 3 - Code SMS valide pour: " + client.getEmail());
        return true;
    }

    /**
     * UC-02 Step 3: Processus complet de vérification MFA
     * @param client Le client
     * @param codeUtilisateur Le code saisi
     * @param adresseIp L'adresse IP
     * @return Résultat de la vérification
     */
    public ResultatMfa verifierMfa(Client client, String codeUtilisateur, String adresseIp) {
        MfaConfig config = obtenirOuCreerConfigMfa(client);
        
        if (!config.isMfaRequise()) {
            return new ResultatMfa(false, true, "AUCUNE", null);
        }
        
        ResultatMfa resultat = new ResultatMfa(true, false, null, null);
        resultat.setConfigMfa(config);
        
        // Essayer TOTP en premier si activé
        if (config.isTotpActive() && validerCodeTOTP(client, codeUtilisateur)) {
            resultat = new ResultatMfa(true, true, "TOTP", null);
            resultat.setConfigMfa(config);
            return resultat;
        }
        
        // Essayer SMS OTP si activé
        if (config.isSmsActive() && validerCodeSmsOtp(client, codeUtilisateur, adresseIp)) {
            resultat = new ResultatMfa(true, true, "SMS", null);
            resultat.setConfigMfa(config);
            return resultat;
        }
        
        // Aucune méthode n'a fonctionné
        resultat.messageErreur = "Code de verification incorrect ou expire.";
        return resultat;
    }

    // === MÉTHODES UTILITAIRES PRIVÉES ===

    /**
     * Calcule un code TOTP pour une fenêtre de temps donnée
     */
    private String calculerTOTP(String secretKey, long timeWindow) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] key = Base64.getDecoder().decode(secretKey);
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeWindow).array();
        
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(timeBytes);
        
        int offset = hash[hash.length - 1] & 0x0F;
        int code = ((hash[offset] & 0x7F) << 24) |
                   ((hash[offset + 1] & 0xFF) << 16) |
                   ((hash[offset + 2] & 0xFF) << 8) |
                   (hash[offset + 3] & 0xFF);
        
        code = code % (int) Math.pow(10, TOTP_DIGITS);
        return String.format("%0" + TOTP_DIGITS + "d", code);
    }

    /**
     * Génère un code numérique aléatoire
     */
    private String genererCodeNumerique(int longueur) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < longueur; i++) {
            code.append(ThreadLocalRandom.current().nextInt(0, 10));
        }
        return code.toString();
    }

    /**
     * Simule l'envoi d'un SMS (remplacer par vraie intégration SMS)
     */
    private void envoyerSmsSimule(Client client, String code) {
        System.out.println("=== SMS MFA OTP ===");
        System.out.println("A: " + client.getTelephone());
        System.out.println("---");
        System.out.println("BrokerX - Code de verification:");
        System.out.println("CODE: " + code);
        System.out.println("Ce code expire dans " + SMS_OTP_VALIDITY_MINUTES + " minutes.");
        System.out.println("===================");
    }

    /**
     * Sauvegarde une configuration MFA
     * @param config La configuration à sauvegarder
     * @return Configuration sauvegardée
     */
    public MfaConfig sauvegarderConfigMfa(MfaConfig config) {
        return mfaConfigRepository.save(config);
    }

    /**
     * Nettoyage automatique des codes expirés
     */
    public void nettoyerCodesExpires() {
        int supprime = otpCodeRepository.deleteExpiredCodes(LocalDateTime.now());
        if (supprime > 0) {
            System.out.println("MFA - " + supprime + " codes expires nettoyes.");
        }
    }
}