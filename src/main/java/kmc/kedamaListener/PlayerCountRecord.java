package kmc.kedamaListener;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kmc.kedamaListener.PlayerCount;

public class PlayerCountRecord {

	private String basicFileName;
	private int maxRecord;
	
	private Gson gson;
	private Charset charset;
	private FileOutputStream jsonRecord;	
	private FileOutputStream csvRecord;
	private int recordCount;
	
	private PlayerCount plc;
	
	private static PlayerCountRecord obj;
	
	public static PlayerCountRecord getPlayerCountRecord(String basicFileName, int maxRecord) {
		obj = new PlayerCountRecord(basicFileName, maxRecord);
		return obj;
	}
	
	public static PlayerCountRecord getPlayerCountRecord() {
		return obj;
	}
	
	public static void release() throws IOException {
		obj.close();
		obj = null;
	}
	
	private PlayerCountRecord(String basicFileName, int maxRecord) {
		this.basicFileName = basicFileName;
		this.maxRecord = maxRecord;
		recordCount = -1;
		gson = new GsonBuilder().create();
		charset = Charset.forName("UTF-8");
		plc = new PlayerCount();
	}
	
	public PlayerCount getPlayerCount() {
		return plc;
	}

	public void newFile() throws IOException {
		close();
		String f = new StringBuilder().append(basicFileName).append('-').append(System.currentTimeMillis()).toString();
		jsonRecord = new FileOutputStream(f + ".json", true);
		csvRecord = new FileOutputStream(f + ".csv", true);
	}
	
	public synchronized void record() throws IOException {
		if(recordCount >= maxRecord || recordCount < 0) {
			newFile();
			recordCount = 0;
		}
		String s = gson.toJson(plc);
		jsonRecord.write(s.getBytes(charset));
		jsonRecord.write(',');
		jsonRecord.write('\r');
		jsonRecord.write('\n');
		jsonRecord.flush();
		csvRecord.write(Long.toString(plc.getTime()).getBytes(charset));
		csvRecord.write(',');
		csvRecord.write(Integer.toString(plc.getOnlineNum()).getBytes(charset));
		csvRecord.write('\r');
		csvRecord.write('\n');
		csvRecord.flush();
		App.logger.info("#record " + s);
		++recordCount;
	}
	
	public void close() throws IOException {
		if(jsonRecord != null)
			jsonRecord.close();
		if(csvRecord != null)
			csvRecord.close();
	}
}
