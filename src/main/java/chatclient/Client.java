package chatclient;

import chatserver.Server;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.kohsuke.args4j.Option;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;


public class Client {

    private static String host; // client ip address

    @Parameter(names = "-p", description = "port address", required = true)
    private static int port = 4444;

    private static String userId; //the client (or user) identity

    private static String currRoom; //the client is in which chatroom

    private static int trigger = 0; // when the trigger exceeds 2, it will start to show the prefix for each client

    /**
     * Method for receive the response of new user ID request from server
     *
     * @param jsonReceive received json format message from server
     */
    private static void newIdResponse(JSONObject jsonReceive) {
        // when current user ID is null (first time connection and auto sign up),
        // set the returned new user ID as user ID
        if (userId == null) {
            userId = jsonReceive.get("identity").toString();
            System.out.print("Connected to " + host + " as " + userId);
        }
        // when the new user ID is same as former or current user ID (when request for user ID change), change failed
        else if (jsonReceive.get("identity").equals(jsonReceive.get("former"))) {
            System.out.print("\nRequested identity invalid or in use");
        } else {
            if (jsonReceive.get("former").equals(userId)) {
                // when former user ID is different from new user ID, set the new user ID as user ID

                userId = jsonReceive.get("identity").toString();
            }
            String formerId = jsonReceive.get("former").toString();
            System.out.print("\n" + formerId + " is now " + jsonReceive.get("identity").toString());

        }
    }

    /**
     * Method for receive the response of room list request from server
     *
     * @param jsonReceive received json format message from server
     */
    private static void listResponse(JSONObject jsonReceive) {
        String words = jsonReceive.get("words").toString();
        if (!words.equals("")) {
            System.out.print("\n" + words);
            return;
        }

        JSONArray roomList = (JSONArray) jsonReceive.get("rooms");
        for (int i = 0; i < roomList.size(); i++) {
            // get each room's information from the returned list
            JSONObject room = (JSONObject) roomList.get(i);
            // print each room's information (state)
            System.out.print("\n" + room.get("roomid").toString());
            System.out.print(": ");
            if (room.get("count").toString().equals("1")) {
                System.out.print(room.get("count").toString() + " guest");
            } else {
                System.out.print(room.get("count").toString() + " guests");
            }
        }
    }

    /**
     * Method for receive the response of room contents request from server
     *
     * @param jsonReceive received json format message from server
     */
    private static void contentsResponse(JSONObject jsonReceive) {
        String roomName = jsonReceive.get("roomid").toString();
        JSONArray roomMembers = (JSONArray) jsonReceive.get("identities");
        // using an ArrayList to record the room members who are recorded in the JSON message
        ArrayList<String> members = new ArrayList<>();
        for (int i = 0; i < roomMembers.size(); i++) {
            members.add(roomMembers.get(i).toString());
        }
        // MainHall doesn't have owner, simply print all members' user ID
        if (roomName.equals("MainHall")) {
            System.out.print("\n" + roomName + " contains");
            for (int i = 0; i < members.size(); i++) {
                System.out.print(" " + members.get(i));
            }
//            System.out.println();
        } else {
            // The rooms except MainHall have ownership, print both members' user ID and owner
            System.out.print("\n" + roomName + " contains ");
            String owner = jsonReceive.get("owner").toString();
            for (int i = 0; i < members.size(); i++) {
                System.out.print(members.get(i));
                if (members.get(i).equals(owner)) {
                    System.out.print("*");
                }
                System.out.print(" ");
            }
//            System.out.println();
        }
    }

    /**
     * Method for receive the broadcast chat message from server
     *
     * @param jsonReceive received json format message from server
     */
    private static void chatResponse(JSONObject jsonReceive) {
        // get user ID
        String user = jsonReceive.get("identity").toString();
        // get user's plain chat message
        String words = jsonReceive.get("content").toString();
        System.out.print("\n" + user + ": " + words);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Socket socket = null;

        host = args[0];

        //
        Client c = new Client();
        String[] argv = new String[2];
        argv[0] = args[1];
        argv[1] = args[2];
        JCommander jCommander = new JCommander(c, argv);
        //

        try {
            socket = new Socket(host, port);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            //create a thread to send messages
            ClientSend clientSend = new ClientSend(socket);
            Thread send = new Thread(clientSend);
            send.start();

            while (true) {
                String serverToClient = in.readUTF();
                //unmarshall the received encoded text, and read it
                JSONObject jsonReceive = (JSONObject) JSONValue.parse(serverToClient);
                String type = (String) jsonReceive.get("type");
                switch (type) {

                    case "newidentity":
                        newIdResponse(jsonReceive);
                        trigger++;
                        break;

                    case "roomcontents":
                        contentsResponse(jsonReceive);
                        trigger++;
                        break;

                    case "roomlist":
                        listResponse(jsonReceive);
                        break;

                    case "message":
                        chatResponse(jsonReceive);
                        break;

                    case "roomchange":
                        String user = jsonReceive.get("identity").toString();
                        String newRoom = jsonReceive.get("roomid").toString();
                        String formerRoom = jsonReceive.get("former").toString();
                        // here need to figure out whether the user is disconnecting from the system or changing room.
                        // if user is disconnecting
                        if (newRoom.equals("") && user.equals(userId)) {

                            System.out.print("\n" + user + " leaves " + formerRoom);

                            System.out.print("\nDisconnected from " + socket.getInetAddress());
                            // close the input stream
                            in.close();
                            // close the socket to communicate
                            socket.close();
                            // wait the client sending thread to completely dead
                            send.join();
                            // disconnect
                            System.exit(0);
                        } else {
                            // if user is changing room
                            if (newRoom.equals(formerRoom)) {
                                System.out.print("\nThe requested room is invalid or non existent.");
                            } else if (!formerRoom.equals("")) {
                                System.out.print("\n" + user + " moves from " + formerRoom + " to " + newRoom);
                            } else {
                                System.out.print("\n" + user + " moves to " + newRoom);
                            }
                            if (user.equals(userId)) {
                                currRoom = newRoom;
                                trigger++;
                            }
                        }
                        break;
                }
                if (trigger > 2) {
                    System.out.print("\n" + "[" + currRoom + "] " + userId + "> ");
                }

            }
        } catch (IOException e) {
            System.out.print("\nFail to connect to server");
            if (socket != null) {
                socket.close();
            }
        }
    }
}
