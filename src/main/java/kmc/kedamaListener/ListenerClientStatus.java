package kmc.kedamaListener;

import java.time.Duration;
import java.time.LocalDateTime;

public class ListenerClientStatus {
	
	public LocalDateTime current;
	
	public LocalDateTime start;
	
	public LocalDateTime lastfail;
	
	public Duration running;
		
	public int restartlistener;
		
	public int restartpinger;			
}