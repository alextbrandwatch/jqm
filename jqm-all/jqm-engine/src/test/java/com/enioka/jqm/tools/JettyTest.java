package com.enioka.jqm.tools;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.persistence.EntityManager;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import com.enioka.jqm.api.JobRequest;
import com.enioka.jqm.api.JqmClientFactory;
import com.enioka.jqm.pki.JpaCa;
import com.enioka.jqm.test.helpers.CreationTools;
import com.enioka.jqm.test.helpers.TestHelpers;

public class JettyTest extends JqmBaseTest
{
    @Test
    public void testSslStartup()
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testSslStartup");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        Helpers.setSingleParam("useSsl", "true", em);
        Helpers.setSingleParam("noHttp", "false", em);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");

        engine1.stop();
    }

    @Test
    public void testSslServices() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testSslServices");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        Helpers.setSingleParam("useSsl", "true", em);
        Helpers.setSingleParam("noHttp", "false", em);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");

        // Launch a job so as to be able to query its status later
        CreationTools.createJobDef(null, true, "App", null, "jqm-tests/jqm-test-datetimemaven/target/test.jar", TestHelpers.qVip, 42,
                "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", true, em);
        JobRequest j = new JobRequest("MarsuApplication", "MAG");
        int i = JqmClientFactory.getClient().enqueue(j);
        TestHelpers.waitFor(1, 10000, em);

        // HTTPS client - with
        KeyStore trustStore = KeyStore.getInstance("JKS");
        FileInputStream instream = new FileInputStream(new File("./conf/trusted.jks"));
        try
        {
            trustStore.load(instream, "SuperPassword".toCharArray());
        }
        finally
        {
            instream.close();
        }

        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore).build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

        CloseableHttpClient cl = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        int port = em.createQuery("SELECT q.port FROM Node q WHERE q.id = :i", Integer.class).setParameter("i", TestHelpers.node.getId())
                .getSingleResult();
        HttpUriRequest rq = new HttpGet("https://" + TestHelpers.node.getDns() + ":" + port + "/status?id=" + i);
        jqmlogger.debug(rq.getURI());
        CloseableHttpResponse rs = cl.execute(rq);

        rs.close();
        cl.close();
        engine1.stop();
    }

    @Test
    public void testSslClientCert() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testSslClientCert");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        Helpers.setSingleParam("useSsl", "true", em);
        Helpers.setSingleParam("noHttp", "false", em);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");

        // Launch a job so as to be able to query its status later
        CreationTools.createJobDef(null, true, "App", null, "jqm-tests/jqm-test-datetimemaven/target/test.jar", TestHelpers.qVip, 42,
                "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", true, em);
        JobRequest j = new JobRequest("MarsuApplication", "MAG");
        int i = JqmClientFactory.getClient().enqueue(j);
        TestHelpers.waitFor(1, 10000, em);

        // Server auth against trusted CA root certificate
        KeyStore trustStore = KeyStore.getInstance("JKS");
        FileInputStream instream = new FileInputStream(new File("./conf/trusted.jks"));
        try
        {
            trustStore.load(instream, "SuperPassword".toCharArray());
        }
        finally
        {
            instream.close();
        }

        // Client auth
        JpaCa.prepareClientStore(em, "CN=testuser", "./conf/client.pfx", "SuperPassword", "client-cert", "./conf/client.cer");
        KeyStore clientStore = KeyStore.getInstance("PKCS12");
        instream = new FileInputStream(new File("./conf/client.pfx"));
        try
        {
            clientStore.load(instream, "SuperPassword".toCharArray());
        }
        finally
        {
            instream.close();
        }

        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore)
                .loadKeyMaterial(clientStore, "SuperPassword".toCharArray()).build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

        CloseableHttpClient cl = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        int port = em.createQuery("SELECT q.port FROM Node q WHERE q.id = :i", Integer.class).setParameter("i", TestHelpers.node.getId())
                .getSingleResult();
        HttpUriRequest rq = new HttpGet("https://" + TestHelpers.node.getDns() + ":" + port + "/status?id=" + i);
        CloseableHttpResponse rs = cl.execute(rq);

        rs.close();
        cl.close();
        engine1.stop();
    }
}
