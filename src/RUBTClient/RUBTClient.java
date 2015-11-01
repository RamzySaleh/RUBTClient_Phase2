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


    public static void main(final String[] args) throws Exception
    {
    	if (args.length < 2){
    		System.out.println("Too few arguments");
    		return;
    	}
    	
        Tracker tracker = new Tracker(args[0]);
        tracker.sendTrackerRequest(Event.NONE);
        
        File fp = new File("File.tmp");
        
        final Client downloadClient = new Client(tracker, fp);
        downloadClient.checkFileState();
        final Server server = new Server();

        Thread thread1 = new Thread(){
        	{downloadClient.fetchFile(args[1]);}
        };
        Thread thread2 = new Thread(){
        	{server.run();}
        };
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        tracker.sendTrackerRequest(Event.STOPPED);

    }
}
