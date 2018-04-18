package kmc.kedamaListener;

import java.util.Scanner;

import com.google.gson.Gson;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
    	ListenerClientStatusManager mgr = ListenerClientStatusManager.getListenerClientStatusManager();
    	App.genGsonBuilder();
    	Gson gson = App.gsonbuilder.create();
    	ListenerClientStatus s = mgr.getListenerClientStatus();
    	String p = gson.toJson(s, s.getClass());
    	System.out.println(p);
        assertTrue( true );
    }
}
