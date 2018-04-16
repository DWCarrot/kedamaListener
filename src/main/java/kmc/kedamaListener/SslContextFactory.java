package kmc.kedamaListener;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.netty.util.internal.SystemPropertyUtil;
import kmc.kedamaListener.js.settings.SSLSettings;

public class SslContextFactory {

	private static SSLContext clientContext;
	private static String PROTOCOL = "TLS";
	
	public static SSLContext getClientContext(SSLSettings settings) {
		if(clientContext == null && settings != null) {
			String algorithm = SystemPropertyUtil.get("ssl.KeyManagerFactory.algorithm");
	        if (algorithm == null)
	            algorithm = "SunX509";
	        try {
	            KeyStore ks2 = KeyStore.getInstance("JKS");
	            ks2.load(App.class.getResourceAsStream("/" + settings.keystore), settings.keystorepw.toCharArray());

	            KeyStore tks2 = KeyStore.getInstance("JKS");
	            tks2.load(App.class.getResourceAsStream("/" + settings.trustkeystore), settings.trustkeystorepw.toCharArray());
	            // Set up key manager factory to use our key store
	            KeyManagerFactory kmf2 = KeyManagerFactory.getInstance(algorithm);
	            TrustManagerFactory tmf2 = TrustManagerFactory.getInstance("SunX509");
	            kmf2.init(ks2, settings.keystorepw.toCharArray());
	            tmf2.init(tks2);
	            clientContext = SSLContext.getInstance(PROTOCOL);
	            clientContext.init(kmf2.getKeyManagers(), tmf2.getTrustManagers(), null);	           
	        } catch (Exception e) {
	            App.logger.error("SSLException @" + Thread.currentThread().getName(), e);
	        }
	        App.logger.info("#process: SSLContext loaded");
		}
        return clientContext;
    }

}
