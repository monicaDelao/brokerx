package brokerx.repository;

import brokerx.entity.MfaOtpCode;
import brokerx.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UC-02 Step 3: Repository pour les codes OTP MFA
 */
@Repository
public interface MfaOtpCodeRepository extends JpaRepository<MfaOtpCode, Long> {

    /**
     * Trouve un code OTP valide pour un client et un type MFA
     * @param client Le client
     * @param codeOtp Le code OTP
     * @param typeMfa Le type MFA
     * @return Code OTP optionnel
     */
    @Query("SELECT m FROM MfaOtpCode m WHERE m.client = :client AND m.codeOtp = :codeOtp AND m.typeMfa = :typeMfa AND m.utilise = false AND m.dateExpiration > :maintenant")
    Optional<MfaOtpCode> findValidCode(@Param("client") Client client, 
                                       @Param("codeOtp") String codeOtp, 
                                       @Param("typeMfa") MfaOtpCode.TypeMfa typeMfa,
                                       @Param("maintenant") LocalDateTime maintenant);

    /**
     * Trouve tous les codes OTP valides pour un client
     * @param client Le client
     * @return Liste des codes valides
     */
    @Query("SELECT m FROM MfaOtpCode m WHERE m.client = :client AND m.utilise = false AND m.dateExpiration > :maintenant ORDER BY m.dateCreation DESC")
    List<MfaOtpCode> findValidCodesForClient(@Param("client") Client client, 
                                             @Param("maintenant") LocalDateTime maintenant);

    /**
     * Trouve le dernier code généré pour un client et un type MFA
     * @param client Le client
     * @param typeMfa Le type MFA
     * @return Code OTP optionnel
     */
    @Query("SELECT m FROM MfaOtpCode m WHERE m.client = :client AND m.typeMfa = :typeMfa ORDER BY m.dateCreation DESC")
    List<MfaOtpCode> findLatestCodeByType(@Param("client") Client client, 
                                          @Param("typeMfa") MfaOtpCode.TypeMfa typeMfa);

    /**
     * Compte les tentatives récentes pour un client (dernière heure)
     * @param client Le client  
     * @param depuis Timestamp de début
     * @return Nombre de tentatives
     */
    @Query("SELECT COUNT(m) FROM MfaOtpCode m WHERE m.client = :client AND m.dateCreation >= :depuis")
    long countRecentAttempts(@Param("client") Client client, 
                             @Param("depuis") LocalDateTime depuis);

    /**
     * Supprime les codes expirés (nettoyage automatique)
     * @param maintenant Timestamp actuel
     * @return Nombre de codes supprimés
     */
    @Query("DELETE FROM MfaOtpCode m WHERE m.dateExpiration < :maintenant")
    int deleteExpiredCodes(@Param("maintenant") LocalDateTime maintenant);

    /**
     * Trouve tous les codes expirés (pour audit avant suppression)
     * @param maintenant Timestamp actuel
     * @return Liste des codes expirés
     */
    @Query("SELECT m FROM MfaOtpCode m WHERE m.dateExpiration < :maintenant")
    List<MfaOtpCode> findExpiredCodes(@Param("maintenant") LocalDateTime maintenant);

    /**
     * Invalide tous les codes non utilisés pour un client (sécurité)
     * @param client Le client
     * @return Nombre de codes invalidés
     */
    @Query("UPDATE MfaOtpCode m SET m.utilise = true, m.dateUtilisation = :maintenant WHERE m.client = :client AND m.utilise = false")
    int invalidateAllCodesForClient(@Param("client") Client client, 
                                    @Param("maintenant") LocalDateTime maintenant);

    /**
     * Statistiques d'utilisation des codes OTP
     * @param depuis Période de début
     * @return Nombre de codes utilisés dans la période
     */
    @Query("SELECT COUNT(m) FROM MfaOtpCode m WHERE m.utilise = true AND m.dateUtilisation >= :depuis")
    long countUsedCodesSince(@Param("depuis") LocalDateTime depuis);
}