package org.rhq.core.domain.content.transfer;


public class EntitlementCertificate {

    private String certificate;
    private String key;

    /**
     *
     */
    public EntitlementCertificate() {

    }

    /**
     *
     * @param certificate
     * @param key
     */
    public EntitlementCertificate(String certificate, String key) {
        this.certificate = certificate;
        this.key = key;
    }

    /**
     * @return the certificate
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * @param certificate the certificate to set
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * @return the private key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the private key to set
     */
    public void setKey(String key) {
        this.key = key;
    }
}