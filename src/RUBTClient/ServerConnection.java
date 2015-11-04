package RUBTClient;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.io.DataInputStream;
import java.io.DataOutputStream;

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
		fileOut = serve.fileOut;
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

		// Read handshake from client
		try{
			length= in.readInt();
			handshake= new byte[length];
			in.readFully(handshake);
		}
		catch(IOException e){
			System.out.println("could not read from inputstream");
			e.printStackTrace();
		}

		// Verify handshake
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

		// Send have message for each piece we have
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

		// Check if client sent interested message
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

			if (interest[0] == (byte)2) {
				// Client sent interested
				break;
			}

			if ((interest[0] == (byte)3) || ((i == 2) && (!(interest[0] == (byte)2)))) {
				// Client didn't send interested or sent not interested so close connection
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

		// Send unchoke message to client
		Message unchoke = new Message((byte)1, 1, -1, "-1".getBytes(), -1, -1, -1, -1, -1, "-1".getBytes());
		try{
			out.write(unchoke.message);
			out.flush();
		}
		catch(IOException e){
			System.out.println("could not write to outstream");
			e.printStackTrace();
		}

		// Read request messages
		byte[] request = null;
		try {
			while (true) {
				length = in.readInt();
				request = new byte[length];
				in.readFully(request);
				ByteBuffer buffer = ByteBuffer.wrap(request);

				int id = buffer.get();

				if (id == 1) {
					// Client sent not interested
					break;
				} else if (id != 6) {
					// Client sent message other than request
					continue;
				} else {
					int index = buffer.getInt();
					int begin = buffer.getInt();
					int len = buffer.getInt();

					if (pieceDownloaded[index]) {
						// Send block to client
						byte[] block = new byte[len];
						System.arraycopy(fileOut, index*pieceLength+begin, block, 0, len);
						Message piece = new Message((byte)7, 9 + len, -1, "-1".getBytes(), -1, -1, -1, index, begin, block);

						out.write(piece.message);
						out.flush();
					}
				}

			}
		}
		catch (IOException e) {
			System.out.println("could not read from inputstream");
			e.printStackTrace();
		}
		catch (Exception e) {
			System.out.println("the message was not of the expected format");
			e.printStackTrace();
		}

		try {
			conn.close();
		}
		catch (IOException e) {
			System.out.println("could not close socket");
			e.printStackTrace();
		}
	}
}
