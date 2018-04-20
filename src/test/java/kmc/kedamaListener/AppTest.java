package kmc.kedamaListener;

import java.io.FileNotFoundException;

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
    	App.genGsonBuilder();
    	Gson gson = App.gsonbuilder.create();
    	try {
			SettingsManager.getSettingsManager("settings.json");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String p = gson.toJson(SysInfo.getSysInfo());
    	System.out.println(p);
        assertTrue( true );
    }
}
