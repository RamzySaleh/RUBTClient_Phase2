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
	public ServerConnection(Server serve, Socket conn){
		fileLength=serve.fileLength;
		pieceLength=serve.pieceLength;
		numPieces=serve.numPieces;
		torrentInfo=serve.torrentInfo;
		peers=serve.peers;
		this.conn=conn;
		listPiecesDownloaded=serve.listPiecesDownloaded;
		try{
		out = new DataOutputStream(conn.getOutputStream());
        in =  new DataInputStream(conn.getInputStream());
		}
		catch(IOException e ){
			System.out.println("could not get the input and output streams of a connection");
			e.printStackTrace();
		}
	}
	public void run(){
		byte[] handshake=null;
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
		if(!verifyHandStart(handshake)){
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
		BitSet verified= new BitSet(numPieces);
		for(int x=0;x<numPieces;x++){
			verified.set(x, pieceDownloaded[x]);
		}
		/*this is definitely the wrong way to do it, as the payload is supposed to have only the bitfield.  not sure how to write 
		a bitfield that isn't divisable by 8 into a byte array i'll look into it more after i push out a general complete pic.
		*/
		byte[] verifyBitField=new byte[numPieces/8+1];
		verifyBitField=toByteArray(verified);
		byte[] dummy= new byte[1];
		int len=verifyBitField.length+1;
		Message bitFieldDecleration= new Message((byte)5, len, -1, verifyBitField, -1, -1, 1, -1, -1, dummy );
		try{
			out.write(bitFieldDecleration.message);
			out.flush();
		}
		catch(IOException e){
			System.out.println("could not write to outstream");
			e.printStackTrace();
		}
		byte[] interest=null;
		try{
			length= in.readInt();
			interest= new byte[length];
			in.readFully(interest);
			}
			catch(IOException e){
				System.out.println("could not read from inputstream");
				e.printStackTrace();			
		}
		String isInterested=null;
		try{
		isInterested=Message.decodeMessage(interest);
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("the message was not of the expected format");
		}
		if (!isInterested.equals("interested")){
			System.out.format("we expected interested but got %s \n", isInterested);
			//should quit or something here
			}
		len=1;
		Message unchoke= new Message((byte)1, len, -1, dummy, -1, -1, 1, -1, -1, dummy );
		try{
			out.write(unchoke.message);
			out.flush();
		}
		catch(IOException e){
			System.out.println("could not write to outstream");
			e.printStackTrace();
		}
		
	}
	//this method is used to verify the begenning of a handshake sent by the client. 
	private boolean verifyHandStart(byte[] dat){
		if(dat==null){
			return false;
		}
		byte[] zeroArr = new byte[8];
		byte[] hash= torrentInfo.info_hash.array();
		//the index of info hash in a handshake.
		int index=19*2+8;
		byte[] hashverify=Arrays.copyOfRange(dat, index, index+hash.length);
		return(Arrays.equals(hashverify, hash));
		}
	public static byte[] toByteArray(BitSet bits) {
		 
	    byte[] bytes = new byte[bits.length()/8+1];
	    for (int i=0; i<bits.length(); i++) {
	        if (bits.get(i)) {
	            bytes[bytes.length-i/8-1] |= 1<<(i%8);
	        }
	    }
	    return bytes;
	}
}
