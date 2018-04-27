package kmc.kedamaListener;

import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.SystemPropertyUtil;
import kmc.kedamaListener.js.settings.SSLSettings;

public class ClientSslContextFactory {

	private static SslContext clientContext;
	private static Logger logger = LoggerFactory.getLogger(ClientSslContextFactory.class);
	
	public static SslContext getClientContext(SSLSettings settings) {
		if(clientContext == null && settings != null) {
			String algorithm = SystemPropertyUtil.get("ssl.KeyManagerFactory.algorithm");
	        if (algorithm == null)
	            algorithm = "SunX509";
	        try {
	        	KeyManagerFactory kmf2 = null;
	        	if(settings.keystore != null) {
		        	if(settings.keystoretype == null || settings.keystoretype.equals("=default"))
		        		settings.keystoretype = KeyStore.getDefaultType();
		        	KeyStore ks2 = KeyStore.getInstance(settings.keystoretype);
		        	ks2.load(App.class.getResourceAsStream("/" + settings.keystore), settings.keystorepw.toCharArray());		            
		            kmf2 = KeyManagerFactory.getInstance(algorithm);
		        	kmf2.init(ks2, settings.keystorepw.toCharArray());
	        	}
	        	TrustManagerFactory tmf2 = null;
	        	if(settings.trustkeystore != null) {
		        	if(settings.trustkeystoretype == null || settings.trustkeystoretype.equals("=default"))
		        		settings.trustkeystoretype = KeyStore.getDefaultType();
		        	KeyStore tks2 = KeyStore.getInstance(settings.trustkeystoretype);
			        tks2.load(App.class.getResourceAsStream("/" + settings.trustkeystore), settings.trustkeystorepw.toCharArray());
		            tmf2 = TrustManagerFactory.getInstance(algorithm);
		            tmf2.init(tks2);
	        	}
	            clientContext = SslContextBuilder.forClient().keyManager(kmf2).trustManager(tmf2).build();         
	        } catch (Exception e) {
	            logger.warn("#SSLException @{}", Thread.currentThread(), e);
	        }
	        logger.info("#process: ClientSSLContext loaded");
		}
        return clientContext;
    }

}
