package kmc.kedamaListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import kmc.kedamaListener.WatchDogTimer.WorkingProcess;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
//    	GetJsonRecord g = new GetJsonRecord(
//    						"jsonRecord",
//    						"record.json",
//    						"record-%d{yyyy-MM-dd}.json"
//    						);
//    	
//    	try {
//    		long a = System.currentTimeMillis();
//			g.merge(g.getFiles(LocalDate.of(2018, 4, 10), LocalDate.now()), new TestOut());
//			long b = System.currentTimeMillis();
//			System.out.println();
//			System.out.println(b - a);
//    	} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    	
//    	DataServer s = new DataServer(18088);
//    	try {
//    		s.init();
//    		s.start();
//    		while(System.in.read() != 's');
//    		s.close(null);
//    		Thread.sleep(2000);
//    	} catch (InterruptedException | IOException e) {
//    		s.close(null);
//			e.printStackTrace();
//		} finally {
//			
//		}
    	
//    	WatchDogTimer wdt = new WatchDogTimer(5000, new WorkingProcess() {
//			
//			@Override
//			public void reboot() throws Exception {
//				System.out.println("rebbot");
//			}
//			
//			@Override
//			public void exceptionCaught(Throwable cause) throws Exception {
//				cause.printStackTrace();
//			}
//		});
//    	
//    	Scanner input = new Scanner(System.in);
//		while(input.hasNext()) {
//			int i = input.nextInt();
//			if(i == 1)
//				wdt.reset();
//			if(i == 2)
//				wdt.stop();
//			if(i == 3)
//				wdt.start();
//			if(i == 4)
//				break;
//		}
//    	input.close();
    	
        assertTrue( true );
    }
}
