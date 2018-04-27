package kmc.kedamaListener;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SysInfo {

	private static List<String> keys = SettingsManager.getSettingsManager().getSettings().sysinfo;
	
	private static JsonObject info;
	
	public static JsonObject getSysInfo() {
		if(info == null)
			info = get();
		return info;
	}
	
	private static JsonObject get() {
		JsonObject res = new JsonObject();
		int i, j;
		String field;
		JsonObject obj;
		JsonElement obj2;
		for(String key : keys) {
			for(i = 0, j = key.indexOf('.'), obj = res; j > 0; i = j + 1, j = key.indexOf('.', i)) {
				field = key.substring(i, j);
				obj2 = obj.get(field);
				if(obj2 == null) {
					obj2 = new JsonObject();
					obj.add(field, obj2);
				} 
				if(!obj2.isJsonObject()) {
					JsonObject obj3 = new JsonObject();
					obj3.add("", obj2);					
					obj.add(field, obj3);
					obj = obj3;
				}
				else
					obj = (JsonObject) obj2;
			}
			field = key.substring(i);
			obj.addProperty(field, System.getProperty(key));
		}
		return res;
	}
	
}
