package brokerx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UC-02 Step 3: Configuration MFA par utilisateur
 * Gestion TOTP, SMS OTP et WebAuthn
 */
@Entity
@Table(name = "MFA_CONFIG")
public class MfaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false, unique = true)
    private Client client;

    @Column(name = "MFA_ACTIVE", nullable = false)
    private boolean mfaActive = false;

    @Column(name = "MFA_OBLIGATOIRE", nullable = false)
    private boolean mfaObligatoire = false;

    // Configuration TOTP
    @Column(name = "TOTP_ACTIVE", nullable = false)
    private boolean totpActive = false;

    @Column(name = "TOTP_SECRET_KEY", length = 100)
    private String totpSecretKey;

    @Column(name = "TOTP_BACKUP_CODES", columnDefinition = "TEXT")
    private String totpBackupCodes; // Codes de récupération séparés par virgule

    // Configuration SMS
    @Column(name = "SMS_ACTIVE", nullable = false)
    private boolean smsActive = false;

    @Column(name = "SMS_NUMERO", length = 20)
    private String smsNumero;

    // Configuration WebAuthn (futur)
    @Column(name = "WEBAUTHN_ACTIVE", nullable = false)
    private boolean webauthnActive = false;

    @Column(name = "WEBAUTHN_CREDENTIALS", columnDefinition = "TEXT")
    private String webauthnCredentials; // JSON des credentials WebAuthn

    // Métadonnées
    @Column(name = "DATE_CREATION", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "DATE_DERNIERE_MODIFICATION")
    private LocalDateTime dateDerniereModification;

    @Column(name = "DERNIERE_VERIFICATION_REUSSIE")
    private LocalDateTime derniereVerificationReussie;

    // Constructeurs
    public MfaConfig() {
        this.dateCreation = LocalDateTime.now();
    }

    public MfaConfig(Client client) {
        this();
        this.client = client;
    }

    // Méthodes utilitaires
    public boolean hasMfaMethodeActive() {
        return totpActive || smsActive || webauthnActive;
    }

    public boolean isMfaRequise() {
        return mfaObligatoire || (mfaActive && hasMfaMethodeActive());
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public boolean isMfaActive() {
        return mfaActive;
    }

    public void setMfaActive(boolean mfaActive) {
        this.mfaActive = mfaActive;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public boolean isMfaObligatoire() {
        return mfaObligatoire;
    }

    public void setMfaObligatoire(boolean mfaObligatoire) {
        this.mfaObligatoire = mfaObligatoire;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public boolean isTotpActive() {
        return totpActive;
    }

    public void setTotpActive(boolean totpActive) {
        this.totpActive = totpActive;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public String getTotpSecretKey() {
        return totpSecretKey;
    }

    public void setTotpSecretKey(String totpSecretKey) {
        this.totpSecretKey = totpSecretKey;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public String getTotpBackupCodes() {
        return totpBackupCodes;
    }

    public void setTotpBackupCodes(String totpBackupCodes) {
        this.totpBackupCodes = totpBackupCodes;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public boolean isSmsActive() {
        return smsActive;
    }

    public void setSmsActive(boolean smsActive) {
        this.smsActive = smsActive;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public String getSmsNumero() {
        return smsNumero;
    }

    public void setSmsNumero(String smsNumero) {
        this.smsNumero = smsNumero;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public boolean isWebauthnActive() {
        return webauthnActive;
    }

    public void setWebauthnActive(boolean webauthnActive) {
        this.webauthnActive = webauthnActive;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public String getWebauthnCredentials() {
        return webauthnCredentials;
    }

    public void setWebauthnCredentials(String webauthnCredentials) {
        this.webauthnCredentials = webauthnCredentials;
        this.dateDerniereModification = LocalDateTime.now();
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateDerniereModification() {
        return dateDerniereModification;
    }

    public void setDateDerniereModification(LocalDateTime dateDerniereModification) {
        this.dateDerniereModification = dateDerniereModification;
    }

    public LocalDateTime getDerniereVerificationReussie() {
        return derniereVerificationReussie;
    }

    public void setDerniereVerificationReussie(LocalDateTime derniereVerificationReussie) {
        this.derniereVerificationReussie = derniereVerificationReussie;
    }

    @Override
    public String toString() {
        return "MfaConfig{" +
                "id=" + id +
                ", clientId=" + (client != null ? client.getId() : "null") +
                ", mfaActive=" + mfaActive +
                ", mfaObligatoire=" + mfaObligatoire +
                ", totpActive=" + totpActive +
                ", smsActive=" + smsActive +
                ", webauthnActive=" + webauthnActive +
                '}';
    }
}