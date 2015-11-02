package RUBTClient;

import java.util.List;

import GivenTools.TorrentInfo;

public class Server {

	public static Tracker tracker;
	public static Client downloadClient;
	public static List<Peer> peers;
	private static TorrentInfo torrentInfo;
    private static int fileLength;
    private static int pieceLength;
	
	public Server(Tracker tracker, Client downloadClient){
		Server.tracker = tracker;
		Server.downloadClient = downloadClient;
		Server.peers = tracker.peers;
		Server.torrentInfo = tracker.torrentInfo;
		Server.pieceLength = torrentInfo.piece_length;
		Server.fileLength = torrentInfo.file_length;
	}
	
	public void run(){
		
	}
}
