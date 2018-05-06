package kmc.kedamaListener;

import java.io.File;
import java.lang.reflect.Type;

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
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Hello world!
 *
 */



public class App {
	
	public static Logger logger;
	
//	public static int failTimes;
	
	public final static Integer version = 44;
	
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
	
    public static void main(String[] args) {
    	System.out.println(new File(System.getProperty("user.dir")));
    	try {
        	logger = LoggerFactory.getLogger(App.class);
        	logger.info("#start @version={}", version);
        	genGsonBuilder();
//        	Gson gson = gsonbuilder.create();
        	SettingsManager mgrs = SettingsManager.getSettingsManager("settings.json");
//        	ListenerClientStatusManager mgr = ListenerClientStatusManager.getListenerClientStatusManager();      	
        	
        	ServerSslContextFactory.getServerContext(mgrs.getSettings().dataserver.ssl);
    		ClientSslContextFactory.getClientContext(mgrs.getIrc().host.ssl);    		
//    		IRCSettings ircs = mgrs.getIrc();	
    		
    		logger.info("#process initialized");
    		
    		ListenerClient c = new ListenerClient();
    		
    		DataServer s = new DataServer(mgrs.getSettings().dataserver.host ,mgrs.getSettings().dataserver.port, mgrs.getSettings().dataserver.indexs)
    							.setDataFileLocate(mgrs.getSettings().filesave)
    							.setPage404(mgrs.getSettings().dataserver.page404);
    		
    		Scanner input = new Scanner(System.in);
    		label1:
    		while(input.hasNext()) {
    			String msg = input.nextLine();
    			logger.info(msg);
    			switch (msg) {
    			case "launcher close":
    				if(!c.isClosed()) {
    					c.close(future -> {version.notify();});
    					synchronized (version) {
    						version.wait(10 * 1000);
    						logger.info("#processs :IRCListener terminated");
    					}
    				}
    				if(!s.isClosed()) {
						s.close(future -> {version.notify();});
						synchronized (version) {
							version.wait(10 * 1000);
							logger.info("#=DataServer process=terminated");
						}
					}
    				break label1;	
				case "client start":
					if(c.isClosed()) {
						c.init();
			    		c.start();
					}
					break;
				case "client close":
					if(!c.isClosed()) {
						c.close(future -> {version.notify();});
						synchronized (version) {
							version.wait(10 * 1000);
							logger.info("#processs :IRCListener terminated");
						}
					}
					break;
				case "server start":
					if(s.isClosed()) {
						s.init();
						s.start();
					}
					break;
				case "server close":
					if(!s.isClosed()) {
						s.close(future -> {version.notify();});
						synchronized (version) {
							version.wait(10 * 1000);
							logger.info("#=DataServer process=terminated");
						}
					}
					break;
				default:
					break;
				}
    		}
    		input.close();
    		
    		
    		
//    		while(failTimes <= ircs.maxfailtime) {
//				c.settingRecord();
//				c.start();
//				mgr.addClientRestart();
//				if(failTimes < 0)
//					break;
//				if(mgr.getRunnningTime() > (ircs.normalworking + ircs.retryperiod) * 1000L)
//					failTimes = 0;
//				else
//					failTimes++;
//				App.logger.info("#state {}" ,gson.toJson(mgr.getListenerClientStatus()));
//				Thread.sleep(ircs.retryperiod * 1000L);
//    		}
    		
		} catch (Exception e) {
			logger.warn("#Exception @{}" , Thread.currentThread(), e);
		}
    	logger.info("#end\r\n");   	
    }
    
}