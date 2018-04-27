package kmc.kedamaListener;


import org.slf4j.Logger;

import com.google.gson.Gson;
import kmc.kedamaListener.PlayerCount;

public class PlayerCountRecord {

	private Gson gson = App.gsonbuilder.create();
	
	private Logger logger = App.logger;
	
	private PlayerCount plc;
	
	private static PlayerCountRecord obj;
	
	public static PlayerCountRecord getPlayerCountRecord() {
		if(obj == null)
			obj = new PlayerCountRecord();
		return obj;
	}
	
	public static String getSPlayerCountRecord() {
		if(obj == null || obj.plc == null)
			return null;
		else
			return obj.gson.toJson(obj.plc);
	}
	
	public static void release() {
		if(obj != null)
			obj.plc = null;
		obj = null;
	}
	
	private PlayerCountRecord() {
		plc = new PlayerCount();
	}
	
	public PlayerCount getPlayerCount() {
		return plc;
	}
	
	public synchronized void record() {
		String s = gson.toJson(plc);
		plc.setContinuous(true);
		RecordInJson.logger.info(s);
		RecordInCSV.logger.info(
				"{},{},{}",
				plc.getTimestamp().toEpochMilli(),
				plc.getTime().format(App.formatter),
				plc.getOnlineNum()
			);
		logger.info("#record {}", s);
	}
}
