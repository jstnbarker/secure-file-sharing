import java.util.*;
import java.io.*;
import java.nio.*;

class FileShare{
	static Scanner scanner = new Scanner(System.in);

	public static void clientSession(){
		Scanner in = new Scanner(System.in);
		System.out.print("Server address: ");
		String address = in.nextLine();
		Client client = new Client();

		while(true){
			System.out.println("Choose an option:");
			System.out.println("0. List files");
			System.out.println("1. Request a file");
			System.out.println("2. Send a file");
			System.out.println("3. Exit");
			System.out.println("> ");

			int option = scanner.nextInt();
			scanner.nextLine();

			try{
				client.connect(address, 5000);
				/*
			} catch (ConnectException e) {
				System.out.println("Server offline");
				*/
			} catch (Exception e) {
				System.out.println(e);
				return;
			}

			try{
				switch(option){
					case 0:
						client.listFiles();
						break;
					case 1:
						System.out.println("Enter the name of the file you want to receive: ");
						File toReceive = new File(scanner.nextLine());
						client.requestFile(toReceive.getPath());
						break;
					case 2:
						System.out.println("Enter the name of the file you want to send:");
						File toSend = new File(scanner.nextLine());
						if(!toSend.exists()) {
							System.out.println("File does not exist");
							return;
						}
						client.sendFile(toSend.getPath());
						break;
					case 3:
						return;
				}
				} catch (IOException e) {
					System.out.println(e);
					return;
				}
			try{
				client.disconnect();
			} catch (IOException e) {
				System.out.println(e);
				return;
			}
		}
	}

	public static void serverSession(){
        System.out.print("Session password: ");
        String password = scanner.nextLine();
		System.out.print("Shared file directory: ");
		File shared = new File(scanner.nextLine());
		if(!shared.exists()){
			System.out.println("Shared directory does not exist");
			return;
		}
		Server server = new Server(5000, shared.getPath(), password);
		try{
			server.listen();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]){
		System.out.println("1. Server session\n2. Client session");
		int selection = scanner.nextInt();
		scanner.nextLine();
		switch(selection){
			case 1:
				serverSession();
				break;
			case 2:
				clientSession();
				break;
		}
	}
}
