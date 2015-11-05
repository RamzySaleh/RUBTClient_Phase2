package RUBTClient;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        
        final Client downloadClient = new Client(tracker, fp, args[1]);
        downloadClient.checkFileState();
        

        final Server server = new Server(tracker);
        
        
        final updateTracker ut = new updateTracker(tracker, trackerUpdateInterval);
        
        
        ExecutorService pool = Executors.newFixedThreadPool(3);
        
        Runnable r1 = new Runnable()
        {
           @Override
           public void run()
           {
        	   downloadClient.run();
           }
        };
       pool.execute(r1);
        
        Runnable r2 = new Runnable()
        {
           @Override
           public void run()
           {
        	   server.run();
           }
        };
        pool.execute(r2);
        
        Runnable r3 = new Runnable()
        {
           @Override
           public void run()
           {
        	   ut.run();
           }
        };
        pool.execute(r3);
        
        while(!Client.userInput.equals("-1")){
        	Thread.sleep(700);
        }
        pool.shutdown();

    }
    
    public static class updateTracker implements Runnable{ 
    	
    	public static Tracker tracker;
    	public static int trackerUpdateInterval;
    	
    	public updateTracker(Tracker tracker, int trackerUpdateInterval){
    		updateTracker.tracker = tracker;
    		updateTracker.trackerUpdateInterval = trackerUpdateInterval;
    	}
    	public void run(){
    		long startTime = System.nanoTime();
    		long currentTime;
    		int i = 0;
    		while(!Client.userInput.equals("-1")){
    			currentTime = System.nanoTime();
    			if(startTime-currentTime>(i*(trackerUpdateInterval)+10)){
        			System.out.println("Tracker updated!");        			
        			tracker.sendTrackerRequest(Event.NONE);
    			}
    		}
    		System.out.println("Tracker update quit!");
    	}
    }
}
