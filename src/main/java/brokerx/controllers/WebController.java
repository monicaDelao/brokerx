package brokerx.controllers;

import brokerx.entity.Client;
import brokerx.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import jakarta.validation.Valid;
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
    
    // Stockage temporaire des sessions de vérification
    private final Map<String, SessionVerification> sessions = new HashMap<>();

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
        
        // Debug: afficher les valeurs reçues
        System.out.println("DEBUG - Données reçues:");
        System.out.println("  - Prénom: " + client.getPrenom());
        System.out.println("  - Nom: " + client.getNom());
        System.out.println("  - Email: " + client.getEmail());
        System.out.println("  - Téléphone: " + client.getTelephone());
        System.out.println("  - Date de naissance: " + client.getDateNaissance());
        System.out.println("  - Adresse: " + client.getAdresse());
        System.out.println("  - Erreurs: " + result.hasErrors());
        if (result.hasErrors()) {
            System.out.println("  - Détails erreurs: " + result.getAllErrors());
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
            
            System.out.println("🎯 CU01 - Étape 2 complétée: Compte PENDING créé avec notifications envoyées");
            System.out.println("📝 Session créée - ID: " + sessionId);
            System.out.println("📧 Email: " + session.email);
            System.out.println("🔑 Code Email: " + session.codeEmail);
            System.out.println("📱 Code OTP: " + session.codeOTP);
            System.out.println("🗂️ Sessions actives: " + sessions.size());
            
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
        System.out.println("🔍 ACCÈS GET /verification-email-session avec sessionId: " + sessionId);
        
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
        
        // 🎉 Inscription complètement terminée avec audit
        System.out.println("🎉 Compte ACTIVÉ avec audit pour: " + session.email + " | ID Audit: " + auditId);
        
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
        
        // ✅ Code OTP validé avec succès
        System.out.println("✅ Vérification OTP réussie pour: " + session.email + " avec code: " + codeVerification);
        clientService.marquerTelephoneVerifie(session.email);
        
        // 🎉 Inscription complètement terminée
        System.out.println("🎉 Inscription complète avec email et téléphone vérifiés pour: " + session.email);
        redirectAttributes.addFlashAttribute("message", "Inscription réussie ! Votre compte est maintenant entièrement activé.");
        return "redirect:/inscription/succes";
    }

    @GetMapping("/inscription-confirmation")
    public String inscriptionConfirmation(@RequestParam String sessionId, Model model) {
        System.out.println("📋 ACCÈS /inscription-confirmation avec sessionId: " + sessionId);
        
        SessionVerification session = sessions.get(sessionId);
        if (session == null) {
            System.out.println("❌ Session introuvable dans /inscription-confirmation");
            model.addAttribute("error", "Session expirée ou invalide");
            return "redirect:/inscription";
        }
        
        System.out.println("✅ Session trouvée pour /inscription-confirmation - Email: " + session.email);
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
            System.out.println("🔗 ACCÈS via lien email avec code: " + code);
            
            // Trouver la session correspondant au code
            for (Map.Entry<String, SessionVerification> entry : sessions.entrySet()) {
                SessionVerification session = entry.getValue();
                if (code.equals(session.codeEmail)) {
                    System.out.println("✅ Session trouvée pour code email: " + session.email);
                    
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
                               Model model,
                               RedirectAttributes redirectAttributes) {
        
        try {
            // Vérifier si l'utilisateur existe et si le mot de passe est correct
            Optional<Client> clientOpt = clientService.trouverParEmail(email);
            
            if (clientOpt.isEmpty()) {
                model.addAttribute("error", "Email ou mot de passe incorrect.");
                model.addAttribute("email", email);
                return "connexion";
            }
            
            Client client = clientOpt.get();
            
            // Vérifier le mot de passe
            if (!client.getMotDePasse().equals(motDePasse)) {
                model.addAttribute("error", "Email ou mot de passe incorrect.");
                model.addAttribute("email", email);
                return "connexion";
            }
            
            // Vérifier si le compte est actif
            if (!client.isCompteActif()) {
                model.addAttribute("error", "Votre compte n'est pas encore activé. Veuillez vérifier votre email.");
                model.addAttribute("email", email);
                return "connexion";
            }
            
            // Connexion réussie - rediriger vers la page d'accueil avec message
            redirectAttributes.addFlashAttribute("message", "Connexion réussie ! Bienvenue " + client.getNom() + ".");
            return "redirect:/";
            
        } catch (Exception e) {
            model.addAttribute("error", "Une erreur s'est produite lors de la connexion.");
            model.addAttribute("email", email);
            return "connexion";
        }
    }
    
    // Classe interne pour les sessions de vérification
    private static class SessionVerification {
        String email;
        String codeEmail;
        String codeOTP;
        boolean emailVerifie = false;
    }
}