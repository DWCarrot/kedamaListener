package kmc.kedamaListener;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.SystemPropertyUtil;
import kmc.kedamaListener.js.settings.SSLSettings;

public class ServerSslContextFactory {
	
	private static SslContext serverContext;
	private static Logger logger = LoggerFactory.getLogger(ServerSslContextFactory.class);

	public static SslContext getServerContext(SSLSettings settings) {
		if(serverContext == null && settings != null) {
			String algorithm = SystemPropertyUtil.get("ssl.KeyManagerFactory.algorithm");
	        if (algorithm == null)
	            algorithm = "SunX509";
	        try {
	        	KeyManagerFactory kmf1 = null;
	        	if(settings.keystore != null) {
		        	if(settings.keystoretype == null || settings.keystoretype.equals("=default"))
		        		settings.keystoretype = KeyStore.getDefaultType();
		        	KeyStore ks1 = KeyStore.getInstance(settings.keystoretype);
			        ks1.load(new FileInputStream(settings.keystore), settings.keystorepw.toCharArray());
			        kmf1 = KeyManagerFactory.getInstance(algorithm);
			        kmf1.init(ks1, settings.keystorepw.toCharArray());
	        	}
	        	TrustManagerFactory tmf1 = null;
	        	if(settings.trustkeystore != null) {
		        	if(settings.trustkeystoretype == null || settings.trustkeystoretype.equals("=default"))
		        		settings.trustkeystoretype = KeyStore.getDefaultType();
		        	KeyStore tks1 = KeyStore.getInstance(settings.trustkeystoretype);
			        tks1.load(new FileInputStream(settings.trustkeystore), settings.trustkeystorepw.toCharArray());
		            tmf1 = TrustManagerFactory.getInstance(algorithm); 
		            tmf1.init(tks1);
	        	}
	            serverContext = SslContextBuilder.forServer(kmf1).trustManager(tmf1).build();         
	        } catch (Exception e) {
	            logger.warn("#SSLException @{}", Thread.currentThread(), e);
	        }
	        logger.info("#process: ServerSSLContext loaded");
		}
        return serverContext;
	}

	public static void clear() {
		serverContext = null;
	}
}
