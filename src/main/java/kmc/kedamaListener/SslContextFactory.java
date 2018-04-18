package kmc.kedamaListener;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.SystemPropertyUtil;
import kmc.kedamaListener.js.settings.SSLSettings;

public class SslContextFactory {

	private static SSLContext clientContext;
	private static String PROTOCOL = "TLS";
	private static Logger logger = LoggerFactory.getLogger(SslContextFactory.class);
	
	public static SSLContext getClientContext(SSLSettings settings) {
		if(clientContext == null && settings != null) {
			String algorithm = SystemPropertyUtil.get("ssl.KeyManagerFactory.algorithm");
	        if (algorithm == null)
	            algorithm = "SunX509";
	        try {
	        	if(settings.keystoretype == null || settings.keystoretype.equals("=default"))
	        		settings.keystoretype = KeyStore.getDefaultType();
	        	KeyStore ks2 = KeyStore.getInstance(settings.keystoretype);
		        ks2.load(App.class.getResourceAsStream("/" + settings.keystore), settings.keystorepw.toCharArray());		            
	        	if(settings.trustkeystoretype == null || settings.trustkeystoretype.equals("=default"))
	        		settings.trustkeystoretype = KeyStore.getDefaultType();
	        	KeyStore tks2 = KeyStore.getInstance(settings.trustkeystoretype);
		        tks2.load(App.class.getResourceAsStream("/" + settings.trustkeystore), settings.trustkeystorepw.toCharArray());
	        	// Set up key manager factory to use our key store
	            KeyManagerFactory kmf2 = KeyManagerFactory.getInstance(algorithm);
	            TrustManagerFactory tmf2 = TrustManagerFactory.getInstance(algorithm);
	            kmf2.init(ks2, settings.keystorepw.toCharArray());
	            tmf2.init(tks2);
	            clientContext = SSLContext.getInstance(PROTOCOL);
	            clientContext.init(kmf2.getKeyManagers(), tmf2.getTrustManagers(), new SecureRandom());          
	        } catch (Exception e) {
	            logger.error("#SSLException", e);
	        }
	        logger.info("#process: SSLContext loaded");
		}
        return clientContext;
    }

}
