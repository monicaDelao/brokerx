package brokerx.controllers;

import brokerx.entity.Client;
import brokerx.service.ClientService;
import brokerx.service.SecurityService;
import brokerx.service.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur monolithique MVC pour BrokerX
 * Gère l'inscription et la vérification des clients
 */
@Controller
public class WebController {

    @Autowired
    private ClientService clientService;
    
    @Autowired
    private SecurityService securityService;
    
    @Autowired
    private MfaService mfaService;
    
    // Stockage temporaire des sessions de vérification
    private final Map<String, SessionVerification> sessions = new HashMap<>();
    
    // Stockage temporaire des sessions MFA en attente
    private final Map<String, SessionMfa> sessionsMfa = new HashMap<>();

    /**
     * UC-02 Step 2: Utilitaire pour obtenir l'adresse IP du client
     * Gère les proxies et load balancers
     */
    private String obtenirAdresseIpClient(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Prendre la première IP si plusieurs (client original)
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Configuration du binding pour les formulaires
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Trimmer pour supprimer les espaces en début/fin
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "BrokerX - Plateforme de Courtage");
        return "home";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, @ModelAttribute("client") Client client) {
        model.addAttribute("pageTitle", "Dashboard - BrokerX");
        if (client == null || client.getEmail() == null) {
            return "redirect:/connexion";
        }
        model.addAttribute("client", client);
        return "dashboard";
    }

    @GetMapping("/inscription")
    public String inscription(Model model) {
        model.addAttribute("pageTitle", "Inscription - BrokerX");
        if (!model.containsAttribute("client")) {
            model.addAttribute("client", new Client());
        }
        return "inscription";
    }
    
    @PostMapping("/inscription")
    public String traiterInscription(@Valid @ModelAttribute("client") Client client,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        
       
        if (result.hasErrors()) {
            System.out.println("  - Details erreurs: " + result.getAllErrors());
        }
        
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Inscription - BrokerX");
            return "inscription";
        }
        
        try {
            // Créer le client avec vérifications (CU01 - Étape 2)
            ClientService.CreationResult creationResult = clientService.creerClientAvecVerification(client);
            
            // Créer session de vérification pour le processus web avec les codes générés
            String sessionId = java.util.UUID.randomUUID().toString();
            SessionVerification session = new SessionVerification();
            session.email = creationResult.getClient().getEmail();
            session.codeEmail = creationResult.getCodeEmail();
            session.codeOTP = creationResult.getCodeOTP();
            sessions.put(sessionId, session);
            
            System.out.println("CU01 - Etape 2 completee: Compte PENDING cree avec notifications envoyees");
            System.out.println("Session creee - ID: " + sessionId);
            System.out.println("Email: " + session.email);
            System.out.println("Code Email: " + session.codeEmail);
            System.out.println("Code OTP: " + session.codeOTP);
            System.out.println("Sessions actives: " + sessions.size());
            
            redirectAttributes.addAttribute("sessionId", sessionId);
            redirectAttributes.addFlashAttribute("email", creationResult.getClient().getEmail());
            return "redirect:/inscription-confirmation";
            
        } catch (IllegalArgumentException e) {
            // Gestion des erreurs de compte existant
            if (e.getMessage().contains("email")) {
                result.rejectValue("email", "error.client", e.getMessage());
            } else if (e.getMessage().contains("téléphone")) {
                result.rejectValue("telephone", "error.client", e.getMessage());
            } else {
                result.reject("error.client", e.getMessage());
            }
            
            model.addAttribute("pageTitle", "Inscription - BrokerX");
            return "inscription";
        }
    }

    @GetMapping("/verification-email-session")
    public String verificationEmail(@RequestParam(required = false) String sessionId, Model model) {
        System.out.println("ACCES GET /verification-email-session avec sessionId: " + sessionId);
        
        // Vérifier si le sessionId est fourni
        if (sessionId == null || sessionId.trim().isEmpty()) {
            model.addAttribute("error", "Session manquante. Veuillez recommencer l'inscription.");
            return "redirect:/inscription";
        }
        
        // Vérifier si la session existe
        SessionVerification session = sessions.get(sessionId);
        if (session == null) {
            System.out.println("Session introuvable pour sessionId: " + sessionId);
            model.addAttribute("error", "Session expirée ou invalide. Veuillez recommencer l'inscription.");
            return "redirect:/inscription";
        }
        
        System.out.println("Session trouvee pour email: " + session.email);
        model.addAttribute("pageTitle", "Verification Email - BrokerX");
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("email", session.email);
        return "verification-email";
    }

    @PostMapping("/verification-email")
    public String traiterVerificationEmail(@RequestParam String sessionId,
                                         @RequestParam String codeVerification,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        
        SessionVerification session = sessions.get(sessionId);
        if (session == null) {
            model.addAttribute("pageTitle", "Vérification Email - BrokerX");
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("error", "Session expirée");
            return "verification-email";
        }
        
        // Validation du code de vérification email
        if (codeVerification == null || codeVerification.trim().isEmpty()) {
            model.addAttribute("pageTitle", "Vérification Email - BrokerX");
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("error", "Veuillez saisir le code de vérification");
            return "verification-email";
        }
        
        // Vérifier si le code correspond à celui généré
        if (!codeVerification.trim().equals(session.codeEmail)) {
            model.addAttribute("pageTitle", "Vérification Email - BrokerX");
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("error", "Code de vérification incorrect. Vérifiez votre email.");
            return "verification-email";
        }
        
        // Code email validé avec succès - ACTIVATION AVEC AUDIT
        //System.out.println("Verification email reussie pour: " + session.email + " avec code: " + codeVerification);
        
        // ACTIVER LE COMPTE AVEC JOURNALISATION D'AUDIT COMPLÈTE
        // Implémente: "Le Système passe le compte à Active et journalise l'audit (horodatage, empreinte des documents)"
        String auditId = clientService.activerCompteAvecAudit(session.email, codeVerification);
        session.emailVerifie = true;
        
        // Inscription complètement terminée avec audit
        System.out.println("Compte ACTIVE avec audit pour: " + session.email + " | ID Audit: " + auditId);
        
        // Supprimer la session car le processus est terminé
        sessions.remove(sessionId);
        
        redirectAttributes.addFlashAttribute("message", "Félicitations ! Votre compte est maintenant actif. Vous pouvez vous connecter.");
        redirectAttributes.addFlashAttribute("email", session.email);
        return "redirect:/inscription/succes";
    }

    @GetMapping("/verification-otp")
    public String verificationOtp(@RequestParam String sessionId, Model model) {
        model.addAttribute("pageTitle", "Vérification OTP - BrokerX");
        model.addAttribute("sessionId", sessionId);
        return "verification-otp";
    }

    @PostMapping("/verification-otp")
    public String traiterVerificationOTP(@RequestParam String sessionId,
                                       @RequestParam String codeVerification,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        
        SessionVerification session = sessions.get(sessionId);
        
        // Vérifications de sécurité
        if (session == null) {
            model.addAttribute("pageTitle", "Vérification OTP - BrokerX");
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("error", "Session expirée. Veuillez recommencer l'inscription.");
            return "verification-otp";
        }
        
        if (!session.emailVerifie) {
            model.addAttribute("pageTitle", "Vérification OTP - BrokerX");
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("error", "Veuillez d'abord vérifier votre email.");
            return "verification-otp";
        }
        
        // Validation du code OTP
        if (codeVerification == null || codeVerification.trim().isEmpty()) {
            model.addAttribute("pageTitle", "Vérification OTP - BrokerX");
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("error", "Veuillez saisir le code reçu par SMS");
            return "verification-otp";
        }
        
        if (!codeVerification.trim().equals(session.codeOTP)) {
            model.addAttribute("pageTitle", "Vérification OTP - BrokerX");
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("error", "Code SMS incorrect. Vérifiez le code reçu sur votre téléphone.");
            return "verification-otp";
        }
        
       
        System.out.println("Verification OTP reussie pour: " + session.email + " avec code: " + codeVerification);
        clientService.marquerTelephoneVerifie(session.email);
        
        // Inscription complètement terminée
        System.out.println("Inscription complète avec email et téléphone vérifiés pour: " + session.email);
        redirectAttributes.addFlashAttribute("message", "Inscription réussie ! Votre compte est maintenant entièrement activé.");
        return "redirect:/inscription/succes";
    }

    @GetMapping("/inscription-confirmation")
    public String inscriptionConfirmation(@RequestParam String sessionId, Model model) {
        System.out.println("ACCÈS /inscription-confirmation avec sessionId: " + sessionId);
        
        SessionVerification session = sessions.get(sessionId);
        if (session == null) {
            System.out.println("Session introuvable dans /inscription-confirmation");
            model.addAttribute("error", "Session expirée ou invalide");
            return "redirect:/inscription";
        }
        
        System.out.println("Session trouvée pour /inscription-confirmation - Email: " + session.email);
        model.addAttribute("pageTitle", "Vérifiez votre email - BrokerX");
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("email", session.email);
        
        System.out.println("🔗 SessionId ajouté au modèle: " + sessionId);
        return "inscription-confirmation";
    }

    /**
     * Route pour vérification directe via lien email (critère d'acceptation)
     * Permet de "cliquer sur le lien" comme spécifié dans les critères
     */
    @GetMapping("/verification-email")
    public String verificationEmailDepuisLien(@RequestParam(required = false) String code, 
                                             @RequestParam(required = false) String sessionId,
                                             Model model) {
        // Si c'est un lien direct depuis l'email avec code
        if (code != null && !code.trim().isEmpty()) {
            System.out.println("ACCÈS via lien email avec code: " + code);
            
            // Trouver la session correspondant au code
            for (Map.Entry<String, SessionVerification> entry : sessions.entrySet()) {
                SessionVerification session = entry.getValue();
                if (code.equals(session.codeEmail)) {
                    System.out.println("Session trouvée pour code email: " + session.email);
                    
                    // Activer directement le compte (critère: "clique sur le lien, et son compte est activé")
                    String auditId = clientService.activerCompteAvecAudit(session.email, code);
                    
                    // Supprimer la session
                    sessions.remove(entry.getKey());
                    
                    model.addAttribute("message", "Félicitations ! Votre compte a été activé en cliquant sur le lien. Vous pouvez maintenant vous connecter.");
                    model.addAttribute("email", session.email);
                    model.addAttribute("auditId", auditId);
                    return "inscription-succes";
                }
            }
            
            // Code non trouvé
            model.addAttribute("error", "Lien de vérification invalide ou expiré. Veuillez recommencer l'inscription.");
            return "redirect:/inscription";
        }
        
        // Si c'est un accès normal avec sessionId (existant) - rediriger vers la route spécialisée
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            return "redirect:/verification-email-session?sessionId=" + sessionId;
        }
        
        // Aucun paramètre valide
        model.addAttribute("error", "Accès invalide. Veuillez recommencer l'inscription.");
        return "redirect:/inscription";
    }

    @GetMapping("/inscription/succes")
    public String inscriptionSucces(Model model) {
        model.addAttribute("pageTitle", "Inscription Réussie - BrokerX");
        return "inscription-succes";
    }

    @GetMapping("/connexion")
    public String connexion(Model model) {
        model.addAttribute("pageTitle", "Connexion - BrokerX");
        return "connexion";
    }
    
    @PostMapping("/connexion")
    public String connexionPost(@RequestParam String email,
                               @RequestParam String motDePasse,
                               HttpServletRequest request,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        
        // UC-02 Step 2: Obtenir les informations de sécurité
        String adresseIp = obtenirAdresseIpClient(request);
        String userAgent = request.getHeader("User-Agent");
        
        System.out.println("UC-02 Step 2 - Tentative de connexion: IP=" + adresseIp + " | Email=" + email);
        
        try {
            // UC-02 Step 2: Vérification anti-brute force et réputation IP
            SecurityService.ResultatSecurite resultatSecurite = securityService.verifierAutorisationConnexion(adresseIp, email);
            
            if (!resultatSecurite.isAcceAutorise()) {
                model.addAttribute("error", resultatSecurite.getRaisonRefus());
                model.addAttribute("email", email);
                
                // Enregistrer la tentative bloquée
                securityService.enregistrerTentativeConnexion(adresseIp, email, false, userAgent, 
                    "Bloque par protection anti-brute force: " + resultatSecurite.getRaisonRefus());
                
                System.out.println("UC-02 Step 2 - CONNEXION BLOQUEE: " + resultatSecurite.getRaisonRefus());
                return "connexion";
            }
            
            // UC-02 Step 2: Vérifier si l'utilisateur existe
            Optional<Client> clientOpt = clientService.trouverParEmail(email);
            
            if (clientOpt.isEmpty()) {
                model.addAttribute("error", "Email ou mot de passe incorrect.");
                model.addAttribute("email", email);
                
                // Enregistrer tentative échouée (utilisateur inexistant)
                securityService.enregistrerTentativeConnexion(adresseIp, email, false, userAgent, "Utilisateur inexistant");
                System.out.println("UC-02 Step 2 - CONNEXION REFUSEE: Utilisateur inexistant pour " + email);
                return "connexion";
            }
            
            Client client = clientOpt.get();
            
            // UC-02 Step 2: Vérifier le mot de passe
            if (!client.getMotDePasse().equals(motDePasse)) {
                model.addAttribute("error", "Email ou mot de passe incorrect.");
                model.addAttribute("email", email);
                
                // Enregistrer tentative échouée (mot de passe incorrect)
                securityService.enregistrerTentativeConnexion(adresseIp, email, false, userAgent, "Mot de passe incorrect");
                System.out.println("UC-02 Step 2 - CONNEXION REFUSEE: Mot de passe incorrect pour " + email);
                return "connexion";
            }
            
            // UC-02 Step 2: Vérifier que le compte est bien actif (EMAIL_VERIFIE = TRUE)
            if (!client.isEmailVerifie()) {
                model.addAttribute("error", "Votre email n'est pas verifie. Vous devez verifier votre email avant de vous connecter.");
                model.addAttribute("email", email);
                
                // Enregistrer tentative échouée (email non vérifié)
                securityService.enregistrerTentativeConnexion(adresseIp, email, false, userAgent, "Email non verifie");
                System.out.println("UC-02 Step 2 - CONNEXION REFUSEE: Email non verifie pour " + email);
                return "connexion";
            }
            
            // UC-02 Step 2: Vérifier que le compte est actif
            if (!client.isCompteActif()) {
                model.addAttribute("error", "Votre compte n'est pas encore actif. Veuillez completer la verification.");
                model.addAttribute("email", email);
                
                // Enregistrer tentative échouée (compte non actif)
                securityService.enregistrerTentativeConnexion(adresseIp, email, false, userAgent, "Compte non actif");
                System.out.println("UC-02 Step 2 - CONNEXION REFUSEE: Compte non actif pour " + email);
                return "connexion";
            }
            
            // UC-02 Step 3: Vérifier si MFA est requise
            boolean mfaRequise = mfaService.isMfaRequise(client);
            
            if (mfaRequise) {
                // MFA requise - Créer session MFA et rediriger vers saisie OTP
                String sessionMfaId = java.util.UUID.randomUUID().toString();
                SessionMfa sessionMfa = new SessionMfa(email, client.getId(), adresseIp);
                
                // Déterminer quelles méthodes MFA sont disponibles
                brokerx.entity.MfaConfig configMfa = mfaService.obtenirOuCreerConfigMfa(client);
                sessionMfa.mfaRequise = true;
                sessionMfa.totpDisponible = configMfa.isTotpActive();
                sessionMfa.smsDisponible = configMfa.isSmsActive();
                
                if (configMfa.isSmsActive() && client.getTelephone() != null) {
                    // Masquer le numéro de téléphone (ex: +1514***3333)
                    String tel = client.getTelephone();
                    if (tel.length() > 4) {
                        sessionMfa.numeroTelephoneMasque = tel.substring(0, Math.min(tel.length() - 4, 4)) + "***" + tel.substring(tel.length() - 4);
                    } else {
                        sessionMfa.numeroTelephoneMasque = "***" + tel;
                    }
                    
                    // Générer et envoyer code SMS automatiquement
                    String codeSms = mfaService.genererEtEnvoyerSmsOtp(client, adresseIp);
                    if (codeSms != null) {
                        System.out.println("UC-02 Step 3 - Code SMS MFA envoye pour: " + email);
                    }
                }
                
                // Sauvegarder session MFA
                sessionsMfa.put(sessionMfaId, sessionMfa);
                
                System.out.println("UC-02 Step 3 - MFA REQUISE pour: " + email + " | TOTP: " + sessionMfa.totpDisponible + " | SMS: " + sessionMfa.smsDisponible);
                
                // Rediriger vers page de saisie OTP
                redirectAttributes.addAttribute("sessionMfaId", sessionMfaId);
                return "redirect:/mfa-verification";
            }
            
            // UC-02 Step 2: Connexion réussie sans MFA - Enregistrer tentative réussie
            securityService.enregistrerTentativeConnexion(adresseIp, email, true, userAgent, null);
            
            System.out.println("UC-02 - CONNEXION COMPLETE pour: " + email + " | IP: " + adresseIp + " | MFA: Non requise");
            
            redirectAttributes.addFlashAttribute("message", "Connexion reussie ! Bienvenue " + client.getNom() + ".");
            redirectAttributes.addFlashAttribute("client", client);
            return "redirect:/dashboard";
            
        } catch (Exception e) {
            // Enregistrer tentative échouée (erreur système)
            securityService.enregistrerTentativeConnexion(adresseIp, email, false, userAgent, "Erreur systeme: " + e.getMessage());
            
            model.addAttribute("error", "Une erreur s'est produite lors de la connexion.");
            model.addAttribute("email", email);
            System.out.println("UC-02 Step 2 - ERREUR CONNEXION: " + e.getMessage());
            return "connexion";
        }
    }
    
    // UC-02 Step 3: Page de saisie du code MFA
    @GetMapping("/mfa-verification")
    public String mfaVerification(@RequestParam String sessionMfaId, Model model) {
        SessionMfa sessionMfa = sessionsMfa.get(sessionMfaId);
        
        if (sessionMfa == null) {
            model.addAttribute("error", "Session MFA expiree. Veuillez vous reconnecter.");
            return "redirect:/connexion";
        }
        
        model.addAttribute("pageTitle", "Verification MFA - BrokerX");
        model.addAttribute("sessionMfaId", sessionMfaId);
        model.addAttribute("email", sessionMfa.email);
        model.addAttribute("totpDisponible", sessionMfa.totpDisponible);
        model.addAttribute("smsDisponible", sessionMfa.smsDisponible);
        model.addAttribute("numeroTelephone", sessionMfa.numeroTelephoneMasque);
        
        return "mfa-verification";
    }
    
    // UC-02 Step 3: Traitement de la vérification MFA
    @PostMapping("/mfa-verification")
    public String mfaVerificationPost(@RequestParam String sessionMfaId,
                                     @RequestParam String codeOtp,
                                     @RequestParam(required = false) String action,
                                     HttpServletRequest request,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        
        SessionMfa sessionMfa = sessionsMfa.get(sessionMfaId);
        String adresseIp = obtenirAdresseIpClient(request);
        
        if (sessionMfa == null) {
            model.addAttribute("error", "Session MFA expiree. Veuillez vous reconnecter.");
            return "redirect:/connexion";
        }
        
        try {
            // Retrouver le client
            Optional<Client> clientOpt = clientService.trouverParEmail(sessionMfa.email);
            if (clientOpt.isEmpty()) {
                sessionsMfa.remove(sessionMfaId);
                return "redirect:/connexion";
            }
            
            Client client = clientOpt.get();
            
            // Gestion de l'action "renvoyer SMS"
            if ("renvoyer-sms".equals(action)) {
                String codeSms = mfaService.genererEtEnvoyerSmsOtp(client, adresseIp);
                if (codeSms != null) {
                    model.addAttribute("success", "Nouveau code SMS envoye.");
                    System.out.println("UC-02 Step 3 - Nouveau code SMS MFA envoye pour: " + sessionMfa.email);
                } else {
                    model.addAttribute("error", "Impossible d'envoyer le SMS. Limite atteinte.");
                }
                
                // Reafficher la page
                model.addAttribute("pageTitle", "Verification MFA - BrokerX");
                model.addAttribute("sessionMfaId", sessionMfaId);
                model.addAttribute("email", sessionMfa.email);
                model.addAttribute("totpDisponible", sessionMfa.totpDisponible);
                model.addAttribute("smsDisponible", sessionMfa.smsDisponible);
                model.addAttribute("numeroTelephone", sessionMfa.numeroTelephoneMasque);
                return "mfa-verification";
            }
            
            // Vérification du code OTP
            MfaService.ResultatMfa resultat = mfaService.verifierMfa(client, codeOtp, adresseIp);
            
            if (resultat.isVerificationReussie()) {
                // MFA réussie - Finaliser la connexion
                sessionsMfa.remove(sessionMfaId);
                
                // Enregistrer tentative de connexion réussie
                securityService.enregistrerTentativeConnexion(adresseIp, sessionMfa.email, true, 
                    request.getHeader("User-Agent"), "MFA validee: " + resultat.getMethodeMfaUtilisee());
                
                System.out.println("UC-02 Step 3 - MFA REUSSIE pour: " + sessionMfa.email + " | Methode: " + resultat.getMethodeMfaUtilisee());
                
                redirectAttributes.addFlashAttribute("message", "Connexion reussie ! Bienvenue " + client.getNom() + ".");
                return "redirect:/";
                
            } else {
                // MFA échouée
                model.addAttribute("error", resultat.getMessageErreur());
                model.addAttribute("pageTitle", "Verification MFA - BrokerX");
                model.addAttribute("sessionMfaId", sessionMfaId);
                model.addAttribute("email", sessionMfa.email);
                model.addAttribute("totpDisponible", sessionMfa.totpDisponible);
                model.addAttribute("smsDisponible", sessionMfa.smsDisponible);
                model.addAttribute("numeroTelephone", sessionMfa.numeroTelephoneMasque);
                
                System.out.println("UC-02 Step 3 - MFA ECHOUEE pour: " + sessionMfa.email + " | Code: " + codeOtp);
                return "mfa-verification";
            }
            
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la verification MFA.");
            System.out.println("UC-02 Step 3 - ERREUR MFA: " + e.getMessage());
            return "redirect:/connexion";
        }
    }
    
    // UC-02 Step 3: Configuration MFA pour utilisateurs
    @GetMapping("/mfa-config")
    public String mfaConfig(@RequestParam(required = false) String email, 
                           @RequestParam(required = false) String autoActivate,
                           Model model, 
                           RedirectAttributes redirectAttributes) {
        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Email requis pour configurer MFA.");
            return "redirect:/connexion";
        }
        
        Optional<Client> clientOpt = clientService.trouverParEmail(email);
        if (clientOpt.isEmpty()) {
            model.addAttribute("error", "Utilisateur introuvable.");
            return "redirect:/connexion";
        }
        
        Client client = clientOpt.get();
        brokerx.entity.MfaConfig configMfa = mfaService.obtenirOuCreerConfigMfa(client);
        
        // Auto-activation MFA SMS si demandé et numéro disponible
        if ("sms".equals(autoActivate) && client.getTelephone() != null && !client.getTelephone().trim().isEmpty() && !configMfa.isSmsActive()) {
            configMfa.setSmsActive(true);
            configMfa.setSmsNumero(client.getTelephone());
            configMfa.setMfaActive(true);
            mfaService.sauvegarderConfigMfa(configMfa);
            
            redirectAttributes.addFlashAttribute("success", "MFA SMS activée automatiquement pour " + email);
            System.out.println("UC-02 Step 3 - MFA SMS auto-activee pour: " + email);
        }
        
        model.addAttribute("pageTitle", "Configuration MFA - BrokerX");
        model.addAttribute("client", client);
        model.addAttribute("configMfa", configMfa);
        
        return "mfa-config-simple";
    }
    
    // UC-02 Step 3: Activation/désactivation MFA SMS
    @PostMapping("/mfa-config/sms")
    public String mfaConfigSms(@RequestParam String email,
                               @RequestParam(required = false) String action,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        
        try {
            Optional<Client> clientOpt = clientService.trouverParEmail(email);
            if (clientOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Utilisateur introuvable.");
                return "redirect:/connexion";
            }
            
            Client client = clientOpt.get();
            brokerx.entity.MfaConfig configMfa = mfaService.obtenirOuCreerConfigMfa(client);
            String adresseIp = obtenirAdresseIpClient(request);
            
            if ("activer".equals(action)) {
                // Activer SMS MFA
                if (client.getTelephone() == null || client.getTelephone().trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Numero de telephone requis pour activer SMS MFA.");
                } else {
                    configMfa.setSmsActive(true);
                    configMfa.setSmsNumero(client.getTelephone());
                    configMfa.setMfaActive(true);
                    mfaService.sauvegarderConfigMfa(configMfa);
                    
                    // Envoyer code de test
                    String codeTest = mfaService.genererEtEnvoyerSmsOtp(client, adresseIp);
                    if (codeTest != null) {
                        redirectAttributes.addFlashAttribute("success", "SMS MFA active ! Code de test envoye au " + client.getTelephone());
                        System.out.println("UC-02 Step 3 - SMS MFA active pour: " + email);
                    } else {
                        redirectAttributes.addFlashAttribute("error", "Erreur lors de l'envoi du SMS de test.");
                    }
                }
            } else if ("desactiver".equals(action)) {
                // Désactiver SMS MFA
                configMfa.setSmsActive(false);
                
                // Si aucune autre méthode active, désactiver MFA complètement
                if (!configMfa.isTotpActive() && !configMfa.isWebauthnActive()) {
                    configMfa.setMfaActive(false);
                }
                
                mfaService.sauvegarderConfigMfa(configMfa);
                redirectAttributes.addFlashAttribute("success", "SMS MFA desactive.");
                System.out.println("UC-02 Step 3 - SMS MFA desactive pour: " + email);
            }
            
            redirectAttributes.addAttribute("email", email);
            return "redirect:/mfa-config";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la configuration MFA.");
            System.out.println("Erreur configuration MFA SMS: " + e.getMessage());
            return "redirect:/connexion";
        }
    }
    
    // Classe interne pour les sessions de vérification
    private static class SessionVerification {
        String email;
        String codeEmail;
        String codeOTP;
        boolean emailVerifie = false;
    }


    // UC-02 Step 3: Classe interne pour les sessions MFA en attente
    private static class SessionMfa {
        String email;
        Long clientId;
        String adresseIp;
        boolean mfaRequise;
        boolean totpDisponible;
        boolean smsDisponible;
        String numeroTelephoneMasque;
        java.time.LocalDateTime dateCreation;
        
        public SessionMfa(String email, Long clientId, String adresseIp) {
            this.email = email;
            this.clientId = clientId;
            this.adresseIp = adresseIp;
            this.dateCreation = java.time.LocalDateTime.now();
        }
    }
}
