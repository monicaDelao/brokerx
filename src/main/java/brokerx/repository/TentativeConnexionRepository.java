package brokerx.repository;

import brokerx.entity.TentativeConnexion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-02 Step 2: Repository pour la gestion des tentatives de connexion
 * Support anti-brute force et réputation IP
 */
@Repository
public interface TentativeConnexionRepository extends JpaRepository<TentativeConnexion, Long> {

    /**
     * Compte les tentatives échouées pour une IP dans une période donnée
     * @param adresseIp L'adresse IP à vérifier
     * @param depuis Le timestamp de début de période
     * @return Nombre de tentatives échouées
     */
    @Query("SELECT COUNT(t) FROM TentativeConnexion t WHERE t.adresseIp = :adresseIp AND t.tentativeReussie = false AND t.timestampTentative >= :depuis")
    long compterTentativesEchoueesParIp(@Param("adresseIp") String adresseIp, @Param("depuis") LocalDateTime depuis);

    /**
     * Compte les tentatives échouées pour un email dans une période donnée
     * @param email L'email à vérifier
     * @param depuis Le timestamp de début de période
     * @return Nombre de tentatives échouées
     */
    @Query("SELECT COUNT(t) FROM TentativeConnexion t WHERE t.email = :email AND t.tentativeReussie = false AND t.timestampTentative >= :depuis")
    long compterTentativesEchoueesParEmail(@Param("email") String email, @Param("depuis") LocalDateTime depuis);

    /**
     * Trouve la dernière tentative réussie pour une IP
     * @param adresseIp L'adresse IP
     * @return La dernière tentative réussie
     */
    @Query("SELECT t FROM TentativeConnexion t WHERE t.adresseIp = :adresseIp AND t.tentativeReussie = true ORDER BY t.timestampTentative DESC")
    List<TentativeConnexion> trouverDerniereTentativeReussieParIp(@Param("adresseIp") String adresseIp);

    /**
     * Trouve toutes les tentatives pour une IP dans une période
     * @param adresseIp L'adresse IP
     * @param depuis Le timestamp de début
     * @return Liste des tentatives
     */
    @Query("SELECT t FROM TentativeConnexion t WHERE t.adresseIp = :adresseIp AND t.timestampTentative >= :depuis ORDER BY t.timestampTentative DESC")
    List<TentativeConnexion> trouverTentativesParIpDepuis(@Param("adresseIp") String adresseIp, @Param("depuis") LocalDateTime depuis);

    /**
     * Compte le nombre total de tentatives échouées pour une IP (réputation)
     * @param adresseIp L'adresse IP
     * @return Nombre total de tentatives échouées
     */
    @Query("SELECT COUNT(t) FROM TentativeConnexion t WHERE t.adresseIp = :adresseIp AND t.tentativeReussie = false")
    long compterTotalTentativesEchoueesParIp(@Param("adresseIp") String adresseIp);

    /**
     * Trouve toutes les tentatives récentes (dernières 24h) pour analyse
     * @param depuis Le timestamp de début (24h)
     * @return Liste des tentatives récentes
     */
    @Query("SELECT t FROM TentativeConnexion t WHERE t.timestampTentative >= :depuis ORDER BY t.timestampTentative DESC")
    List<TentativeConnexion> trouverTentativesRecentes(@Param("depuis") LocalDateTime depuis);
}