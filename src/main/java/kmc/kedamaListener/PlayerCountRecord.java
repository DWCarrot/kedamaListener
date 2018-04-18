package kmc.kedamaListener;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kmc.kedamaListener.PlayerCount;

public class PlayerCountRecord {

	private Gson gson = App.gsonbuilder.create();
	
	private PlayerCount plc = new PlayerCount();
	
	private static PlayerCountRecord obj;
	
	public static PlayerCountRecord getPlayerCountRecord() {
		if(obj == null)
			obj = new PlayerCountRecord();
		return obj;
	}
	
	public static void release() throws IOException {
		if(obj != null)
			obj.plc = null;
		obj = null;
	}
	
	private PlayerCountRecord() {

	}
	
	public PlayerCount getPlayerCount() {
		return plc;
	}
	
	public synchronized void record() throws IOException {
		String s = gson.toJson(plc);
		RecordInJson.logger.info(s);
		RecordInCSV.logger.info(
				"{},{},{}",
				plc.getTimestamp().toEpochMilli(),
				plc.getTime().format(App.formatter),
				plc.getOnlineNum()
			);
		App.logger.info("#record {}", s);
	}
}
