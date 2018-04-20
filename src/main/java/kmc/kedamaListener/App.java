package kmc.kedamaListener;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kmc.kedamaListener.ListenerClientStatusManager;
import kmc.kedamaListener.js.settings.IRCSettings;
/**
 * Hello world!
 *
 */



public class App {
	
	public static Logger logger;
	
	public static int failTimes;
	
	public final static int version = 16;
	
	public static GsonBuilder gsonbuilder;
	
	public static ZoneId zone = ZoneId.of("Asia/Shanghai");
	
	
	public static DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	
	public static void genGsonBuilder () {
		gsonbuilder = new GsonBuilder();
    	gsonbuilder.registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {   		  		
			@Override			
			public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {				
				return new JsonPrimitive(ZonedDateTime.of(src, zone).format(formatter));
			}  		
    	});
    	
    	gsonbuilder.registerTypeAdapter(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
			@Override
			public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
				return new JsonPrimitive(src.format(formatter));
			}
		});
    	gsonbuilder.registerTypeAdapter(Duration.class, new JsonSerializer<Duration>() {
			@Override
			public JsonElement serialize(Duration src, Type typeOfSrc, JsonSerializationContext context) {
				return new JsonPrimitive(src.toMillis());
			}
		});
    	gsonbuilder.registerTypeAdapter(Instant.class, new JsonSerializer<Instant>() {
			@Override
			public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
				return new JsonPrimitive(src.toEpochMilli());
			}
		});
	}
	
    public static void main( String[] args ) {

    	failTimes = 0;
    	logger = LoggerFactory.getLogger(App.class);
    	logger.info("#start @version={}", version);
    	
    	genGsonBuilder();
    	
    	Gson gson = gsonbuilder.create();   	
    	try {   		
        	SettingsManager mgrs = SettingsManager.getSettingsManager("settings.json");
        	ListenerClientStatusManager mgr = ListenerClientStatusManager.getListenerClientStatusManager();      	
        	ListenerClient c = new ListenerClient();
    		SslContextFactory.getClientContext(mgrs.getIrc().host.ssl);
    		
    		IRCSettings ircs = mgrs.getIrc();	
    		while(failTimes <= ircs.maxfailtime) {
				c.settingRecord();
				c.start();
				mgr.addClientRestart();
				if(failTimes < 0)
					break;
				if(mgr.getRunnningTime() > (ircs.normalworking + ircs.retryperiod) * 1000L)
					failTimes = 0;
				else
					failTimes++;
				App.logger.info("#state {}" ,gson.toJson(mgr.getListenerClientStatus()));
				Thread.sleep(ircs.retryperiod * 1000L);
    		}
		} catch (Exception e) {
			logger.error("#Exception @{}" , Thread.currentThread().getName(), e);
		}
    	logger.info("#end\r\n");   	
    }
    
}