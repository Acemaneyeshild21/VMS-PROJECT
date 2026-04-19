-- ============================================================================
-- VMS (VoucherManager System) — Schéma PostgreSQL complet
-- MCCI Business School — BTS SIO SLAM — Session 2026
-- ============================================================================

-- ────────────────────────────────────────────────────────────────────────────
-- 1. TABLES PRINCIPALES
-- ────────────────────────────────────────────────────────────────────────────

-- Sociétés / Enseignes
CREATE TABLE IF NOT EXISTS societe (
    societe_id   SERIAL PRIMARY KEY,
    nom          VARCHAR(150) NOT NULL,
    adresse      TEXT,
    telephone    VARCHAR(30),
    email        VARCHAR(150),
    actif        BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Magasins (points de vente rattachés à une société)
CREATE TABLE IF NOT EXISTS magasin (
    magasin_id     SERIAL PRIMARY KEY,
    societe_id     INT REFERENCES societe(societe_id),
    nom_magasin    VARCHAR(150) NOT NULL,
    adresse        TEXT,
    telephone      VARCHAR(30),
    superviseur_id INT,
    actif          BOOLEAN DEFAULT TRUE,
    date_creation  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Utilisateurs (siège + magasins)
CREATE TABLE IF NOT EXISTS utilisateur (
    userid       SERIAL PRIMARY KEY,
    username     VARCHAR(80)  NOT NULL UNIQUE,
    email        VARCHAR(150) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    role         VARCHAR(50)  NOT NULL DEFAULT 'Collaborateur',
    magasin_id   INT REFERENCES magasin(magasin_id),
    actif        BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN (
        'Administrateur','Manager','Comptable','Approbateur','Collaborateur','Superviseur_Magasin'
    ))
);

-- FK différée : superviseur_id dans magasin → utilisateur (créé après magasin)
ALTER TABLE magasin
    ADD CONSTRAINT fk_magasin_superviseur
    FOREIGN KEY (superviseur_id) REFERENCES utilisateur(userid);

-- Clients (bénéficiaires des bons)
CREATE TABLE IF NOT EXISTS client (
    clientid       SERIAL PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    contact_number VARCHAR(30),
    company        VARCHAR(150),
    actif          BOOLEAN DEFAULT TRUE,
    date_creation  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Demandes de bons (cycle de vie complet)
CREATE TABLE IF NOT EXISTS demande (
    demande_id              SERIAL PRIMARY KEY,
    reference               VARCHAR(50) UNIQUE,
    invoice_reference       VARCHAR(50) UNIQUE,
    clientid                INT NOT NULL REFERENCES client(clientid),
    magasin_id              INT REFERENCES magasin(magasin_id),
    nombre_bons             INT NOT NULL DEFAULT 1,
    valeur_unitaire         NUMERIC(12,2) NOT NULL DEFAULT 0,
    montant_total           NUMERIC(14,2) GENERATED ALWAYS AS (nombre_bons * valeur_unitaire) STORED,
    type_bon                VARCHAR(30) DEFAULT 'Standard',
    validite_jours          INT DEFAULT 365,
    motif                   TEXT,
    description             TEXT,
    email                   VARCHAR(150),
    email_destinataire      VARCHAR(150),
    statuts                 VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE_PAIEMENT',

    -- Traçabilité création
    cree_par                INT REFERENCES utilisateur(userid),
    date_creation           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_demande            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Paiement
    paiement_valide         BOOLEAN DEFAULT FALSE,
    date_paiement           TIMESTAMP,
    valide_par              INT REFERENCES utilisateur(userid),

    -- Approbation
    approuve                BOOLEAN DEFAULT FALSE,
    approuve_par            INT REFERENCES utilisateur(userid),
    date_approbation        TIMESTAMP,

    -- Génération
    date_generation         TIMESTAMP,
    genere_par              INT REFERENCES utilisateur(userid),

    date_modification       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_statuts CHECK (statuts IN (
        'EN_ATTENTE_PAIEMENT','PAYE','APPROUVE','GENERE','ENVOYE','REJETE','ANNULE','ARCHIVE'
    )),
    CONSTRAINT chk_nombre_bons CHECK (nombre_bons > 0),
    CONSTRAINT chk_valeur_unitaire CHECK (valeur_unitaire >= 0)
);

-- Bons individuels (générés après approbation)
CREATE TABLE IF NOT EXISTS bon (
    bon_id          SERIAL PRIMARY KEY,
    demande_id      INT NOT NULL REFERENCES demande(demande_id),
    code_unique     VARCHAR(64) NOT NULL UNIQUE,
    qr_data         TEXT NOT NULL,
    valeur          NUMERIC(12,2) NOT NULL,
    date_emission   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP NOT NULL,
    statut          VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    pdf_path        TEXT,
    signature_hash  VARCHAR(128),

    CONSTRAINT chk_bon_statut CHECK (statut IN (
        'ACTIF','REDIME','EXPIRE','ANNULE'
    ))
);

-- Index pour recherche rapide par code
CREATE INDEX IF NOT EXISTS idx_bon_code ON bon(code_unique);
CREATE INDEX IF NOT EXISTS idx_bon_statut ON bon(statut);
CREATE INDEX IF NOT EXISTS idx_bon_expiration ON bon(date_expiration);

-- Rédemptions (enregistrement en magasin)
CREATE TABLE IF NOT EXISTS redemption (
    redemption_id   SERIAL PRIMARY KEY,
    bon_id          INT NOT NULL REFERENCES bon(bon_id),
    magasin_id      INT NOT NULL REFERENCES magasin(magasin_id),
    utilisateur_id  INT NOT NULL REFERENCES utilisateur(userid),
    date_redemption TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    commentaire     TEXT,

    CONSTRAINT uq_redemption_bon UNIQUE (bon_id) -- un bon = une seule rédemption
);

-- Audit trail (traçabilité complète.)
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id    SERIAL PRIMARY KEY,
    table_name  VARCHAR(50) NOT NULL,
    record_id   INT NOT NULL,
    action      VARCHAR(20) NOT NULL,
    ancien_val  JSONB,
    nouveau_val JSONB,
    utilisateur_id INT REFERENCES utilisateur(userid),
    username       VARCHAR(80),
    ip_address     VARCHAR(45),
    contexte       TEXT,
    date_action    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_action CHECK (action IN (
        'CREATION','MODIFICATION','SUPPRESSION','PAIEMENT','APPROBATION',
        'GENERATION','ENVOI','REDEMPTION','REJET','ANNULATION',
        'CONNEXION','CONNEXION_ECHOUEE','INSCRIPTION',
        'CHANGEMENT_STATUT','ARCHIVAGE_MASSIF','UTILISATION_BON',
        'UPDATE_EMAIL',
        'RESET_PASSWORD_DEMANDE','RESET_PASSWORD_SUCCES','RESET_PASSWORD_ECHEC'
    ))
);

CREATE INDEX IF NOT EXISTS idx_audit_table ON audit_log(table_name, record_id);
CREATE INDEX IF NOT EXISTS idx_audit_date ON audit_log(date_action);

-- Journal des emails envoyés (historique + debug des envois SMTP)
CREATE TABLE IF NOT EXISTS email_log (
    email_id          SERIAL PRIMARY KEY,
    demande_id        INT REFERENCES demande(demande_id) ON DELETE SET NULL,
    destinataire      VARCHAR(255) NOT NULL,
    cc                VARCHAR(255),
    sujet             VARCHAR(500),
    statut            VARCHAR(20) NOT NULL,  -- ENVOYE, ECHEC, SIMULATION
    erreur            TEXT,
    nb_pieces_jointes INT DEFAULT 0,
    utilisateur_id    INT REFERENCES utilisateur(userid),
    date_envoi        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_email_statut CHECK (statut IN ('ENVOYE','ECHEC','SIMULATION'))
);

CREATE INDEX IF NOT EXISTS idx_email_log_date       ON email_log(date_envoi DESC);
CREATE INDEX IF NOT EXISTS idx_email_log_statut     ON email_log(statut);
CREATE INDEX IF NOT EXISTS idx_email_log_demande    ON email_log(demande_id);
CREATE INDEX IF NOT EXISTS idx_email_log_destinataire ON email_log(destinataire);

-- Demandes de réinitialisation de mot de passe (OTP 6 chiffres par email)
CREATE TABLE IF NOT EXISTS password_reset (
    reset_id      SERIAL PRIMARY KEY,
    user_id       INT NOT NULL REFERENCES utilisateur(userid) ON DELETE CASCADE,
    code_hash     VARCHAR(255) NOT NULL,  -- BCrypt du code (jamais en clair)
    expires_at    TIMESTAMP NOT NULL,      -- now + 15 minutes
    used          BOOLEAN DEFAULT FALSE,
    tentatives    INT DEFAULT 0,           -- compteur d'essais (max 3)
    date_demande  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address    VARCHAR(45)
);

CREATE INDEX IF NOT EXISTS idx_pwdreset_user    ON password_reset(user_id, used, expires_at);
CREATE INDEX IF NOT EXISTS idx_pwdreset_expires ON password_reset(expires_at);

-- Paramètres applicatifs (configuration email, bons, etc.)
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

-- Données par défaut
INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('email', 'Configuration SMTP'),
    ('bon_validite_defaut', '365'),
    ('bon_type_defaut', 'Standard'),
    ('bon_entreprise', 'Intermart Maurice'),
    ('bon_format_qr', 'QR_CODE'),
    ('bon_signature', 'false')
ON CONFLICT (setting_key) DO NOTHING;

-- ────────────────────────────────────────────────────────────────────────────
-- 2. TRIGGER : Mise à jour automatique de date_modification sur demande
-- ────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION trg_demande_date_modification()
RETURNS TRIGGER AS $$
BEGIN
    NEW.date_modification := CURRENT_TIMESTAMP;

    -- Audit automatique des changements de statut
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

DROP TRIGGER IF EXISTS trg_demande_update ON demande;
CREATE TRIGGER trg_demande_update
    BEFORE UPDATE ON demande
    FOR EACH ROW
    EXECUTE FUNCTION trg_demande_date_modification();

-- ────────────────────────────────────────────────────────────────────────────
-- 3. PROCÉDURE STOCKÉE : Génération automatique des bons pour une demande
-- ────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION sp_generer_bons(
    p_demande_id INT,
    p_genere_par INT
)
RETURNS INT AS $$
DECLARE
    v_nombre     INT;
    v_valeur     NUMERIC(12,2);
    v_validite   INT;
    v_date_exp   TIMESTAMP;
    v_code       VARCHAR(64);
    v_count      INT := 0;
    v_ref        VARCHAR(50);
    i            INT;
BEGIN
    -- Vérifier que la demande est approuvée
    SELECT nombre_bons, valeur_unitaire, validite_jours, reference
    INTO v_nombre, v_valeur, v_validite, v_ref
    FROM demande
    WHERE demande_id = p_demande_id AND statuts = 'APPROUVE';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Demande % non trouvée ou non approuvée', p_demande_id;
    END IF;

    -- Calculer la date d'expiration
    v_date_exp := CURRENT_TIMESTAMP + (v_validite || ' days')::INTERVAL;

    -- Générer chaque bon avec un code unique
    FOR i IN 1..v_nombre LOOP
        v_code := v_ref || '-' || LPAD(i::TEXT, 4, '0') || '-' ||
                  UPPER(SUBSTRING(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT) FROM 1 FOR 8));

        INSERT INTO bon (demande_id, code_unique, qr_data, valeur, date_expiration)
        VALUES (p_demande_id, v_code, v_code, v_valeur, v_date_exp);

        v_count := v_count + 1;
    END LOOP;

    -- Mettre à jour le statut de la demande
    UPDATE demande
    SET statuts = 'GENERE',
        date_generation = CURRENT_TIMESTAMP,
        genere_par = p_genere_par
    WHERE demande_id = p_demande_id;

    -- Log audit
    INSERT INTO audit_log (table_name, record_id, action, nouveau_val, utilisateur_id, contexte)
    VALUES ('demande', p_demande_id, 'GENERATION',
            jsonb_build_object('bons_generes', v_count, 'valeur_unitaire', v_valeur),
            p_genere_par,
            v_count || ' bons générés pour la demande ' || v_ref);

    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- ────────────────────────────────────────────────────────────────────────────
-- 4. PROCÉDURE : Rédemption sécurisée d'un bon
-- ────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION sp_redimer_bon(
    p_code_unique   VARCHAR(64),
    p_magasin_id    INT,
    p_utilisateur_id INT
)
RETURNS TABLE(succes BOOLEAN, message TEXT, bon_valeur NUMERIC) AS $$
DECLARE
    v_bon_id     INT;
    v_statut     VARCHAR(20);
    v_expiration TIMESTAMP;
    v_valeur     NUMERIC(12,2);
    v_demande_id INT;
BEGIN
    -- Chercher le bon
    SELECT b.bon_id, b.statut, b.date_expiration, b.valeur, b.demande_id
    INTO v_bon_id, v_statut, v_expiration, v_valeur, v_demande_id
    FROM bon b
    WHERE b.code_unique = p_code_unique
    FOR UPDATE; -- Verrouillage pour anti double-spend

    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, 'Code invalide : bon introuvable'::TEXT, 0::NUMERIC;
        RETURN;
    END IF;

    -- Vérifier si déjà rédimé
    IF v_statut = 'REDIME' THEN
        RETURN QUERY SELECT FALSE, 'Ce bon a déjà été utilisé'::TEXT, 0::NUMERIC;
        RETURN;
    END IF;

    -- Vérifier si expiré
    IF v_expiration < CURRENT_TIMESTAMP THEN
        UPDATE bon SET statut = 'EXPIRE' WHERE bon_id = v_bon_id;
        RETURN QUERY SELECT FALSE, 'Ce bon est expiré depuis le ' || TO_CHAR(v_expiration, 'DD/MM/YYYY')::TEXT, 0::NUMERIC;
        RETURN;
    END IF;

    -- Vérifier si annulé
    IF v_statut != 'ACTIF' THEN
        RETURN QUERY SELECT FALSE, ('Ce bon est ' || v_statut)::TEXT, 0::NUMERIC;
        RETURN;
    END IF;

    -- Effectuer la rédemption
    UPDATE bon SET statut = 'REDIME' WHERE bon_id = v_bon_id;

    INSERT INTO redemption (bon_id, magasin_id, utilisateur_id)
    VALUES (v_bon_id, p_magasin_id, p_utilisateur_id);

    -- Audit
    INSERT INTO audit_log (table_name, record_id, action, nouveau_val, utilisateur_id, contexte)
    VALUES ('bon', v_bon_id, 'REDEMPTION',
            jsonb_build_object('magasin_id', p_magasin_id, 'valeur', v_valeur, 'code', p_code_unique),
            p_utilisateur_id,
            'Rédemption du bon ' || p_code_unique || ' en magasin');

    RETURN QUERY SELECT TRUE, ('Bon validé — Valeur : Rs ' || TO_CHAR(v_valeur, 'FM999,999.00'))::TEXT, v_valeur;
END;
$$ LANGUAGE plpgsql;

-- ────────────────────────────────────────────────────────────────────────────
-- 5. VUES pour REPORTING (Excel/ODBC)
-- ────────────────────────────────────────────────────────────────────────────

-- Vue : Toutes les demandes avec détails
CREATE OR REPLACE VIEW v_demandes_completes AS
SELECT
    d.demande_id,
    d.reference,
    d.invoice_reference,
    c.name AS client_nom,
    c.email AS client_email,
    c.company AS client_societe,
    d.nombre_bons,
    d.valeur_unitaire,
    d.montant_total,
    d.type_bon,
    d.validite_jours,
    d.statuts,
    m.nom_magasin,
    s.nom AS societe_nom,
    d.date_creation,
    d.date_demande,
    d.date_paiement,
    d.date_approbation,
    d.date_generation,
    u_cree.username AS cree_par_nom,
    u_valid.username AS valide_par_nom,
    u_appro.username AS approuve_par_nom
FROM demande d
LEFT JOIN client c ON d.clientid = c.clientid
LEFT JOIN magasin m ON d.magasin_id = m.magasin_id
LEFT JOIN societe s ON m.societe_id = s.societe_id
LEFT JOIN utilisateur u_cree ON d.cree_par = u_cree.userid
LEFT JOIN utilisateur u_valid ON d.valide_par = u_valid.userid
LEFT JOIN utilisateur u_appro ON d.approuve_par = u_appro.userid;

-- Vue : Demandes en attente de paiement
CREATE OR REPLACE VIEW v_demandes_attente_paiement AS
SELECT * FROM v_demandes_completes
WHERE statuts = 'EN_ATTENTE_PAIEMENT';

-- Vue : Liste des bons avec statut détaillé
CREATE OR REPLACE VIEW v_bons_details AS
SELECT
    b.bon_id,
    b.code_unique,
    b.valeur,
    b.statut,
    b.date_emission,
    b.date_expiration,
    CASE
        WHEN b.statut = 'REDIME' THEN 'Utilisé'
        WHEN b.statut = 'ANNULE' THEN 'Annulé'
        WHEN b.date_expiration < CURRENT_TIMESTAMP THEN 'Expiré'
        WHEN b.date_expiration < CURRENT_TIMESTAMP + INTERVAL '30 days' THEN 'Proche expiration'
        ELSE 'Actif'
    END AS statut_detaille,
    d.reference AS demande_reference,
    d.invoice_reference,
    c.name AS client_nom,
    c.email AS client_email,
    m.nom_magasin AS magasin_emission,
    r.date_redemption,
    mr.nom_magasin AS magasin_redemption,
    ur.username AS redime_par
FROM bon b
JOIN demande d ON b.demande_id = d.demande_id
LEFT JOIN client c ON d.clientid = c.clientid
LEFT JOIN magasin m ON d.magasin_id = m.magasin_id
LEFT JOIN redemption r ON b.bon_id = r.bon_id
LEFT JOIN magasin mr ON r.magasin_id = mr.magasin_id
LEFT JOIN utilisateur ur ON r.utilisateur_id = ur.userid;

-- Vue : Bons proches d'expiration (< 30 jours)
CREATE OR REPLACE VIEW v_bons_proche_expiration AS
SELECT * FROM v_bons_details
WHERE statut = 'ACTIF'
  AND date_expiration BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '30 days';

-- Vue : Statistiques par société
CREATE OR REPLACE VIEW v_stats_societe AS
SELECT
    s.nom AS societe,
    COUNT(DISTINCT d.demande_id) AS total_demandes,
    COALESCE(SUM(d.montant_total), 0) AS montant_total,
    COUNT(DISTINCT b.bon_id) FILTER (WHERE b.statut = 'ACTIF') AS bons_actifs,
    COUNT(DISTINCT b.bon_id) FILTER (WHERE b.statut = 'REDIME') AS bons_redimes,
    COUNT(DISTINCT b.bon_id) FILTER (WHERE b.statut = 'EXPIRE' OR b.date_expiration < CURRENT_TIMESTAMP) AS bons_expires
FROM societe s
LEFT JOIN magasin m ON s.societe_id = m.societe_id
LEFT JOIN demande d ON m.magasin_id = d.magasin_id
LEFT JOIN bon b ON d.demande_id = b.demande_id
GROUP BY s.societe_id, s.nom;

-- ────────────────────────────────────────────────────────────────────────────
-- 6. DONNÉES DE TEST
-- ────────────────────────────────────────────────────────────────────────────

-- Société
INSERT INTO societe (nom, adresse, email) VALUES
    ('Intermart Maurice', 'Cybercité, Ebène', 'contact@intermart.mu')
ON CONFLICT DO NOTHING;

-- Magasins
INSERT INTO magasin (societe_id, nom_magasin, adresse) VALUES
    (1, 'Intermart Ebène',       'Cybercité, Ebène'),
    (1, 'Intermart Port-Louis',  'Caudan Waterfront, Port-Louis'),
    (1, 'Intermart Curepipe',    'Curepipe Road, Curepipe')
ON CONFLICT DO NOTHING;

-- Utilisateur admin par defaut (mot de passe : admin)
INSERT INTO utilisateur (username, email, password, role) VALUES
    ('admin', 'admin@intermart.mu', '$2a$10$IHLDj8FrYmsg3payhyc6x.mVUo7JdHH.kP5YaFg2K9MvqgOzG.0/6', 'Administrateur')
ON CONFLICT (username) DO NOTHING;
