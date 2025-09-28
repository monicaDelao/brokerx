package brokerx.service;

import brokerx.entity.TentativeConnexion;
import brokerx.repository.TentativeConnexionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UC-02 Step 2: Service de sécurité pour anti-brute force et réputation IP
 * Implémente les protections système pour l'authentification
 */
@Service
public class SecurityService {

    @Autowired
    private TentativeConnexionRepository tentativeRepository;

    // Configuration sécurité - Ajustable selon les besoins
    private static final int MAX_TENTATIVES_PAR_IP_15MIN = 5;
    private static final int MAX_TENTATIVES_PAR_EMAIL_15MIN = 3;
    private static final int DUREE_BLOCAGE_MINUTES = 15;
    private static final int MAX_TENTATIVES_ECHECS_REPUTATION = 50;
    
    // IPs en liste blanche (développement, admin)
    private static final Set<String> IP_LISTE_BLANCHE = new HashSet<>(Arrays.asList(
        "127.0.0.1",      // localhost
        "::1",            // localhost IPv6
        "0:0:0:0:0:0:0:1" // localhost IPv6 alternative
    ));
    
    // IPs en liste noire (connues malveillantes)
    private static final Set<String> IP_LISTE_NOIRE = new HashSet<>();

    /**
     * Résultat de la vérification de sécurité
     */
    public static class ResultatSecurite {
        private boolean acceAutorise;
        private String raisonRefus;
        private int minutesRestantesAvantDeblocage;
        
        public ResultatSecurite(boolean acceAutorise, String raisonRefus, int minutesRestantes) {
            this.acceAutorise = acceAutorise;
            this.raisonRefus = raisonRefus;
            this.minutesRestantesAvantDeblocage = minutesRestantes;
        }
        
        // Getters
        public boolean isAcceAutorise() { return acceAutorise; }
        public String getRaisonRefus() { return raisonRefus; }
        public int getMinutesRestantesAvantDeblocage() { return minutesRestantesAvantDeblocage; }
    }

    /**
     * UC-02 Step 2: Vérifie si une tentative de connexion est autorisée
     * @param adresseIp L'adresse IP du client
     * @param email L'email de connexion
     * @return Résultat de la vérification de sécurité
     */
    public ResultatSecurite verifierAutorisationConnexion(String adresseIp, String email) {
        
        System.out.println("UC-02 Step 2 - Verification securite pour IP: " + adresseIp + " | Email: " + email);
        
        // 1. Vérifier liste noire IP
        if (IP_LISTE_NOIRE.contains(adresseIp)) {
            System.out.println("SECURITE - IP en liste noire: " + adresseIp);
            return new ResultatSecurite(false, "Adresse IP bloquee pour activite suspecte.", -1);
        }
        
        // 2. Vérifier liste blanche IP (bypass autres vérifications)
        if (IP_LISTE_BLANCHE.contains(adresseIp)) {
            System.out.println("SECURITE - IP en liste blanche (autorisee): " + adresseIp);
            return new ResultatSecurite(true, null, 0);
        }
        
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite15min = maintenant.minusMinutes(DUREE_BLOCAGE_MINUTES);
        
        // 3. Anti-brute force par IP
        long tentativesEchoueesIp = tentativeRepository.compterTentativesEchoueesParIp(adresseIp, limite15min);
        if (tentativesEchoueesIp >= MAX_TENTATIVES_PAR_IP_15MIN) {
            System.out.println("SECURITE - Trop de tentatives pour IP: " + adresseIp + " (" + tentativesEchoueesIp + " tentatives)");
            
            // Calculer minutes restantes de blocage
            List<TentativeConnexion> tentativesRecentes = tentativeRepository.trouverTentativesParIpDepuis(adresseIp, limite15min);
            if (!tentativesRecentes.isEmpty()) {
                LocalDateTime derniereTentative = tentativesRecentes.get(0).getTimestampTentative();
                LocalDateTime finBlocage = derniereTentative.plusMinutes(DUREE_BLOCAGE_MINUTES);
                int minutesRestantes = (int) java.time.Duration.between(maintenant, finBlocage).toMinutes();
                minutesRestantes = Math.max(1, minutesRestantes); // Au minimum 1 minute
                
                return new ResultatSecurite(false, 
                    "Trop de tentatives de connexion. Reessayez dans " + minutesRestantes + " minute(s).", 
                    minutesRestantes);
            }
        }
        
        // 4. Anti-brute force par email
        long tentativesEchoueesEmail = tentativeRepository.compterTentativesEchoueesParEmail(email, limite15min);
        if (tentativesEchoueesEmail >= MAX_TENTATIVES_PAR_EMAIL_15MIN) {
            System.out.println("SECURITE - Trop de tentatives pour email: " + email + " (" + tentativesEchoueesEmail + " tentatives)");
            return new ResultatSecurite(false, 
                "Trop de tentatives pour cet email. Reessayez dans " + DUREE_BLOCAGE_MINUTES + " minutes.", 
                DUREE_BLOCAGE_MINUTES);
        }
        
        // 5. Réputation IP (historique total)
        long totalEchecsIp = tentativeRepository.compterTotalTentativesEchoueesParIp(adresseIp);
        if (totalEchecsIp >= MAX_TENTATIVES_ECHECS_REPUTATION) {
            System.out.println("SECURITE - IP avec mauvaise reputation: " + adresseIp + " (" + totalEchecsIp + " echecs totaux)");
            // Pour une IP avec mauvaise réputation, on est plus strict
            if (tentativesEchoueesIp >= 2) {
                return new ResultatSecurite(false, 
                    "Adresse IP avec historique suspect. Acces temporairement limite.", 
                    DUREE_BLOCAGE_MINUTES);
            }
        }
        
        System.out.println("SECURITE - Connexion autorisee pour IP: " + adresseIp + " | Email: " + email);
        return new ResultatSecurite(true, null, 0);
    }

    /**
     * Enregistre une tentative de connexion (réussie ou échouée)
     * @param adresseIp L'adresse IP
     * @param email L'email
     * @param reussie True si connexion réussie
     * @param userAgent User-Agent du navigateur
     * @param raisonEchec Raison de l'échec si applicable
     */
    public void enregistrerTentativeConnexion(String adresseIp, String email, boolean reussie, 
                                            String userAgent, String raisonEchec) {
        
        TentativeConnexion tentative = new TentativeConnexion(adresseIp, email, reussie);
        tentative.setUserAgent(userAgent);
        if (!reussie && raisonEchec != null) {
            tentative.setRaisonEchec(raisonEchec);
        }
        
        tentativeRepository.save(tentative);
        
        String statusStr = reussie ? "REUSSIE" : "ECHOUEE";
        System.out.println("AUDIT - Tentative " + statusStr + " enregistree: IP=" + adresseIp + 
                          " | Email=" + email + " | Raison=" + raisonEchec);
    }

    /**
     * Ajoute une IP à la liste noire
     * @param adresseIp L'adresse IP à bloquer
     */
    public void ajouterIpListeNoire(String adresseIp) {
        IP_LISTE_NOIRE.add(adresseIp);
        System.out.println("SECURITE - IP ajoutee a la liste noire: " + adresseIp);
    }

    /**
     * Obtient des statistiques de sécurité pour monitoring
     * @return Map avec les statistiques
     */
    public void afficherStatistiquesSecurite() {
        LocalDateTime derniere24h = LocalDateTime.now().minusHours(24);
        List<TentativeConnexion> tentativesRecentes = tentativeRepository.trouverTentativesRecentes(derniere24h);
        
        long totalTentatives = tentativesRecentes.size();
        long tentativesReussies = tentativesRecentes.stream().mapToLong(t -> t.isTentativeReussie() ? 1 : 0).sum();
        long tentativesEchouees = totalTentatives - tentativesReussies;
        
        System.out.println("=== STATISTIQUES SECURITE (24h) ===");
        System.out.println("Total tentatives: " + totalTentatives);
        System.out.println("Tentatives reussies: " + tentativesReussies);
        System.out.println("Tentatives echouees: " + tentativesEchouees);
        System.out.println("IPs en liste noire: " + IP_LISTE_NOIRE.size());
        System.out.println("=====================================");
    }
}