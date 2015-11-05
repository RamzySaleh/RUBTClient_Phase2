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
        
        final Client downloadClient = new Client(tracker, fp);
        downloadClient.checkFileState();
        

        final Server server = new Server(tracker, downloadClient);
        
        /**
        ExecutorService pool_Download_Upload_Tracker = Executors.newFixedThreadPool(3);
        
        pool_Download_Upload_Tracker.submit(
	    		new Runnable() {public void run() { 
	    			try {downloadClient.fetchFile(args[1]);} 
	    			catch (Exception e) { e.printStackTrace(); } 
	    		}} 
	    );
        
        pool_Download_Upload_Tracker.submit(
	    		new Runnable() {public void run() { 
	    			try {server.run();} 
	    			catch (Exception e) { e.printStackTrace(); } 
	    		}} 
	    );
        
        pool_Download_Upload_Tracker.submit(
	    		new Runnable() {public void run() { 
	    			try {updateTracker(tracker, trackerUpdateInterval);} 
	    			catch (Exception e) { e.printStackTrace(); } 
	    		}} 
	    );
        */
        
        Thread thread1 = new Thread(){
        	{downloadClient.fetchFile(args[1]);}
        };
        Thread thread2 = new Thread(){
        	{System.out.println("Server started running.");
        		server.run();}
        }; 
        Thread thread3 = new Thread(){
        	{updateTracker(tracker, trackerUpdateInterval);}
        };
        
        thread1.start();
        thread2.start();
        thread3.start();
        
        
        

    }
    
    public static void updateTracker(Tracker tracker, int trackerUpdateInterval){
    	while(true){
    		try {
    			System.out.println("Tracker updated!");
				Thread.sleep(1000*(trackerUpdateInterval+1));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		tracker.sendTrackerRequest(Event.NONE);
    	}
    }
}
