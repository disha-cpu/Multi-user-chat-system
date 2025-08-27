package socket.Chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    public static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static ConcurrentHashMap<String, ClientHandler> userMap = new ConcurrentHashMap<>();

    private Socket socket;
    private BufferedReader bReader;
    private BufferedWriter bWriter;
    private String Username;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.Username = bReader.readLine();
            clients.add(this);
            userMap.put(Username, this);
            broadcastMessage("SERVER: " + Username + " has joined the chat");
        } catch (IOException e) {
            closeEverything(socket, bReader, bWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bReader.readLine();

                if (messageFromClient.equals("/users")) {
                    StringBuilder usersList = new StringBuilder("Online Users: ");
                    for (String user : userMap.keySet()) {
                        usersList.append(user).append(", ");
                    }
                    bWriter.write(usersList.toString());
                    bWriter.newLine();
                    bWriter.flush();
                    continue;
                }

                if (messageFromClient.equals("/history")) {
                    List<String> lines = Files.readAllLines(Paths.get("src\\socket\\Chat\\chatlog.txt"));
                    for (int i = Math.max(0, lines.size() - 10); i < lines.size(); i++) {
                        bWriter.write(lines.get(i));
                        bWriter.newLine();
                    }
                    bWriter.flush();
                    continue;
                }

                if (messageFromClient.startsWith("@")) {
                    String[] split = messageFromClient.split(" ", 2);
                    String targetUser = split[0].substring(1);
                    String msg = split.length > 1 ? split[1] : "";
                    ClientHandler target = userMap.get(targetUser);

                    if (target != null) {
                        target.bWriter.write("(" + Username + " messaged privately ): " + msg);
                        target.bWriter.newLine();
                        target.bWriter.flush();
                    } else {
                        bWriter.write("User '" + targetUser + ", not found");
                        bWriter.newLine();
                        bWriter.flush();
                    }
                    continue;
                } 
                
                broadcastMessage(messageFromClient);

            } catch (IOException e) {
                closeEverything(socket, bReader, bWriter);
                break;
            }
        }

    }

    public void broadcastMessage(String message) {
        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter("src\\socket\\Chat\\chatlog.txt", true))) {
            logWriter.write(message);
            logWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (ClientHandler c : clients) {
            try {
                if (!Username.equals(c.Username)) {
                    // c.bWriter.write(message);
                    c.bWriter.write(Username+": "+message);
                    c.bWriter.newLine();
                    c.bWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bReader, bWriter);
            }
        }
    }

    public void removeClientHandler() {
        clients.remove(this);
        userMap.remove(Username);
        broadcastMessage("SERVER: " + Username + " has left the chat");
    }

    public void closeEverything(Socket socket, BufferedReader bReader, BufferedWriter bWriter) {
        removeClientHandler();
        try {
            if (bReader != null) {
                bReader.close();
            }
            if (bWriter != null) {
                bWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
