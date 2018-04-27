package kmc.kedamaListener;

public class WatchDogTimer implements Runnable {

	public interface WorkingProcess {
		
		public void reboot() throws Exception;
		
		public void exceptionCaught(Throwable cause) throws Exception;
	}
	
	private Thread t;
	
	private long timeout;
	
	private boolean unfeeded;
	
	private byte[] lock;
	
	private WorkingProcess workingProcess;
	
	public WatchDogTimer(long timeout, WorkingProcess workingProcess) {
		this.timeout = timeout;
		this.workingProcess = workingProcess;
		lock = new byte[] {0x00};
	}
	
	public void start() {
		if(lock == null || t != null)
			return;
		t = new Thread(this);
		t.start();
	}
	
	public void stop() {
		if(t != null)
			t.interrupt();
	}
	
	public void reset() {
		if(lock == null)
			return;
		synchronized (lock) {
			unfeeded = false;
			lock.notify();
		}
	}
	
	@Override
	public void run() {
		try {
			while(!Thread.interrupted()) {
				synchronized (lock) {
					unfeeded = true;
					lock.wait(timeout);
					if(unfeeded)
						try {
							workingProcess.reboot();
						} catch (Exception e) {
							workingProcess.exceptionCaught(e);
						}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			unfeeded = false;
			t = null;
		}
	}

}
