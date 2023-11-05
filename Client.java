// Client.java

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Scanner;
import javax.net.ssl.*;


public class Client {
    private SSLSocket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;

    public Client() {
        return;
    }

    public void connect(String address, int port) throws ConnectException, IOException {
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
        SSLSocketFactory ssf = sc.getSocketFactory();
        socket = (SSLSocket) ssf.createSocket(address, port);
         // Set the SSL/TLS protocols and cipher suites
        socket.setEnabledProtocols(new String[] {"TLSv1.3"});
        socket.setEnabledCipherSuites(new String[] {"TLS_AES_128_GCM_SHA256"});
        this.input = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    } catch (Exception e) {
        e.printStackTrace();
    }

    }

    public void sendFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        
        out.writeUTF(file.getName());
        out.writeLong(file.length());
        while (fis.read(buffer) > 0) {
            out.write(buffer);
        }
        
        fis.close();
    }

    public void requestFile(String fileName) throws IOException {
        out.writeUTF(fileName);
        File file = new File("received_" + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        
        int filesize = (int) input.readLong(); // Send file size in separate msg
        int read = 0;
        int totalRead = 0;
        int remaining = filesize;
        while((read = input.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            fos.write(buffer, 0, read);
        }
        
        fos.close();
    }

    //list files on server
    public void listFiles() throws IOException {
        out.writeUTF("list");
        String fileName;
        System.out.println("\nFilelist: ");
        while (!(fileName = input.readUTF()).equals("end")) {
            System.out.println("\t" + fileName);
        }
        System.out.println("---\n");
    }

    //Disconnect from the server
    public void disconnect() throws IOException {
        out.writeUTF("Over");
        out.close();
        input.close();
        socket.close();
    }

    public void handler(int option){
        Scanner scanner = new Scanner(System.in);

        try{
            connect("127.0.0.1", 5000);
        } catch (ConnectException e){
            System.out.println("Server offline");
            return;
        } catch (IOException e){
            System.out.println(e);
            return;
        }

        switch(option){
            case 0:
                try{
                    listFiles();
                } catch (IOException e){
                    System.out.println(e);
                    return;
                }
                break;
            case 1:
                System.out.println("Enter the name of the file you want to receive:");
                String fileToReceive = scanner.nextLine();

                try{
                    requestFile(fileToReceive);
                } catch (IOException e){
                    System.out.println(e);
                    return;
                }
                break;
            case 2:
                System.out.println("Enter the name of the file you want to send:");
                String fileToSend = scanner.nextLine();
                try{
                    sendFile(fileToSend);
                } catch (IOException e){
                    System.out.println(e);
                    return;
                }
                break;
        }
        try{
            disconnect();
        } catch (IOException e){
            System.out.println(e);
            return;
        }
    }

    public static void main(String[] args) {
        while(true){
            Client client = new Client();
            Scanner scanner = new Scanner(System.in);

            System.out.println("Choose an option:");
            System.out.println("0. List files");
            System.out.println("1. Request a file");
            System.out.println("2. Send a file");
            System.out.println("3. Exit");

            System.out.print("> ");
            int option = scanner.nextInt();
            scanner.nextLine(); // consume the newline character
            
            if(option == 3){
                return;
            }
            client.handler(option);
        }
    }
}
