package brokerx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UC-02 Step 3: Codes OTP temporaires pour MFA
 * Stockage sécurisé des codes avec expiration
 */
@Entity
@Table(name = "MFA_OTP_CODES")
public class MfaOtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private Client client;

    @Column(name = "CODE_OTP", nullable = false, length = 10)
    private String codeOtp;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE_MFA", nullable = false, length = 20)
    private TypeMfa typeMfa;

    @Column(name = "UTILISE", nullable = false)
    private boolean utilise = false;

    @Column(name = "DATE_CREATION", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "DATE_EXPIRATION", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "DATE_UTILISATION")
    private LocalDateTime dateUtilisation;

    @Column(name = "ADRESSE_IP_CREATION", length = 45)
    private String adresseIpCreation;

    @Column(name = "ADRESSE_IP_UTILISATION", length = 45)
    private String adresseIpUtilisation;

    @Column(name = "NUMERO_TENTATIVES", nullable = false)
    private int numeroTentatives = 0;

    // Énumération des types MFA
    public enum TypeMfa {
        SMS,       // Code envoyé par SMS
        TOTP,      // Code généré par app TOTP
        EMAIL,     // Code envoyé par email (backup)
        BACKUP     // Code de récupération
    }

    // Constructeurs
    public MfaOtpCode() {
        this.dateCreation = LocalDateTime.now();
    }

    public MfaOtpCode(Client client, String codeOtp, TypeMfa typeMfa, int dureeValiditeMinutes) {
        this();
        this.client = client;
        this.codeOtp = codeOtp;
        this.typeMfa = typeMfa;
        this.dateExpiration = dateCreation.plusMinutes(dureeValiditeMinutes);
    }

    // Méthodes utilitaires
    public boolean isExpire() {
        return LocalDateTime.now().isAfter(dateExpiration);
    }

    public boolean isValide() {
        return !utilise && !isExpire();
    }

    public void marquerUtilise(String adresseIp) {
        this.utilise = true;
        this.dateUtilisation = LocalDateTime.now();
        this.adresseIpUtilisation = adresseIp;
    }

    public void incrementerTentatives() {
        this.numeroTentatives++;
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

    public String getCodeOtp() {
        return codeOtp;
    }

    public void setCodeOtp(String codeOtp) {
        this.codeOtp = codeOtp;
    }

    public TypeMfa getTypeMfa() {
        return typeMfa;
    }

    public void setTypeMfa(TypeMfa typeMfa) {
        this.typeMfa = typeMfa;
    }

    public boolean isUtilise() {
        return utilise;
    }

    public void setUtilise(boolean utilise) {
        this.utilise = utilise;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public LocalDateTime getDateUtilisation() {
        return dateUtilisation;
    }

    public void setDateUtilisation(LocalDateTime dateUtilisation) {
        this.dateUtilisation = dateUtilisation;
    }

    public String getAdresseIpCreation() {
        return adresseIpCreation;
    }

    public void setAdresseIpCreation(String adresseIpCreation) {
        this.adresseIpCreation = adresseIpCreation;
    }

    public String getAdresseIpUtilisation() {
        return adresseIpUtilisation;
    }

    public void setAdresseIpUtilisation(String adresseIpUtilisation) {
        this.adresseIpUtilisation = adresseIpUtilisation;
    }

    public int getNumeroTentatives() {
        return numeroTentatives;
    }

    public void setNumeroTentatives(int numeroTentatives) {
        this.numeroTentatives = numeroTentatives;
    }

    @Override
    public String toString() {
        return "MfaOtpCode{" +
                "id=" + id +
                ", clientId=" + (client != null ? client.getId() : "null") +
                ", typeMfa=" + typeMfa +
                ", utilise=" + utilise +
                ", expire=" + isExpire() +
                ", tentatives=" + numeroTentatives +
                '}';
    }
}