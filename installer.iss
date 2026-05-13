; ============================================================
;  VoucherManager VMS — Installeur Professionnel
;  Généré pour BTS SIO SLAM — MCCI Business School
;  Auteur  : Daniel Maminjanaharyr
;  Version : 1.0.0
; ============================================================

#define AppName      "VoucherManager VMS"
#define AppVersion   "1.0.0"
#define AppPublisher "MCCI Business School"
#define AppExeName   "VoucherManager VMS.exe"
#define AppSourceDir "target\jpackage-image\VoucherManager VMS"

[Setup]
; Identité
AppName                = {#AppName}
AppVersion             = {#AppVersion}
AppPublisher           = {#AppPublisher}
AppPublisherURL        = https://mcci.edu
AppSupportURL          = https://mcci.edu
AppUpdatesURL          = https://mcci.edu

; Installation
DefaultDirName         = {autopf}\{#AppName}
DefaultGroupName       = {#AppName}
DisableProgramGroupPage= no

; Sortie
OutputDir              = target\output
OutputBaseFilename     = VoucherManager-VMS-Setup-v{#AppVersion}

; Icône et style
SetupIconFile          = src\main\resources\vms.ico
WizardStyle            = modern

; Compression maximale
Compression            = lzma2/ultra64
SolidCompression       = yes
LZMAUseSeparateProcess = yes

; Privilèges et architecture
PrivilegesRequired     = lowest
ArchitecturesAllowed   = x64compatible
ArchitecturesInstallIn64BitMode = x64compatible

; Windows 10 minimum
MinVersion             = 10.0

; Désinstalleur propre dans "Applications"
Uninstallable          = yes
UninstallDisplayName   = {#AppName}
UninstallDisplayIcon   = {app}\{#AppExeName}
CreateUninstallRegKey  = yes

; Informations affichées dans Ajout/Suppression de programmes
VersionInfoVersion     = {#AppVersion}
VersionInfoCompany     = {#AppPublisher}
VersionInfoDescription = Système de Gestion de Bons Cadeaux
VersionInfoProductName = {#AppName}

[Languages]
Name: "french"; MessagesFile: "compiler:Languages\French.isl"

; ============================================================
;  PAGE 1 : Bienvenue
; ============================================================
[Messages]
WelcomeLabel1=Bienvenue dans l'assistant d'installation de [name]
WelcomeLabel2=Cet assistant va installer [name/ver] sur votre ordinateur.%n%nIl est recommandé de fermer toutes les autres applications avant de continuer.%n%nCliquez sur Suivant pour continuer.
FinishedHeadingLabel=Installation de [name] terminée
FinishedLabel=L'installation de [name] s'est terminée avec succès.%n%nCliquez sur Terminer pour quitter l'assistant.

; ============================================================
;  FICHIERS À COPIER (Pages 3, 5, 6)
; ============================================================
[Files]
; Copier toute l'image APP (exe + app/ + runtime/)
Source: "{#AppSourceDir}\*"; \
    DestDir: "{app}"; \
    Flags: ignoreversion recursesubdirs createallsubdirs

; ============================================================
;  TÂCHES — Page 4 : Raccourcis
; ============================================================
[Tasks]
; Raccourci Bureau (décoché par défaut — l'utilisateur choisit)
Name: "desktopicon"; \
    Description: "Créer un raccourci sur le Bureau"; \
    GroupDescription: "Raccourcis :"; \
    Flags: unchecked

; Menu Démarrer (coché par défaut)
Name: "startmenu"; \
    Description: "Créer un raccourci dans le Menu Démarrer"; \
    GroupDescription: "Raccourcis :"; \
    Flags: checkedonce

; ============================================================
;  ICÔNES — Raccourcis créés
; ============================================================
[Icons]
; Menu Démarrer
Name: "{group}\{#AppName}"; \
    Filename: "{app}\{#AppExeName}"; \
    IconFilename: "{app}\{#AppExeName}"; \
    Tasks: startmenu

; Bureau
Name: "{autodesktop}\{#AppName}"; \
    Filename: "{app}\{#AppExeName}"; \
    IconFilename: "{app}\{#AppExeName}"; \
    Tasks: desktopicon

; Désinstaller dans le menu démarrer
Name: "{group}\Désinstaller {#AppName}"; \
    Filename: "{uninstallexe}"; \
    Tasks: startmenu

; ============================================================
;  PAGE 7 : Lancer l'application à la fin
; ============================================================
[Run]
Filename: "{app}\{#AppExeName}"; \
    Description: "Lancer {#AppName} maintenant"; \
    Flags: nowait postinstall skipifsilent; \
    WorkingDir: "{app}"

; ============================================================
;  INFORMATIONS SUR L'ESPACE DISQUE (Page 3)
; ============================================================
[InstallDelete]
; Nettoyer les anciens fichiers avant réinstallation
Type: filesandordirs; Name: "{app}\runtime"
Type: filesandordirs; Name: "{app}\app"

; ============================================================
;  CODE PASCAL — Personnalisation des pages
; ============================================================
[Code]
// Page 2 — Licence (texte généré)
function GetLicenseText: String;
begin
  Result :=
    'CONTRAT DE LICENCE UTILISATEUR FINAL' + #13#10 +
    '=====================================' + #13#10 + #13#10 +
    'VoucherManager VMS — Version 1.0.0' + #13#10 +
    'Copyright © 2026 MCCI Business School. Tous droits réservés.' + #13#10 + #13#10 +
    'Ce logiciel est développé dans le cadre du BTS SIO SLAM.' + #13#10 +
    'Son utilisation est réservée aux employés autorisés de l''entreprise.' + #13#10 + #13#10 +
    'DROITS ACCORDÉS' + #13#10 +
    '  • Installer et utiliser ce logiciel sur tout ordinateur de l''entreprise.' + #13#10 +
    '  • Faire des sauvegardes à des fins de sécurité.' + #13#10 + #13#10 +
    'RESTRICTIONS' + #13#10 +
    '  • Ne pas redistribuer sans autorisation écrite.' + #13#10 +
    '  • Ne pas modifier, décompiler ou désassembler ce logiciel.' + #13#10 + #13#10 +
    'BASE DE DONNÉES' + #13#10 +
    '  Ce logiciel se connecte à une base de données distante.' + #13#10 +
    '  Une connexion Internet est requise pour son fonctionnement.' + #13#10 + #13#10 +
    'LIMITATION DE RESPONSABILITÉ' + #13#10 +
    '  Ce logiciel est fourni "tel quel" sans garantie d''aucune sorte.' + #13#10 + #13#10 +
    'En installant ce logiciel, vous acceptez les termes de ce contrat.';
end;

// Créer la page Licence
var
  LicensePage: TOutputMsgMemoWizardPage;

procedure InitializeWizard;
begin
  // Page Licence (Page 2) insérée après la page de bienvenue
  LicensePage := CreateOutputMsgMemoPage(
    wpWelcome,
    'Contrat de Licence',
    'Veuillez lire attentivement le contrat de licence avant de continuer.',
    'Contrat de Licence Utilisateur Final :',
    GetLicenseText()
  );
end;
