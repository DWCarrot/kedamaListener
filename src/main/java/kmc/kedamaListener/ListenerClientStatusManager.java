package kmc.kedamaListener;

import java.time.Duration;
import java.time.LocalDateTime;

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
		status.start = LocalDateTime.now();
		status.current = null;
		status.lastfail = null;
		status.running = null;
		status.restartlistener = 0;
		status.restartpinger = 0;
	}
	
	public void setFailTime() {
		status.lastfail = LocalDateTime.now();
	}
	
	public long getRunnningTime() {
		LocalDateTime thisfail = LocalDateTime.now();
		long running;
		if(status.lastfail == null)
			running = Duration.between(status.start, thisfail).toMillis();
		else
			running = Duration.between(status.lastfail, thisfail).toMillis();
		status.lastfail = thisfail;
		return running;
	}
	
	public void addClientRestart() {
		++status.restartlistener;
	}
	
	public ListenerClientStatus getListenerClientStatus() {
		status.current = LocalDateTime.now();
		status.running = Duration.between(status.start, status.current);
		return status;
	}
}
