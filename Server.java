// Server.java
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.net.ssl.*;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.io.IOException;
import java.security.UnrecoverableKeyException;
import java.security.KeyManagementException;

public class Server {
    // Declare necessary variables
    private SSLSocket socket = null;
    private SSLServerSocket server = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private String directoryPath;

    // Server constructor
    public Server(int port, String directoryPath) {
        this.directoryPath = directoryPath;
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

            // Wait for client to connect
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Method to listen for client messages
    public void listen() throws IOException {
        String line = "";
        while (true){
			try {
                
				System.out.println("Waiting for a client ...");
				socket = (SSLSocket) server.accept();
				System.out.println("Client accepted");

				// Create data streams for communication
				in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				out = new DataOutputStream(socket.getOutputStream());
                try {
                    // Read client message
                    line = in.readUTF();

                    // If client requests list of files
                    if (line.equals("list")) {
                        System.out.println("\tSending file list");
                        File folder = new File(directoryPath);
                        File[] listOfFiles = folder.listFiles();
                        for (File file : listOfFiles) {
                            if (file.isFile()) {
                                // Send file name to client
                                out.writeUTF(file.getName());
                            }
                        }
                        out.writeUTF("end");
                    } else {
                        String filepath=(directoryPath + "/" + line);
                        // If client requests a specific file
                        File file = new File(filepath);
                        if (file.exists()) {
                            // Send file to client
                            byte[] buffer = new byte[4096];
                            FileInputStream fis = new FileInputStream(file);
                            out.writeLong(file.length());
                            while (fis.read(buffer) > 0) {
                                out.write(buffer);
                            }
                            System.out.println("\tSent " + filepath);
                            fis.close();
                        } else {
                            // If file doesn't exist, receive file from client
                            out.writeLong(-1);
                            FileOutputStream fos = new FileOutputStream(filepath);
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
                    }
                } catch (IOException i) {
                    System.out.println(i);
                }
			} catch (IOException i){
				System.out.println(i);
			}
        }
    }

    // Main method
    public static void main(String[] args) {
        // Create server and start listening
        Server server = new Server(5000, "C:/Users/reese/OneDrive/Documents/Classes/Coputer/Server/Data");
        try {
            server.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
