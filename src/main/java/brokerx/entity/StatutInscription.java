package brokerx.entity;

/**
 * Énumération des statuts d'inscription des clients
 */
public enum StatutInscription {
    PENDING("PENDING", "En attente de vérification"),
    EMAIL_VERIFIED("EMAIL_VERIFIED", "Email vérifié"),
    COMPLETE("COMPLETE", "Inscription complète"),
    SUSPENDED("SUSPENDED", "Compte suspendu"),
    REJECTED("REJECTED", "Inscription rejetée");
    
    private final String code;
    private final String description;
    
    StatutInscription(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return code;
    }
}