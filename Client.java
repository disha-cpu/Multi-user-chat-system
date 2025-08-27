package socket.Chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    
    private Socket socket;
    private BufferedReader bReader;
    private BufferedWriter bWriter;
    private String Username;

    public Client(Socket socket, String Username){
        try{
            this.socket = socket;
            this.bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.Username = Username;
        }catch(IOException e){
            closeEverything(socket, bReader, bWriter);
        }
    }

    public void sendMessage(){
        try{
            bWriter.write(Username);
            bWriter.newLine();
            bWriter.flush();

            Scanner sc = new Scanner(System.in);
            while (socket.isConnected()) {
                String message = sc.nextLine();
                // bWriter.write(Username+": "+message);
                bWriter.write(message);
                bWriter.newLine();
                bWriter.flush();
            }
        }catch (Exception e){
           System.out.println("Byeeeeeee");
        }
    }

    public void listenForMessage(){
        new Thread(new Runnable() {
            public void run(){
                String messageFromGroupChat;
                while(socket.isConnected()){
                    try{
                        messageFromGroupChat = bReader.readLine();
                        System.out.println(messageFromGroupChat);
                    }catch(IOException e){
                        closeEverything(socket, bReader, bWriter);
                    }
                }
                
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader bReader, BufferedWriter bWriter){
        try{
            if(bReader!=null){
                bReader.close();
            }
            if(bWriter!=null){
                bWriter.close();
            }
            if(socket!=null){
                socket.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your username for the group chat");
        String Username = sc.nextLine();
        Socket socket = new Socket("localhost", 9086);
        Client client = new Client(socket, Username);
        client.listenForMessage();
        client.sendMessage();
    }

}
