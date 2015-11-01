package RUBTClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import GivenTools.*;
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
        
        File fp = new File("File.dat");
        Client downloadClient = new Client(tracker, fp);
        downloadClient.checkFileState();
        downloadClient.fetchFile(args[1]);


    }
}
