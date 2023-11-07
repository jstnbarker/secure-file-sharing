// Server.java
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.*;
import java.security.spec.KeySpec;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.cert.CertificateException;
import java.io.IOException;
import java.security.UnrecoverableKeyException;
import java.security.KeyManagementException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Server {
    // Declare necessary variables
    private SSLSocket socket = null;
    private SSLServerSocket server = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private String directoryPath;
    private LinkedList<String[]> files;
    private String sessionHash = "";
    private byte[] sessionSalt = new byte[16];

    // Server constructor
    public Server(int port, String directoryPath, String sessionPassword) {
        SecureRandom random = new SecureRandom();
        random.nextBytes(sessionSalt);
        sessionHash = hash("test");

        this.directoryPath = directoryPath;
        // Initialize file list
        files = new LinkedList<String[]>();

        for (File file : new File(directoryPath).listFiles()) {
            if (file.isFile()) {
                String[] temp = {file.getName(), ""};
                files.add(temp);
            }
        }

        System.out.println("Found existing files:");
        ListIterator<String[]> fileIterator = files.listIterator();
        while(fileIterator.hasNext()){
            String[] temp = fileIterator.next();
            System.out.println("\tFile: " + temp[0]);
        }
        
        // Initialize SSL socket
        try {
           // Load the keystore
            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new FileInputStream("keystore.jks"), "password".toCharArray());

                // Set up the key manager factory
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, "password".toCharArray());

                // Set up the trust manager factory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);

                // Set up the SSL context
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
                 
                // Create SSLServerSocket
                 SSLServerSocketFactory ssf = sc.getServerSocketFactory();
                server = (SSLServerSocket) ssf.createServerSocket(port);
            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException e) {
                System.out.println(e);
            }

            System.out.println("Server started");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String hash(String plaintext){
        KeySpec spec = new PBEKeySpec(plaintext.toCharArray(), sessionSalt, 65536, 128);
        try{
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = f.generateSecret(spec).getEncoded();
            Base64.Encoder enc = Base64.getEncoder();
            System.out.println("\tSession salt: " + enc.encodeToString(sessionSalt));
            System.out.println("\tSession hash: " + enc.encodeToString(hash));
            return enc.encodeToString(hash);
        } catch(NoSuchAlgorithmException | InvalidKeySpecException e){
            System.out.println(e);
        }
        return "1";
    }

    public void sendList() throws IOException, FileNotFoundException {
        System.out.println("\tSending file list");
        File[] listOfFiles =  new File(directoryPath).listFiles();

        ListIterator<String[]> fileIterator = files.listIterator();
        while(fileIterator.hasNext()){
            out.writeUTF(fileIterator.next()[0]);
        }
        out.writeUTF("end");
    }

    private boolean verifyAvailable(File target){
        ListIterator<String[]> fileIterator = files.listIterator();
        while(fileIterator.hasNext()){
            if(fileIterator.next()[0].equals(target.getName())) return true;
        }
        return false;
    }

    public void sendFile(File target) throws IOException, FileNotFoundException {
        if (verifyAvailable(target)) {
            out.writeUTF("allow");
            // Send file to client
            byte[] buffer = new byte[4096];
            FileInputStream fis = new FileInputStream(target.getPath());
            out.writeLong(target.length());
            while (fis.read(buffer) > 0) {
                out.write(buffer);
            }
            fis.close();
            System.out.println("\tSent " + target.getPath());
        }
        else{
            System.out.println("\tRejected request for " + target.getPath());
            out.writeUTF("deny");
        }
    }

    public void recvFile(File target) throws IOException, FileNotFoundException {
        out.writeLong(-1);
        FileOutputStream fos = new FileOutputStream(target.getPath());
        byte[] buffer = new byte[4096];
        int filesize = (int) in.readLong(); // Read file size.
        int read = 0;
        int totalRead = 0;
        int remaining = filesize;
        while((read = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            fos.write(buffer, 0, read);
        }
        fos.close();
    }

    // Method to listen for client messages
    public void listen() throws IOException {
        while (true){
            System.out.println("Waiting for a client ...");
            socket = (SSLSocket) server.accept();
            System.out.println("Client accepted");

            // Create data streams for communication
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());

            try{
            int option = Integer.valueOf(in.readUTF());
                switch(option) {
                    case 0:
                        sendList();
                        break;
                    case 1:
                        sendFile(new File(directoryPath + "/" + in.readUTF()));
                        break;
                    case 2:
                        recvFile(new File(directoryPath + "/" + in.readUTF()));
                        break;
                }

            } catch (Exception e){
                System.out.println(e);
            }
        }
    }
}
