import pkg.vms.VoucherPDFGenerator;
import pkg.vms.DAO.BonDAO;

/**
 * Test rapide pour generer un bon cadeau PDF de demo.
 */
public class TestVoucherPDF {
    public static void main(String[] args) throws Exception {
        BonDAO.BonInfo bon = new BonDAO.BonInfo();
        bon.bonId = 1;
        bon.codeUnique = "VMS-2026-0001-A3F8B2C1";
        bon.valeur = 5000.00;
        bon.statut = "ACTIF";
        bon.dateEmission = "2026-04-14 10:00:00";
        bon.dateExpiration = "2027-04-14 10:00:00";
        bon.demandeId = 1;
        bon.clientNom = "Jean-Pierre Ramgoolam";
        bon.clientEmail = "jpramgoolam@test.mu";
        bon.reference = "DEM-2026-0001";

        String path = VoucherPDFGenerator.genererPDF(bon);
        System.out.println("Bon cadeau genere : " + path);

        // Ouvrir automatiquement le PDF
        java.awt.Desktop.getDesktop().open(new java.io.File(path));
    }
}
