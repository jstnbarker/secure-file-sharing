// Server.java
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Server {
    // Declare necessary variables
    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private String directoryPath;

    // Server constructor
    public Server(int port, String directoryPath) {
        this.directoryPath = directoryPath;
        try {
            // Create server socket
            server = new ServerSocket(port);
            System.out.println("Server started");

            // Wait for client to connect
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    // Method to listen for client messages
    public void listen() throws IOException {
        String line = "";
        while (true){
			try {
				System.out.println("Waiting for a client ...");
				socket = server.accept();
				System.out.println("Client accepted");

				// Create data streams for communication
				in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				out = new DataOutputStream(socket.getOutputStream());
                try {
                    // Read client message
                    line = in.readUTF();

                    // If client requests list of files
                    if (line.equals("list")) {
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
                        // If client requests a specific file
                        File file = new File(directoryPath + "/" + line);
                        if (file.exists()) {
                            // Send file to client
                            byte[] buffer = new byte[4096];
                            FileInputStream fis = new FileInputStream(file);
                            out.writeLong(file.length());
                            while (fis.read(buffer) > 0) {
                                out.write(buffer);
                            }
                            fis.close();
                        } else {
                            // If file doesn't exist, receive file from client
                            out.writeLong(-1);
                            FileOutputStream fos = new FileOutputStream(directoryPath + "/" + line);
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
        Server server = new Server(5000, "/home/jstn/server/");
        try {
            server.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
