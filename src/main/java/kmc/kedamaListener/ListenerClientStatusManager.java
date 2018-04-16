package kmc.kedamaListener;

import java.util.Date;

public class ListenerClientStatusManager {
	
	private static ListenerClientStatusManager obj;
	
	public static ListenerClientStatusManager getListenerClientStatusManager() {
		if(obj == null)
			obj = new ListenerClientStatusManager();
		return obj;
	}
	
	public ListenerClientStatus status;
	
	private ListenerClientStatusManager() {
		status = new ListenerClientStatus();
		status.start = new Date();
		status.current = new Date();
		status.lastfail = null;
		status.running = 0;
		status.restartlistener = 0;
		status.restartpinger = 0;
	}
	
	public ListenerClientStatus getListenerClientStatus() {
		status.current.setTime(System.currentTimeMillis());
		status.running = status.current.getTime() - status.start.getTime();
		return status;
	}
}
