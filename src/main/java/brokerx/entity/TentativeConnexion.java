package brokerx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UC-02 Step 2: Entité pour tracker les tentatives de connexion et sécurité IP
 * Gestion anti-brute force et réputation IP
 */
@Entity
@Table(name = "TENTATIVES_CONNEXION")
public class TentativeConnexion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ADRESSE_IP", nullable = false, length = 45)
    private String adresseIp;

    @Column(name = "EMAIL", length = 100)
    private String email;

    @Column(name = "TENTATIVE_REUSSIE", nullable = false)
    private boolean tentativeReussie = false;

    @Column(name = "TIMESTAMP_TENTATIVE", nullable = false)
    private LocalDateTime timestampTentative;

    @Column(name = "USER_AGENT", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "RAISON_ECHEC", length = 200)
    private String raisonEchec;

    // Constructeurs
    public TentativeConnexion() {
        this.timestampTentative = LocalDateTime.now();
    }

    public TentativeConnexion(String adresseIp, String email, boolean tentativeReussie) {
        this();
        this.adresseIp = adresseIp;
        this.email = email;
        this.tentativeReussie = tentativeReussie;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdresseIp() {
        return adresseIp;
    }

    public void setAdresseIp(String adresseIp) {
        this.adresseIp = adresseIp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isTentativeReussie() {
        return tentativeReussie;
    }

    public void setTentativeReussie(boolean tentativeReussie) {
        this.tentativeReussie = tentativeReussie;
    }

    public LocalDateTime getTimestampTentative() {
        return timestampTentative;
    }

    public void setTimestampTentative(LocalDateTime timestampTentative) {
        this.timestampTentative = timestampTentative;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRaisonEchec() {
        return raisonEchec;
    }

    public void setRaisonEchec(String raisonEchec) {
        this.raisonEchec = raisonEchec;
    }

    @Override
    public String toString() {
        return "TentativeConnexion{" +
                "id=" + id +
                ", adresseIp='" + adresseIp + '\'' +
                ", email='" + email + '\'' +
                ", tentativeReussie=" + tentativeReussie +
                ", timestampTentative=" + timestampTentative +
                ", raisonEchec='" + raisonEchec + '\'' +
                '}';
    }
}