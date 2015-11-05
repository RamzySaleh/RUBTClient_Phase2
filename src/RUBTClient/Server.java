package RUBTClient;

import java.net.ServerSocket;
import java.net.SocketException;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.net.Socket;

import GivenTools.TorrentInfo;
/* The server needs to be able to send have messages to all connected peers whenever a new piece gets verified.  I created an arraylist that contains a list
 * of verified pieces, and an int to keep track of the last piece downloaded.  periodically, check to see if the last element is different then the recorded last element
 * if it is, send have messages for all pieces between the index of previous last, and the index of current length-1.
 */
public class Server extends Thread implements Runnable{

	public static Tracker tracker;
	public static Server server;
	public static Client downloadClient;
	public static List<Peer> peers;
	public static TorrentInfo torrentInfo;
	public static int port;
    public static int fileLength;
    public static int pieceLength;
    public static int numPieces;
    public static boolean[] pieceDownloaded;
    public static ArrayList<Integer> listPiecesDownloaded;
    public static byte[] fileOut;
    private static ServerSocket serveSocket;
	
	public Server(Tracker tracker, Client downloadClient){
		Server.tracker = tracker;
		port=tracker.port;
		Server.downloadClient = downloadClient;
		Server.peers = tracker.peers;
		Server.torrentInfo = tracker.torrentInfo;
		Server.pieceLength = torrentInfo.piece_length;
		Server.fileLength = torrentInfo.file_length;
		numPieces = (int)Math.ceil((double)torrentInfo.file_length / (double)torrentInfo.piece_length);
		pieceDownloaded=Client.pieceDownloaded;
		listPiecesDownloaded=Client.listPiecesDownloaded;
		Server.fileOut = Client.fileOut;
	}
	
	public void run(){
		try{
			serveSocket= new ServerSocket(port);
			System.out.println("Server socket opened at port = "+port);
			}
			catch(IOException e){
				System.out.println("Could not listen on port = "+port);
				return;
			}
		ArrayList<Socket> peerSockets= new ArrayList<Socket>(5);
		Executor pool = Executors.newFixedThreadPool(5);
		
		int ind=0;
		while(!Client.userInput.equals("-1")){

			try{
				peerSockets.add(ind,serveSocket.accept());
			}catch(IOException e){
				e.printStackTrace();
				System.out.println("Something went wrong with setting up a peer connection with server.");
			}
			final ServerConnection s = new ServerConnection(this,peerSockets.get(ind));  
			Runnable r = new Runnable()
            {
               @Override
               public void run()
               {
       				s.run();
               }
            };
            pool.execute(r);
			ind++;
		}
    	System.out.println("Server quit!");
	}
}
