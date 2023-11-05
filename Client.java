// Client.java
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;

    public Client(String address, int port) {
        try {
            socket = new Socket(address, port);
            System.out.println("Connected");
            input = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            System.out.println(e);
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
        while (!(fileName = input.readUTF()).equals("end")) {
            System.out.println(fileName);
        }
    }

    //Disconnect from the server
    public void disconnect() throws IOException {
        out.close();
        input.close();
        socket.close();
    }

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", 5000);
        Scanner scanner = new Scanner(System.in);
        try {
            while(true){
                System.out.println("List of files on server:");
                client.listFiles();
                System.out.println("-----------------------------------");
                System.out.println("Choose an option:");
                System.out.println("1. Request a file");
                System.out.println("2. Send a file");
                System.out.println("3. Exit");
                int option = scanner.nextInt();
                scanner.nextLine(); // consume the newline character
                if(option == 1){
                    System.out.println("Enter the name of the file you want to receive:");
                    String fileToReceive = scanner.nextLine();
                    client.requestFile(fileToReceive);
                }
                else if(option == 2){
                    System.out.println("Enter the name of the file you want to send:");
                    String fileToSend = scanner.nextLine();
                    client.sendFile(fileToSend);
                }
                else if(option == 3){
                    client.disconnect();
                    break;
                }
                else{
                    System.out.println("Invalid option");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}