package brokerx.repository;

import brokerx.entity.MfaConfig;
import brokerx.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * UC-02 Step 3: Repository pour la configuration MFA des utilisateurs
 */
@Repository
public interface MfaConfigRepository extends JpaRepository<MfaConfig, Long> {

    /**
     * Trouve la configuration MFA pour un client
     * @param client Le client
     * @return Configuration MFA optionnelle
     */
    Optional<MfaConfig> findByClient(Client client);

    /**
     * Trouve la configuration MFA par ID client
     * @param clientId L'ID du client
     * @return Configuration MFA optionnelle
     */
    @Query("SELECT m FROM MfaConfig m WHERE m.client.id = :clientId")
    Optional<MfaConfig> findByClientId(@Param("clientId") Long clientId);

    /**
     * Trouve la configuration MFA par email client
     * @param email L'email du client
     * @return Configuration MFA optionnelle
     */
    @Query("SELECT m FROM MfaConfig m WHERE m.client.email = :email")
    Optional<MfaConfig> findByClientEmail(@Param("email") String email);

    /**
     * Trouve tous les clients avec MFA obligatoire
     * @return Liste des configurations MFA obligatoires
     */
    @Query("SELECT m FROM MfaConfig m WHERE m.mfaObligatoire = true")
    List<MfaConfig> findAllWithMfaObligatoire();

    /**
     * Trouve tous les clients avec MFA activée (pas nécessairement obligatoire)
     * @return Liste des configurations MFA actives
     */
    @Query("SELECT m FROM MfaConfig m WHERE m.mfaActive = true AND (m.totpActive = true OR m.smsActive = true OR m.webauthnActive = true)")
    List<MfaConfig> findAllWithMfaActive();

    /**
     * Compte le nombre de clients avec TOTP activé
     * @return Nombre de clients TOTP
     */
    @Query("SELECT COUNT(m) FROM MfaConfig m WHERE m.totpActive = true")
    long countByTotpActive();

    /**
     * Compte le nombre de clients avec SMS activé
     * @return Nombre de clients SMS
     */
    @Query("SELECT COUNT(m) FROM MfaConfig m WHERE m.smsActive = true")
    long countBySmsActive();

    /**
     * Vérifie si un client a MFA requise (obligatoire ou active avec méthode configurée)
     * @param clientId L'ID du client
     * @return True si MFA requise
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MfaConfig m WHERE m.client.id = :clientId AND (m.mfaObligatoire = true OR (m.mfaActive = true AND (m.totpActive = true OR m.smsActive = true OR m.webauthnActive = true)))")
    boolean isMfaRequiseForClient(@Param("clientId") Long clientId);
}