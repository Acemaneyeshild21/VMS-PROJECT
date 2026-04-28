-- ============================================================================
-- VMS — MIGRATION PHASE 5 : Procédures Stockées & Triggers Métier
-- MCCI Business School — BTS SIO SLAM RP2 — Session 2026
-- Appliquée EN PLUS du schema.sql existant (idempotente)
-- ============================================================================

-- ════════════════════════════════════════════════════════════════════════════
-- A. CORRECTION chk_action — actions Phase 3 manquantes
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS chk_action;
ALTER TABLE audit_log ADD CONSTRAINT chk_action CHECK (action IN (
    -- Cycle de vie demandes
    'CREATION','MODIFICATION','SUPPRESSION',
    'PAIEMENT','APPROBATION','GENERATION',
    'ENVOI','ENVOI_ECHEC','RENVOI_EMAIL',
    'REDEMPTION','REDEMPTION_ECHOUEE','UTILISATION_BON',
    'REJET','ANNULATION','ARCHIVAGE','ARCHIVAGE_MASSIF','CHANGEMENT_STATUT',
    -- Authentification
    'CONNEXION','CONNEXION_ECHOUEE','CONNEXION_BLOQUEE','DECONNEXION','INSCRIPTION',
    'COMPTE_VERROUILLE','DEVERROUILLAGE',
    -- Gestion utilisateurs
    'CHANGE_PASSWORD','UPDATE_PROFILE','UPDATE_ROLE','DELETE','UPDATE_EMAIL',
    -- Réinitialisation mot de passe (flux OTP Phase 3)
    'RESET_PASSWORD','RESET_PASSWORD_DEMANDE','RESET_PASSWORD_OK',
    'RESET_PASSWORD_SUCCES','RESET_PASSWORD_ECHEC'
));

-- ════════════════════════════════════════════════════════════════════════════
-- B. PROCÉDURE STOCKÉE 3 : sp_archiver_demandes_expirees
--    Archive toutes les demandes dont l'ensemble des bons sont expirés
--    ou dont la validité est dépassée.
--    Retourne le nombre de demandes archivées.
-- ════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION sp_archiver_demandes_expirees(
    p_user_id   INT  DEFAULT NULL
)
RETURNS INT AS $$
DECLARE
    v_count     INT := 0;
    v_demande   RECORD;
BEGIN
    -- Parcourir toutes les demandes GENERE ou ENVOYE dont tous les bons sont expirés
    FOR v_demande IN
        SELECT d.demande_id, d.reference
        FROM demande d
        WHERE d.statuts IN ('GENERE', 'ENVOYE')
          AND NOT EXISTS (
              SELECT 1 FROM bon b
              WHERE b.demande_id = d.demande_id
                AND b.statut = 'ACTIF'
                AND b.date_expiration > CURRENT_TIMESTAMP
          )
          AND EXISTS (
              SELECT 1 FROM bon b
              WHERE b.demande_id = d.demande_id
          )
    LOOP
        -- Archiver la demande
        UPDATE demande
        SET statuts          = 'ARCHIVE',
            date_modification = CURRENT_TIMESTAMP
        WHERE demande_id = v_demande.demande_id;

        -- Audit
        INSERT INTO audit_log (
            table_name, record_id, action,
            ancien_val, nouveau_val,
            utilisateur_id, contexte
        ) VALUES (
            'demande', v_demande.demande_id, 'ARCHIVAGE_MASSIF',
            jsonb_build_object('statuts', 'GENERE/ENVOYE'),
            jsonb_build_object('statuts', 'ARCHIVE'),
            p_user_id,
            'Archivage automatique — tous les bons de ' || v_demande.reference || ' sont expirés'
        );

        v_count := v_count + 1;
    END LOOP;

    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION sp_archiver_demandes_expirees(INT) IS
'Archive les demandes dont tous les bons sont expirés. Appelée manuellement ou en planifié.
Retourne le nombre de demandes archivées. p_user_id optionnel pour l''audit.';

-- ════════════════════════════════════════════════════════════════════════════
-- C. PROCÉDURE STOCKÉE 4 : sp_statistiques_globales
--    Retourne tous les KPIs du tableau de bord en un seul appel.
--    Évite les N+1 requêtes depuis Java. Prend en compte le rôle
--    de l'utilisateur (Manager/Collaborateur voient uniquement leur magasin).
-- ════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION sp_statistiques_globales(
    p_role    VARCHAR(60) DEFAULT 'Administrateur',
    p_user_id INT         DEFAULT NULL
)
RETURNS TABLE (
    total_demandes      BIGINT,
    montant_total       NUMERIC,
    bons_actifs         BIGINT,
    bons_redimes        BIGINT,
    bons_expires        BIGINT,
    taux_redemption     NUMERIC,
    demandes_attente    BIGINT,
    demandes_approuvees BIGINT,
    bons_expirant_30j   BIGINT,
    redemptions_today   BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(DISTINCT d.demande_id)::BIGINT                                         AS total_demandes,
        COALESCE(SUM(d.montant_total), 0)::NUMERIC                                   AS montant_total,
        COUNT(b.bon_id) FILTER (WHERE b.statut = 'ACTIF')::BIGINT                   AS bons_actifs,
        COUNT(b.bon_id) FILTER (WHERE b.statut = 'REDIME')::BIGINT                  AS bons_redimes,
        COUNT(b.bon_id) FILTER (WHERE b.statut = 'EXPIRE'
                                   OR (b.statut = 'ACTIF'
                                       AND b.date_expiration < CURRENT_TIMESTAMP))::BIGINT AS bons_expires,
        CASE
            WHEN COUNT(b.bon_id) > 0
            THEN ROUND(
                    COUNT(b.bon_id) FILTER (WHERE b.statut = 'REDIME')::NUMERIC
                    / COUNT(b.bon_id)::NUMERIC * 100, 1
                 )
            ELSE 0
        END::NUMERIC                                                                  AS taux_redemption,
        COUNT(DISTINCT d.demande_id)
            FILTER (WHERE d.statuts = 'EN_ATTENTE_PAIEMENT')::BIGINT                AS demandes_attente,
        COUNT(DISTINCT d.demande_id)
            FILTER (WHERE d.statuts = 'APPROUVE')::BIGINT                           AS demandes_approuvees,
        COUNT(b.bon_id)
            FILTER (WHERE b.statut = 'ACTIF'
                      AND b.date_expiration BETWEEN CURRENT_TIMESTAMP
                                                AND CURRENT_TIMESTAMP + INTERVAL '30 days')::BIGINT
                                                                                     AS bons_expirant_30j,
        COUNT(r.redemption_id)
            FILTER (WHERE r.date_redemption::date = CURRENT_DATE)::BIGINT           AS redemptions_today
    FROM demande d
    LEFT JOIN bon b      ON b.demande_id = d.demande_id
    LEFT JOIN redemption r ON r.bon_id   = b.bon_id
    WHERE
        -- Administrateur / Approbateur : tous les magasins
        -- Manager / Collaborateur : uniquement leur magasin si magasin_id connu
        (
            p_role IN ('Administrateur', 'Approbateur', 'Comptable')
            OR p_user_id IS NULL
            OR d.magasin_id IN (
                SELECT m.magasin_id FROM magasin m
                WHERE m.superviseur_id = p_user_id
                  OR p_role NOT IN ('Superviseur_Magasin')
            )
        );
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION sp_statistiques_globales(VARCHAR, INT) IS
'Retourne les KPIs du tableau de bord en un seul appel DB.
Respecte le filtrage par rôle : Administrateur voit tout, Superviseur_Magasin
voit uniquement les données de son magasin.';

-- ════════════════════════════════════════════════════════════════════════════
-- D. TRIGGER 8 : trg_client_actif_check
--    AVANT INSERT sur demande : vérifie que le client associé
--    est bien actif. Bloque la création de demandes pour des clients
--    désactivés — protection DB-level indépendante du code Java.
-- ════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION trg_fn_client_actif_check()
RETURNS TRIGGER AS $$
DECLARE
    v_actif  BOOLEAN;
    v_nom    VARCHAR(150);
BEGIN
    SELECT actif, name
    INTO v_actif, v_nom
    FROM client
    WHERE clientid = NEW.clientid;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'CLIENT_INTROUVABLE: aucun client avec clientid=%', NEW.clientid
            USING ERRCODE = 'P0010';
    END IF;

    IF NOT v_actif THEN
        RAISE EXCEPTION 'CLIENT_INACTIF: le client "%" (id=%) est désactivé — demande refusée.',
            v_nom, NEW.clientid
            USING ERRCODE = 'P0011';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_client_actif_check ON demande;
CREATE TRIGGER trg_client_actif_check
    BEFORE INSERT ON demande
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_client_actif_check();

COMMENT ON FUNCTION trg_fn_client_actif_check() IS
'Trigger BEFORE INSERT sur demande. Rejette toute demande liée à un client inactif.
Sécurité DB-level complémentaire au contrôle Java.';

-- ════════════════════════════════════════════════════════════════════════════
-- E. TRIGGER 9 : trg_bon_expire_auto
--    BEFORE UPDATE sur bon (colonne statut).
--    Si le bon est ACTIF mais que sa date d'expiration est passée,
--    force automatiquement le statut à EXPIRE — cohérence garantie même
--    lors d'un UPDATE direct en pgAdmin.
-- ════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION trg_fn_bon_expire_auto()
RETURNS TRIGGER AS $$
BEGIN
    -- Si on essaie de mettre à jour un bon encore marqué ACTIF mais expiré
    IF NEW.statut = 'ACTIF' AND NEW.date_expiration < CURRENT_TIMESTAMP THEN
        NEW.statut := 'EXPIRE';
        -- Pas d'exception : juste normalisation silencieuse
    END IF;

    -- Empêcher de "réactiver" un bon déjà REDIME ou ANNULE
    IF OLD.statut IN ('REDIME', 'ANNULE') AND NEW.statut NOT IN ('REDIME', 'ANNULE') THEN
        RAISE EXCEPTION
            'BON_IMMUABLE: le bon % (id=%) est déjà % et ne peut plus être modifié.',
            OLD.code_unique, OLD.bon_id, OLD.statut
            USING ERRCODE = 'P0020';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bon_expire_auto ON bon;
CREATE TRIGGER trg_bon_expire_auto
    BEFORE UPDATE OF statut ON bon
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_bon_expire_auto();

COMMENT ON FUNCTION trg_fn_bon_expire_auto() IS
'Trigger BEFORE UPDATE(statut) sur bon.
1) Normalise ACTIF→EXPIRE si date_expiration dépassée.
2) Bloque toute tentative de "dé-rédimer" un bon déjà REDIME ou ANNULE.';

-- ════════════════════════════════════════════════════════════════════════════
-- F. VUE : v_activite_magasin_jour
--    Utilisée par l'application magasin (POS) pour afficher
--    le résumé d'activité du jour par point de vente.
-- ════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW v_activite_magasin_jour AS
SELECT
    m.magasin_id,
    m.nom_magasin,
    COUNT(r.redemption_id)           AS nb_redemptions_jour,
    COALESCE(SUM(b.valeur), 0)       AS montant_redeme_jour,
    MAX(r.date_redemption)           AS derniere_redemption,
    MIN(r.date_redemption)           AS premiere_redemption
FROM magasin m
LEFT JOIN redemption r ON r.magasin_id = m.magasin_id
                      AND r.date_redemption::date = CURRENT_DATE
LEFT JOIN bon b        ON b.bon_id = r.bon_id
WHERE m.actif = TRUE
GROUP BY m.magasin_id, m.nom_magasin
ORDER BY nb_redemptions_jour DESC;

COMMENT ON VIEW v_activite_magasin_jour IS
'Synthèse des rédemptions du jour par magasin. Rafraîchie en temps réel.
Utilisée par le dashboard POS et le reporting siège.';

-- ════════════════════════════════════════════════════════════════════════════
-- G. VUE : v_rapport_odbc
--    Vue dénormalisée complète, optimisée pour la connexion ODBC Excel/Power BI.
--    Contient toutes les informations pertinentes en une seule table plate.
-- ════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW v_rapport_odbc AS
SELECT
    -- Demande
    d.demande_id,
    d.reference                             AS demande_reference,
    d.invoice_reference                     AS facture_reference,
    d.statuts                               AS demande_statut,
    d.nombre_bons,
    d.valeur_unitaire,
    d.montant_total,
    d.type_bon,
    d.validite_jours,
    d.date_creation                         AS demande_date_creation,
    d.date_paiement,
    d.date_approbation,
    d.date_generation,
    -- Client
    c.clientid,
    c.name                                  AS client_nom,
    c.email                                 AS client_email,
    c.contact_number                        AS client_telephone,
    c.company                               AS client_societe,
    -- Bon
    b.bon_id,
    b.code_unique,
    b.valeur                                AS bon_valeur,
    b.statut                                AS bon_statut,
    b.date_emission,
    b.date_expiration,
    CASE
        WHEN b.statut = 'REDIME'                          THEN 'Utilisé'
        WHEN b.statut = 'ANNULE'                          THEN 'Annulé'
        WHEN b.date_expiration < CURRENT_TIMESTAMP        THEN 'Expiré'
        WHEN b.date_expiration < CURRENT_TIMESTAMP + INTERVAL '30 days'
                                                          THEN 'Proche expiration'
        ELSE 'Actif'
    END                                     AS bon_statut_lisible,
    GREATEST(0, EXTRACT(DAY FROM (b.date_expiration - CURRENT_TIMESTAMP)))::INT
                                            AS jours_restants,
    -- Rédemption
    r.redemption_id,
    r.date_redemption,
    mr.nom_magasin                          AS magasin_redemption,
    ur.username                             AS caissier_redemption,
    -- Magasin émetteur
    m.nom_magasin                           AS magasin_emission,
    s.nom                                   AS societe,
    -- Utilisateurs workflow
    u_cree.username                         AS cree_par,
    u_appro.username                        AS approuve_par
FROM demande d
LEFT JOIN client          c     ON c.clientid         = d.clientid
LEFT JOIN magasin         m     ON m.magasin_id        = d.magasin_id
LEFT JOIN societe         s     ON s.societe_id        = m.societe_id
LEFT JOIN bon             b     ON b.demande_id        = d.demande_id
LEFT JOIN redemption      r     ON r.bon_id            = b.bon_id
LEFT JOIN magasin         mr    ON mr.magasin_id       = r.magasin_id
LEFT JOIN utilisateur     ur    ON ur.userid           = r.utilisateur_id
LEFT JOIN utilisateur     u_cree  ON u_cree.userid     = d.cree_par
LEFT JOIN utilisateur     u_appro ON u_appro.userid    = d.approuve_par;

COMMENT ON VIEW v_rapport_odbc IS
'Vue plate dénormalisée pour connexion ODBC (Excel / Power BI / LibreOffice Calc).
Contient toutes les colonnes utiles. Chaque ligne = 1 bon avec sa demande parente.
Connexion : DSN PostgreSQL → Tableau croisé dynamique sur montant_total / statut.';

-- ════════════════════════════════════════════════════════════════════════════
-- H. VUE : v_tableau_bord_temps_reel
--    Vue unique pour le tableau de bord siège — KPIs principaux.
--    Évite les 5 requêtes séparées dans StatistiquesDAO.
-- ════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW v_tableau_bord_temps_reel AS
SELECT
    -- Demandes
    COUNT(DISTINCT d.demande_id)                                                AS total_demandes,
    COUNT(DISTINCT d.demande_id) FILTER (WHERE d.statuts = 'EN_ATTENTE_PAIEMENT')
                                                                                AS demandes_attente_paiement,
    COUNT(DISTINCT d.demande_id) FILTER (WHERE d.statuts = 'APPROUVE')         AS demandes_approuvees,
    COUNT(DISTINCT d.demande_id) FILTER (WHERE d.statuts = 'ARCHIVE')          AS demandes_archivees,
    COALESCE(SUM(d.montant_total), 0)                                           AS montant_total,
    -- Bons
    COUNT(b.bon_id)                                                             AS total_bons,
    COUNT(b.bon_id) FILTER (WHERE b.statut = 'ACTIF')                          AS bons_actifs,
    COUNT(b.bon_id) FILTER (WHERE b.statut = 'REDIME')                         AS bons_redimes,
    COUNT(b.bon_id) FILTER (WHERE b.statut IN ('EXPIRE')
                               OR (b.statut = 'ACTIF'
                                   AND b.date_expiration < CURRENT_TIMESTAMP)) AS bons_expires,
    COUNT(b.bon_id) FILTER (WHERE b.statut = 'ACTIF'
                              AND b.date_expiration BETWEEN CURRENT_TIMESTAMP
                                                        AND CURRENT_TIMESTAMP + INTERVAL '30 days')
                                                                                AS bons_expirant_30j,
    -- Taux de rédemption global
    CASE
        WHEN COUNT(b.bon_id) > 0
        THEN ROUND(
            COUNT(b.bon_id) FILTER (WHERE b.statut = 'REDIME')::NUMERIC
            / COUNT(b.bon_id)::NUMERIC * 100, 1)
        ELSE 0
    END                                                                         AS taux_redemption_pct,
    -- Activité du jour
    COUNT(r.redemption_id) FILTER (WHERE r.date_redemption::date = CURRENT_DATE)
                                                                                AS redemptions_aujourd_hui,
    COALESCE(SUM(b2.valeur) FILTER (WHERE r2.date_redemption::date = CURRENT_DATE), 0)
                                                                                AS montant_redeme_aujourd_hui
FROM demande d
LEFT JOIN bon         b   ON b.demande_id = d.demande_id
LEFT JOIN redemption  r   ON r.bon_id     = b.bon_id
LEFT JOIN bon         b2  ON b2.bon_id    = r.bon_id
LEFT JOIN redemption  r2  ON r2.bon_id    = b2.bon_id;

COMMENT ON VIEW v_tableau_bord_temps_reel IS
'Vue temps réel pour le dashboard siège — tous les KPIs en un seul SELECT.
Utilisée par StatistiquesDAO / StatistiquesController pour réduire les allers-retours DB.';

-- ════════════════════════════════════════════════════════════════════════════
-- I. INDEX SUPPLÉMENTAIRES (performance Phase 5)
-- ════════════════════════════════════════════════════════════════════════════

-- Accélérer les recherches de bons par statut (dashboard, reporting)
CREATE INDEX IF NOT EXISTS idx_bon_statut_expiration
    ON bon(statut, date_expiration);

-- Accélérer les recherches de demandes par client + statut
CREATE INDEX IF NOT EXISTS idx_demande_client_statut
    ON demande(clientid, statuts);

-- Accélérer les rédemptions par magasin et par date (historique POS)
CREATE INDEX IF NOT EXISTS idx_redemption_magasin_date
    ON redemption(magasin_id, date_redemption DESC);

-- Accélérer l'audit par action (logs filtrés par type d'action)
CREATE INDEX IF NOT EXISTS idx_audit_action_date
    ON audit_log(action, date_action DESC);

-- ════════════════════════════════════════════════════════════════════════════
-- FIN MIGRATION PHASE 5
-- ════════════════════════════════════════════════════════════════════════════

-- Récapitulatif des objets BD :
-- ─────────────────────────────────────────────────────────────────────────
-- PROCÉDURES STOCKÉES (4 au total) :
--   1. sp_generer_bons(demande_id, user_id)              → INT
--   2. sp_redimer_bon(code, magasin_id, user_id)         → TABLE(succes, message, valeur)
--   3. sp_archiver_demandes_expirees(user_id)            → INT   [PHASE 5]
--   4. sp_statistiques_globales(role, user_id)           → TABLE(10 KPIs) [PHASE 5]
--
-- TRIGGERS (9 au total) :
--   1. trg_demande_update          BEFORE UPDATE demande  → date_modification + audit statut
--   2. trg_separation_taches       BEFORE UPDATE demande  → SoD (paiement ≠ approbateur)
--   3. trg_redemption_auto_redime  AFTER INSERT redemption→ mark bon REDIME
--   4. trg_anti_double_spend       BEFORE INSERT redemption→ bloque double rédemption
--   5. trg_demande_montant         BEFORE INSERT/UPDATE   → valide nb_bons > 0, valeur > 0
--   6. trg_utilisateur_normalize_email BEFORE INSERT/UPDATE → email en minuscules
--   7. trg_client_normalize_email  BEFORE INSERT/UPDATE   → email en minuscules
--   8. trg_client_actif_check      BEFORE INSERT demande  → client doit être actif [PHASE 5]
--   9. trg_bon_expire_auto         BEFORE UPDATE bon.statut → auto-expire + bloque dé-rédimer [PHASE 5]
--
-- VUES (8 au total) :
--   1. v_demandes_completes         — toutes les demandes avec détails
--   2. v_demandes_attente_paiement  — filtre EN_ATTENTE_PAIEMENT
--   3. v_bons_details               — bons avec statut lisible + redemption info
--   4. v_bons_proche_expiration     — bons expirant dans < seuil jours (configurable)
--   5. v_stats_societe              — KPIs par société
--   6. v_activite_magasin_jour      — activité du jour par magasin (POS) [PHASE 5]
--   7. v_rapport_odbc               — vue plate ODBC pour Excel/Power BI [PHASE 5]
--   8. v_tableau_bord_temps_reel    — tous les KPIs dashboard en 1 SELECT [PHASE 5]
-- ─────────────────────────────────────────────────────────────────────────
