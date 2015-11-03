package RUBTClient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import GivenTools.*;
import RUBTClient.Tracker.Event;

public class Client{
	
	public Tracker tracker;
	public List<Peer> peers;
	private TorrentInfo torrentInfo;
    private String peer_id;
    public byte[] fileOut;
    private int pieceLength;
    private int numPieces;
    private int fileLength;
    private int alreadyDownloaded = 0;
    public File fp;
    private boolean[] pieceDownloaded;
   
    
    public Client (Tracker tracker, File fp){
    	this.tracker = tracker;
    	this.torrentInfo = tracker.torrentInfo;
    	this.peers = tracker.peers;
    	this.fp = fp;
		this.fileOut = new byte[torrentInfo.file_length];
		this.pieceLength = torrentInfo.piece_length;
		this.fileLength = torrentInfo.file_length;
        numPieces = (int)Math.ceil((double)torrentInfo.file_length / (double)torrentInfo.piece_length);
        pieceDownloaded = new boolean[numPieces];
        this.peer_id = tracker.peer_id;
				
	}

    /**
     * Download the file from a peer
     * @return the full file as a byte array
     * @throws Exception
     */
    public void fetchFile(String fileName) throws Exception {
    	
        // Select peer to connect to
        List<Peer> peersSelected = findAPeer();

        if (peersSelected == null)
        {
            throw new Exception("Could not connect to a peer!");
        } else if (peersSelected.size() == 1) {
        	System.out.println("Only found one peer. Downloading from this peer only");
        	// NEEDS WORK
        }

        if (alreadyDownloaded == numPieces){
        	System.out.println("Already Downloaded!");
        	System.out.println("Download time = 0 seconds");
        	File fileN = new File(fileName);
        	fp.renameTo(fileN);
        	return;
        }

        try {
            // Create socket and connect to peer
        	Peer peer0 = peersSelected.get(0);
        	Peer peer1 = peersSelected.get(1);
            System.out.println("Connecting to peer: " + peer0.getIP());
            peer0.connectPeer();
            System.out.println("Connecting to peer: " + peer1.getIP());
            peer1.connectPeer();

            
            // Create handshake
            Message handshake = new Message(this.peer_id.getBytes(), torrentInfo.info_hash.array());

            // Send handshake to peers
            System.out.println("Sending handshake");
            peer0.out.write(handshake.message);
            peer1.out.write(handshake.message);
            
            peer0.out.flush();
            peer1.out.flush();

            // Receive handshake from peers
            byte[] handshakeResponse0 = new byte[68];
            byte[] handshakeResponse1 = new byte[68];
            
            peer0.in.read(handshakeResponse0);
            peer1.in.read(handshakeResponse1);

            // Verify handshake
            if (!verifyHandshake(handshakeResponse0) || !verifyHandshake(handshakeResponse1)) {
                throw new Exception("Could not verify handshake");
            }

            // Create interested message
            Message interested = new Message((byte) 2, 1, -1, "-1".getBytes(), -1, -1, -1,-1, -1, "-1".getBytes());

            int length;
            int response_id0;
            int response_id1;

            // Send interested message until peer unchokes
            for (int i = 0; i < 20; i++)
            {
            	peer0.out.write(interested.message);
            	peer1.out.write(interested.message);
            	
                peer0.out.flush();
                peer1.out.flush();
                
                
                length = peer0.in.readInt();
                response_id0 =  (int)peer0.in.readByte();
                length = peer1.in.readInt();
                response_id1 = (int)peer1.in.readByte();

               
                if (response_id0 == 1 && response_id1 == 1)
                {
                    System.out.println("Peer unchoked");
                    break;
                }
                else
                {
                    if (i == 19)
                    {
                        throw new Exception("Peer not sending unchoke message");
                    }
                }
            }

            // Send HTTP GET to tracker to indicate download started
            tracker.sendTrackerRequest(Event.STARTED);

            long timeBegin = System.nanoTime();

            int i;
            int count = alreadyDownloaded;
            // Loop through each piece
            BufferedReader bufIn = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter -1 and enter to cancel download -->");
            
            String userInput = "";

            while(count < numPieces && !userInput.equals("-1")){
            {
            	            	
            	if(bufIn.ready()){
            		userInput = bufIn.readLine();
            		if (userInput.equals("-1")){
            			break;
            		}
            	}
            	
            	i = findPieceToDownload();
            	
            	if (i == -1) break;
                
            	
            	if(i%10 == 0 && count!= 0){
            		System.out.println("Enter -1 and enter to cancel download -->");
            	}
            	
                int currentPieceLength;
                
                if (i == numPieces - 1)
                {
                    if (fileLength % pieceLength == 0)
                    {
                        currentPieceLength = pieceLength;
                    }
                    else
                    {
                        currentPieceLength = fileLength % pieceLength;
                    }
                }
                else
                {
                    currentPieceLength = pieceLength;
                }
                
                byte[] piece = new byte[currentPieceLength];
                
                Peer peerToAsk = peer0;
                if (i%2 == 1){
                	peerToAsk = peer1;
                } 
                
                piece = downloadPiece(peerToAsk,i);
                
                // Verify SHA-1 for piece
                if (verifyPiece(piece, i))
                {
                    System.out.println("Piece #"+i+" verified, downloaded from: "+peerToAsk.getIP());
                    pieceDownloaded[i] = true;
                    System.arraycopy(piece, 0, fileOut, i * pieceLength, currentPieceLength);
                    updateSaveFile(piece,i);
                }
                else
                {
                    throw new Exception("Incorrect piece SHA-1");
                }
            }
        }
            peer0.disconnectPeer();
            peer1.disconnectPeer();

        	long timeEnd = System.nanoTime();
        	
        	System.out.println("Download time = "+(timeEnd-timeBegin)/1000000000+" seconds");
        }
        finally{
        	saveCompletedFileToDisk(fileName);
            // Send HTTP GET to tracker to indicate download is complete
        	tracker.sendTrackerRequest(Event.COMPLETED);
        }

    }

    /**
     * Choose peer from peer list to connect to
     * @return Peer object of peer to connect to
     */
    private List<Peer> findAPeer(){

    	List<Peer> peersToDownload = new LinkedList<Peer>();
    	if (peers == null) System.out.println("Peers is null!");
        for (int i = 0; i < peers.size(); i++)
        {
                String currentPeerIP = peers.get(i).getIP();
                
                if (currentPeerIP.equals("128.6.171.130") || currentPeerIP.equals("128.6.171.131"))
                {
                	peersToDownload.add(peers.get(i));                    
                }           
        }

        return peersToDownload;
    }

    /**
     * Compare info_hash in peer response handshake to torrent
     * @param handshake peer response to handshake message
     * @return true if info_hash matches
     */
    private boolean verifyHandshake(byte[] handshake)
    {
        byte[] info_hash = new byte[20];

        System.arraycopy(handshake, 28, info_hash, 0, 20);

        return (Arrays.equals(info_hash, torrentInfo.info_hash.array()));
    }

    /**
     * Compare piece SHA-1 to torrent
     * @param piece byte array of piece data
     * @param index index of piece
     * @return true if SHA-1 hashes match
     */
    private boolean verifyPiece(byte[] piece, int index)
    {
        byte sha1Piece[] = new byte[20];
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            sha1Piece = md.digest(piece);
        } catch (NoSuchAlgorithmException e){
            System.err.println(e);
        }

        byte[] hashFromTor = torrentInfo.piece_hashes[index].array();

        return Arrays.equals(sha1Piece, hashFromTor);
    }

    public void checkFileState(){
    	
    	if(fp.exists()){
    		try {
    			System.out.println("Existing download!");
    			FileInputStream in = new FileInputStream(fp);
				alreadyDownloaded = 0;
				int length;
				byte[] pieceData;
				
				while(alreadyDownloaded<numPieces){
					
						if (alreadyDownloaded==435){
							length = fileLength%pieceLength;
						} else {
							length = pieceLength;
						}
						
						pieceData = new byte[length];
						in.read(pieceData);
						if (!verifyPiece(pieceData,alreadyDownloaded)){ 
							in.close(); 
							return;
						}
						System.out.println("Verified SHA1 hash of piece at index = "+alreadyDownloaded+" = "+verifyPiece(pieceData,alreadyDownloaded));
						tracker.downloaded += length;
						tracker.left -= length;
						System.arraycopy(pieceData, 0, fileOut, alreadyDownloaded*length, pieceData.length);
						pieceDownloaded[alreadyDownloaded] = true;
						alreadyDownloaded++;
					}
				
				in.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
    	} else{
    		System.out.println("This is a new download.");
    	}
    	
    	
    }
    
    public void updateSaveFile(byte[] piece, int index){
    	
    	try {
			FileOutputStream out = new FileOutputStream(fp, true);
	    	System.arraycopy(piece, 0, fileOut, index*pieceLength, piece.length);
	    	tracker.downloaded += piece.length;
	    	tracker.left -= piece.length;
	    	out.write(piece);
	    	out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }
    
    private int findPieceToDownload(){
    	
    	for (int k = 0; k<numPieces; k++){
    		if(pieceDownloaded[k] == false){
    			pieceDownloaded[k] = true;
    			return k;
    		}
    	}
    	return -1;
    }
    
    private byte[] downloadPiece(Peer peer, int pieceIndex){
    	byte[] piece;
    	pieceLength = torrentInfo.piece_length;
        fileLength = torrentInfo.file_length;
        int currentPieceLength;
        int numBlocks;
        int blockLength = 16384;
        int currentBlockLength;
        int length;
        int response_id;

        if (pieceIndex == numPieces - 1)
        {
            if (fileLength % pieceLength == 0)
            {
                currentPieceLength = pieceLength;
            }
            else
            {
                currentPieceLength = fileLength % pieceLength;
            }
        }
        else
        {
            currentPieceLength = pieceLength;
        }

        piece = new byte[currentPieceLength];

        numBlocks = (int)Math.ceil((double)currentPieceLength / (double)blockLength);

        for (int j = 0; j < numBlocks; j++)
        {
            // Calculate block length if last block
            if (j == numBlocks - 1)
            {
                if (currentPieceLength % blockLength == 0)
                {
                    currentBlockLength = blockLength;
                }
                else
                {
                    currentBlockLength = currentPieceLength % blockLength;
                }
            }
            else
            {
                currentBlockLength = blockLength;
            }

            // Create request message
            Message request = new Message((byte) 6, 13, -1, "-1".getBytes(), pieceIndex, j * blockLength, currentBlockLength, -1, -1, "-1".getBytes());

            try {
            // Send request message to peer
            peer.out.write(request.message);
            peer.out.flush();

            length = peer.in.readInt() - 9;
            response_id = (int) peer.in.readByte();
            int index = peer.in.readInt();
            int begin = peer.in.readInt();

            // Copy block into piece byte array
            if ((response_id == 7) && (index == pieceIndex))
            {
                byte[] block = new byte[length];
                peer.in.readFully(block);
                System.arraycopy(block, 0, piece, begin, length);
            }
            }
            catch (Exception e){
            	System.out.println(e);
            }
        }
    	
    	return piece;
    	
    }
    
    private void saveCompletedFileToDisk(String fileName){
    	
    	 try{
             FileOutputStream fileOutStream = new FileOutputStream(new File(fileName));
             fileOutStream.write(fileOut);
             fileOutStream.close();
         } catch (Exception e){
             System.out.println("Error writing file to hard disk. "+e);
         }
    	
    }
    
    private int byteArrToInt(byte[] byteArr){
    	return ByteBuffer.wrap(byteArr).getInt();
    }
    
	/**
	 * 
	 * Helper method to represent integers as 4-byte big-endian arrays.
	 * 
	 * BigInteger.valueof(i).toByteArray() will create a byte array of smallest 
	 * possible size to represent i. 
	 * 
	 * @param i - integer which we wish to convert to 4-byte big-endian
	 * @return byte array representing integer
	 * 
	 */
	private byte[] intToByteArr(int i){
		
		byte[] byteArray = {(byte)(i >>> 24), (byte)(i >>> 16), (byte) (i >>> 8), (byte)(i)};
		return byteArray;
		
	}
	
	
}
