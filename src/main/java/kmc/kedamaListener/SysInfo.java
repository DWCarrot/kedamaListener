package kmc.kedamaListener;

import com.google.gson.JsonObject;

public class SysInfo {

	private static String[] keys = {
			"os.name", "os.version", "os.arch",
			"java.vm.name", "java.vm.version", "java.vm.vendor",
			"user.country", "user.name", "file.encoding"
			};
	
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
		JsonObject obj, obj2;
		for(String key : keys) {
			for(i = 0, j = key.indexOf('.'), obj = res; j > 0; i = j + 1, j = key.indexOf('.', i)) {
				field = key.substring(i, j);
				obj2 = (JsonObject)obj.get(field);
				if(obj2 == null) {
					obj2 = new JsonObject();
					obj.add(field, obj2);
				}
				obj = obj2;
			}
			field = key.substring(i);
			obj.addProperty(field, System.getProperty(key));
		}
		return res;
	}
	
}
