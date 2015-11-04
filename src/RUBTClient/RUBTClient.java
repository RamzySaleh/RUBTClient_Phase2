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
    	
    	final Tracker tracker = new Tracker(args[0]);
        tracker.sendTrackerRequest(Event.NONE);
        

        final int trackerUpdateInterval = Math.min(tracker.interval/2, 180/2);
        
        File fp = new File("File.tmp");
        
        final Client downloadClient = new Client(tracker, fp);
        downloadClient.checkFileState();
        

        final Server server = new Server(tracker, downloadClient);
        
        
        Thread thread1 = new Thread(){
        	{downloadClient.fetchFile(args[1]);}
        };
        Thread thread2 = new Thread(){
        	{server.run();}
        }; 
        Thread thread3 = new Thread(){
        	{updateTracker(tracker, trackerUpdateInterval);}
        };
        
        thread1.start();
        thread2.start();
        thread3.start();
        
        thread1.join();
        
        tracker.sendTrackerRequest(Event.STOPPED);

    }
    
    public static void updateTracker(Tracker tracker, int trackerUpdateInterval){
    	while(true){
    		try {
				Thread.sleep(1000*(trackerUpdateInterval+1));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		tracker.sendTrackerRequest(Event.NONE);
    	}
    }
}
