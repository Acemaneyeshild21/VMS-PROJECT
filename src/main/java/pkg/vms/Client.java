package pkg.vms;

import java.sql.Timestamp;

/**
 * Classe modèle représentant un client dans le système VMS
 */
public class Client {
    private int clientId;
    private String name;
    private String email;
    private String contactNumber;
    private String company;
    private Timestamp dateCreation;
    private boolean actif;

    // Constructeur vide
    public Client() {
    }

    // Constructeur complet
    public Client(int clientId, String name, String email, String contactNumber, String company, Timestamp dateCreation, boolean actif) {
        this.clientId = clientId;
        this.name = name;
        this.email = email;
        this.contactNumber = contactNumber;
        this.company = company;
        this.dateCreation = dateCreation;
        this.actif = actif;
    }

    // Constructeur sans ID
    public Client(String name, String email, String contactNumber, String company) {
        this.name = name;
        this.email = email;
        this.contactNumber = contactNumber;
        this.company = company;
        this.actif = true;
    }

    // Getters et Setters
    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    @Override
    public String toString() {
        return "Client{" +
                "clientId=" + clientId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", company='" + company + '\'' +
                ", actif=" + actif +
                '}';
    }
}

