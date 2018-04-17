package kmc.kedamaListener;

import java.util.Date;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kmc.kedamaListener.ListenerClientStatusManager;
import kmc.kedamaListener.js.settings.IRCSettings;
/**
 * Hello world!
 *
 */



public class App {
	
	public static Logger logger;
	
	public static int failTimes;
	
	public final static int version = 4;
	
    public static void main( String[] args ) {

    	failTimes = 0;
    	logger = Logger.getLogger(App.class);
    	logger.info("#start @version: " + version);
    	Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    	
    	try {   		
        	SettingsManager mgrs = SettingsManager.getSettingsManager("settings.json");
        	ListenerClientStatusManager mgr = ListenerClientStatusManager.getListenerClientStatusManager();      	
        	ListenerClient c = new ListenerClient();
    		SslContextFactory.getClientContext(mgrs.getIrc().host.ssl);
    		
    		IRCSettings ircs = mgrs.getIrc();
    		ListenerClientStatus s = mgr.getListenerClientStatus();    		
    		long tmp = 0;
    		while(failTimes <= ircs.maxfailtime) {
				c.settingRecord();
				c.start();
				tmp = System.currentTimeMillis();
				s.restartlistener++;
				if(failTimes < 0)
					break;
				if(s.lastfail == null || tmp - s.lastfail.getTime() > (ircs.normalworking + ircs.retryperiod) * 1000L)
					failTimes = 0;
				else
					failTimes++;
				if(s.lastfail == null)
					s.lastfail = new Date();
				s.lastfail.setTime(tmp);
				App.logger.info("#state " + gson.toJson(mgr.getListenerClientStatus()));
				Thread.sleep(ircs.retryperiod * 1000L);
    		}
		} catch (Exception e) {
			logger.error("#Exception @" + Thread.currentThread().getName(), e);
		}
    	logger.info("#end");
    	
    	
    }
    
}