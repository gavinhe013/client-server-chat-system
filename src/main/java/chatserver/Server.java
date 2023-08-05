package chatserver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.kohsuke.args4j.Option;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {

    @Parameter(names = "-p", description = "port address")
    private static int port = 4444;

    // record how many users has been connected to the server (both online and disconnected)
    // used for generating default user ID
    protected static Integer accumulatedUserAmount = 0;

    // using thread safe list to record all user identities and connection threads
    protected static List<String> userIdentities = Collections.synchronizedList(new ArrayList<>());
    protected static List<Connection> userThreads = Collections.synchronizedList(new ArrayList<>());

    // using thread safe list to record all room objects within the server
    protected static List<Room> rooms = Collections.synchronizedList(new ArrayList<>());


    /**
     * Method for closing all remaining connected threads
     * @throws InterruptedException
     */
    private static void closeAll() throws InterruptedException {
        for (Connection c : userThreads) {
            c.join();
        }
    }

    /**
     * Method for broadcasting message to all connected users
     * @param message String message need to be broadcast
     * @throws IOException
     */
    public static void broadcastToAll(String message) throws IOException {
        for (Connection c : userThreads) {
            DataOutputStream out = c.getOutput();
            Thread messageSender = new Thread(new ServerSend(out, message));
            messageSender.start();
        }
    }

    /**
     * Method to get the chatroom object given the room identity
     * @param roomId room identity
     * @return the room object
     */
    public static Room getRoom(String roomId) {
        for (Room room : rooms) {
            if (room.getRoomID().equals(roomId)) {
                return room;
            }
        }
        return null;
    }

    /**
     * The execution part of creating a room in the server side
     * @param roomId room identity
     * @param owner owner identity
     */
    public static void createRoom(String roomId, String owner) {
        synchronized (rooms) {
            Room newRoom = new Room(roomId);
            newRoom.setOwner(owner);
            rooms.add(newRoom);
        }
    }



    public static void main(String[] args) throws IOException, InterruptedException {
        //
        Server s = new Server();
        String[] argv = args;
        JCommander jCommander = new JCommander(s, argv);
        //
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is listening..." + " port address：" + port);

            Room mainHall = new Room("MainHall");
            rooms.add(mainHall);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connection is established.");

                accumulatedUserAmount++;
                int num = accumulatedUserAmount;

                String newUser = "guest" + num;
                userIdentities.add(newUser);

                Connection client = new Connection(socket, newUser);
                userThreads.add(client);
                client.start();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
                closeAll();
            }
        }
    }

}
