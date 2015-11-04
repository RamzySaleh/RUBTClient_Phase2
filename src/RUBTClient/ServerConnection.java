package RUBTClient;

import java.net.ServerSocket;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import GivenTools.TorrentInfo;


public class ServerConnection extends Thread{
	public static int fileLength;
	public static int pieceLength;
	public static int numPieces;
	public static List<Peer> peers;
	public static TorrentInfo torrentInfo;
	public static boolean[] pieceDownloaded;
	public static ArrayList<Integer> listPiecesDownloaded;
	private int lastPieceDownloaded;
	public static byte[] fileOut;
	public DataInputStream in;
	public DataOutputStream out;
	private Socket conn;

	public ServerConnection(Server serve, Socket conn) {
		fileLength = serve.fileLength;
		pieceLength = serve.pieceLength;
		numPieces = serve.numPieces;
		torrentInfo = serve.torrentInfo;
		peers = serve.peers;
		this.conn = conn;
		listPiecesDownloaded = serve.listPiecesDownloaded;
		try{
			out = new DataOutputStream(conn.getOutputStream());
			in =  new DataInputStream(conn.getInputStream());
		}
		catch(IOException e ){
			System.out.println("could not get the input and output streams of a connection");
			e.printStackTrace();
		}
	}

	public void run() {
		byte[] handshake = null;
		int length;

		try{
			length= in.readInt();
			handshake= new byte[length];
			in.readFully(handshake);
		}
		catch(IOException e){
			System.out.println("could not read from inputstream");
			e.printStackTrace();
		}

		if(!Client.verifyHandshake(handshake)){
			System.out.println("could not verify handshake");
			try{
				conn.close();
			}
			catch(IOException e){
				System.out.println("closing connection failed");
				e.printStackTrace();
			}
			return;
		}

		for (int i = 0; i < listPiecesDownloaded.size(); i++) {
			try {
				Message havePiece = new Message((byte) 4, 5, listPiecesDownloaded.get(i), "-1".getBytes(), -1, -1, -1, -1, -1, "-1".getBytes());
				out.write(havePiece.message);
			}
			catch (IOException e) {
				System.out.println("could not write to outstream");
				e.printStackTrace();
			}
		}

		for (int i = 0; i < 3; i++) {
			byte[] interest = null;
			try {
				length = in.readInt();
				interest = new byte[length];
				in.readFully(interest);
			} catch (IOException e) {
				System.out.println("could not read from inputstream");
				e.printStackTrace();
			}

			String isInterested = null;
			try {
				isInterested = Message.decodeMessage(interest);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("the message was not of the expected format");
			}

			if (isInterested.equals("interested")) {
				break;
			}

			if ((isInterested.equals("uninterested")) || ((i == 2) && (!isInterested.equals("interested")))) {
				try {
					conn.close();
					return;
				}
				catch (IOException e) {
					System.out.println("could not close socket");
					e.printStackTrace();
				}
			}
		}

		Message unchoke = new Message((byte)1, 1, -1, "-1".getBytes(), -1, -1, -1, -1, -1, "-1".getBytes());
		try{
			out.write(unchoke.message);
			out.flush();
		}
		catch(IOException e){
			System.out.println("could not write to outstream");
			e.printStackTrace();
		}

	}
}
