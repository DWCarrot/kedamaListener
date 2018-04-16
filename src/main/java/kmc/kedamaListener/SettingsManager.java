package kmc.kedamaListener;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.google.gson.Gson;

import kmc.kedamaListener.js.settings.IRCSettings;
import kmc.kedamaListener.js.settings.MCPingSettings;
import kmc.kedamaListener.js.settings.Settings;


public class SettingsManager {

	private static SettingsManager obj;
	
	public static SettingsManager getSettingsManager(String settingFile) throws FileNotFoundException {
		obj = new SettingsManager();
		Gson gson = new Gson();	
		InputStream infile = App.class.getResourceAsStream("/" + settingFile);
		obj.settings = gson.fromJson(new InputStreamReader(infile, Charset.forName("UTF-8")) , Settings.class);
		return obj;
	}
	
	public static SettingsManager getSettingsManager() {
		return obj;
	}
	
	public Settings settings;
	
	private SettingsManager() {
		
	}
	
	public Settings getSettings() {
		return settings;
	}
	
	public IRCSettings getIrc() {
		return settings.irc;
	}

	public MCPingSettings getMcping() {
		return settings.mcping;
	}

}
