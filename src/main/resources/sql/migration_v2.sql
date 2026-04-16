-- ============================================================================
-- VMS — Migration v2 : corrections audit + ajouts ParametresPanel
-- Exécuter dans pgAdmin sur la base VMS
-- ============================================================================

-- 1. Ajouter superviseur_id à magasin (si pas déjà présent)
ALTER TABLE magasin ADD COLUMN IF NOT EXISTS superviseur_id INT;
ALTER TABLE magasin DROP CONSTRAINT IF EXISTS fk_magasin_superviseur;
ALTER TABLE magasin ADD CONSTRAINT fk_magasin_superviseur
    FOREIGN KEY (superviseur_id) REFERENCES utilisateur(userid);

-- 2. Mettre à jour le CHECK sur demande.statuts (ajouter ARCHIVE)
ALTER TABLE demande DROP CONSTRAINT IF EXISTS chk_statuts;
ALTER TABLE demande ADD CONSTRAINT chk_statuts CHECK (statuts IN (
    'EN_ATTENTE_PAIEMENT','PAYE','APPROUVE','GENERE','ENVOYE','REJETE','ANNULE','ARCHIVE'
));

-- 3. Mettre à jour le CHECK sur audit_log.action (ajouter 7 nouvelles actions)
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS chk_action;
ALTER TABLE audit_log ADD CONSTRAINT chk_action CHECK (action IN (
    'CREATION','MODIFICATION','SUPPRESSION','PAIEMENT','APPROBATION',
    'GENERATION','ENVOI','REDEMPTION','REJET','ANNULATION',
    'CONNEXION','CONNEXION_ECHOUEE','INSCRIPTION',
    'CHANGEMENT_STATUT','ARCHIVAGE_MASSIF','UTILISATION_BON',
    'UPDATE_EMAIL'
));

-- 4. Mettre à jour le trigger pour gérer ARCHIVE
CREATE OR REPLACE FUNCTION trg_demande_date_modification()
RETURNS TRIGGER AS $$
BEGIN
    NEW.date_modification := CURRENT_TIMESTAMP;

    IF OLD.statuts IS DISTINCT FROM NEW.statuts THEN
        INSERT INTO audit_log (table_name, record_id, action, ancien_val, nouveau_val, contexte)
        VALUES (
            'demande',
            NEW.demande_id,
            CASE NEW.statuts
                WHEN 'PAYE'     THEN 'PAIEMENT'
                WHEN 'APPROUVE' THEN 'APPROBATION'
                WHEN 'GENERE'   THEN 'GENERATION'
                WHEN 'ENVOYE'   THEN 'ENVOI'
                WHEN 'REJETE'   THEN 'REJET'
                WHEN 'ANNULE'   THEN 'ANNULATION'
                WHEN 'ARCHIVE'  THEN 'ARCHIVAGE_MASSIF'
                ELSE 'MODIFICATION'
            END,
            jsonb_build_object('statuts', OLD.statuts),
            jsonb_build_object('statuts', NEW.statuts),
            'Changement automatique de statut via trigger'
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. Créer la table app_settings (paramètres applicatifs)
CREATE TABLE IF NOT EXISTS app_settings (
    setting_id    SERIAL PRIMARY KEY,
    setting_key   VARCHAR(50) NOT NULL UNIQUE,
    setting_value TEXT,
    smtp_server   VARCHAR(150),
    smtp_port     INT DEFAULT 587,
    smtp_username VARCHAR(150),
    smtp_password VARCHAR(255),
    tls_enabled   BOOLEAN DEFAULT TRUE,
    from_email    VARCHAR(150),
    from_name     VARCHAR(150),
    admin_email   VARCHAR(150),
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5b. Ajouter les colonnes from_name et admin_email si la table existait déjà
ALTER TABLE app_settings ADD COLUMN IF NOT EXISTS from_name VARCHAR(150);
ALTER TABLE app_settings ADD COLUMN IF NOT EXISTS admin_email VARCHAR(150);

-- 6. Insérer les données par défaut
INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('email', 'Configuration SMTP'),
    ('bon_validite_defaut', '365'),
    ('bon_type_defaut', 'Standard'),
    ('bon_entreprise', 'Intermart Maurice'),
    ('bon_format_qr', 'QR_CODE'),
    ('bon_signature', 'false')
ON CONFLICT (setting_key) DO NOTHING;

-- 7. Hasher le mot de passe admin si encore en clair
UPDATE utilisateur
SET password = '$2a$10$IHLDj8FrYmsg3payhyc6x.mVUo7JdHH.kP5YaFg2K9MvqgOzG.0/6'
WHERE username = 'admin' AND password = 'admin';

-- ============================================================================
-- Terminé ! Vérification :
-- ============================================================================
SELECT 'superviseur_id ajouté' AS check1 FROM information_schema.columns
    WHERE table_name = 'magasin' AND column_name = 'superviseur_id';
SELECT 'app_settings créée' AS check2 FROM information_schema.tables
    WHERE table_name = 'app_settings';
SELECT COUNT(*) AS nb_settings FROM app_settings;
