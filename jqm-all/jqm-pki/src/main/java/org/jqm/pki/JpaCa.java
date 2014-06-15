package org.jqm.pki;

import java.io.File;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemReader;

import com.enioka.jqm.jpamodel.GlobalParameter;
import com.enioka.jqm.jpamodel.PKI;

/**
 * This class is the link between the X509 methods in CertificateRequest and the JPA database store.
 * 
 */
public class JpaCa
{
    public static CertificateRequest initCa(EntityManager em)
    {
        // result field
        CertificateRequest cr = new CertificateRequest();

        // Get the alias of the private key to use
        String caAlias = null;
        try
        {
            caAlias = em.createQuery("SELECT p FROM GlobalParameter p WHERE p.key = 'keyAlias'", GlobalParameter.class).getSingleResult()
                    .getValue();
        }
        catch (NoResultException e)
        {
            caAlias = Constants.CA_DEFAULT_PRETTY_NAME;
        }

        // Create the CA if it does not already exist
        PKI pki = null;
        try
        {
            pki = em.createQuery("SELECT p FROM PKI p WHERE p.prettyName = :pn", PKI.class).setParameter("pn", caAlias).getSingleResult();
        }
        catch (NoResultException e)
        {
            // Create the CA certificate and PK
            cr = new CertificateRequest();
            cr.generateCA(caAlias);

            // Store
            pki = new PKI();
            pki.setPemPK(cr.writePemPrivateToString());
            pki.setPemCert(cr.writePemPublicToString());
            pki.setPrettyName(caAlias);
            em.getTransaction().begin();
            em.persist(pki);
            em.getTransaction().commit();
        }

        try
        {
            // Public (X509 certificate)
            String pemCert = pki.getPemCert();
            StringReader sr = new StringReader(pemCert);
            PemReader pr = new PemReader(sr);
            cr.holder = new X509CertificateHolder(pr.readPemObject().getContent());
            pr.close();

            // Private key
            String pemPrivate = pki.getPemPK();
            sr = new StringReader(pemPrivate);
            PEMParser pp = new PEMParser(sr);
            PEMKeyPair caKeyPair = (PEMKeyPair) pp.readObject();
            pp.close();
            byte[] encodedPrivateKey = caKeyPair.getPrivateKeyInfo().getEncoded();
            KeyFactory keyFactory = KeyFactory.getInstance(Constants.KEY_ALGORITHM);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
            cr.privateKey = keyFactory.generatePrivate(privateKeySpec);
            cr.publicKey = keyFactory.generatePublic(privateKeySpec);
        }
        catch (Exception e)
        {
            throw new PkiException(e);
        }

        // Done
        return cr;
    }

    public static void prepareWebServerStores(EntityManager em, String subject, String pfxPath, String prettyName, String cerPath)
    {
        File pfx = new File(pfxPath);

        if (pfx.canRead())
        {
            return;
        }

        CertificateRequest ca = initCa(em);

        CertificateRequest srv = new CertificateRequest();
        srv.generateServerCert(prettyName, ca.holder, ca.privateKey, subject);
        srv.writePfxToFile(pfxPath, Constants.PFX_PASSWORD);
        srv.writePemPublicToFile(cerPath);
    }

}