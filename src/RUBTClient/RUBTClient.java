package RUBTClient;

import java.io.*;
import RUBTClient.Tracker.Event;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */
public class RUBTClient
{


    public static void main(String[] args) throws Exception
    {
    	if (args.length < 2){
    		System.out.println("Too few arguments");
    		return;
    	}
    	
        Tracker tracker = new Tracker(args[0]);
        tracker.sendTrackerRequest(Event.NONE);
        
        File fp = new File("File.tmp");
        Client downloadClient = new Client(tracker, fp);
        downloadClient.checkFileState();
        downloadClient.fetchFile(args[1]);

        tracker.sendTrackerRequest(Event.STOPPED);

    }
}
