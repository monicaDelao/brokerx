package brokerx.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
public class Client {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(min = 2, max = 50)
    @Column(nullable = false)
    private String prenom;
    
    @NotBlank
    @Size(min = 2, max = 50)
    @Column(nullable = false)
    private String nom;
    
    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;
    
    @Pattern(regexp = "^\\d{10}$", message = "Le numéro de téléphone doit contenir exactement 10 chiffres")
    @Column(nullable = true)
    private String telephone;
    
    @NotNull(message = "La date de naissance est obligatoire")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;
    
    @NotBlank(message = "L'adresse est obligatoire")
    @Size(min = 10, max = 200, message = "L'adresse doit contenir entre 10 et 200 caractères")
    @Column(nullable = false)
    private String adresse;
    
    @NotBlank
    @Size(min = 8)
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;
    
    @Column(name = "email_verifie")
    private boolean emailVerifie = false;
    
    @Column(name = "telephone_verifie")
    private boolean telephoneVerifie = false;
    
    @Column(name = "date_inscription")
    private LocalDateTime dateInscription;
    
    @Column(name = "status_inscription")
    private String statusInscription = "PENDING";
    
    public Client() {
        this.dateInscription = LocalDateTime.now();
    }
    
    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    
    public boolean isEmailVerifie() { return emailVerifie; }
    public void setEmailVerifie(boolean emailVerifie) { this.emailVerifie = emailVerifie; }
    
    public boolean isTelephoneVerifie() { return telephoneVerifie; }
    public void setTelephoneVerifie(boolean telephoneVerifie) { this.telephoneVerifie = telephoneVerifie; }
    
    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }
    
    public String getStatusInscription() { return statusInscription; }
    public void setStatusInscription(String statusInscription) { this.statusInscription = statusInscription; }
    
    public String getNomComplet() { return prenom + " " + nom; }
    
    public boolean isInscriptionComplete() {
        return emailVerifie && "ACTIVE".equals(statusInscription);
    }
    
    public boolean isCompteActif() {
        return "ACTIVE".equals(statusInscription);
    }
}