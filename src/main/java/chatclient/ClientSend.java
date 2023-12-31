package chatclient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientSend implements Runnable {

    protected Socket socket;

    /**
     * Constructor method for ClientSend
     *
     * @param socket
     */
    public ClientSend(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Scanner keyboard = new Scanner(System.in);
            String messageToServer;

            //Set up the identity (auto generated by server) when connection is established
            //send this sign up request to the server
            String signup = new ClientMessage().requestNewID("");
            out.writeUTF(signup);
            out.flush();

            //Send a request to join the MainHall (automatically) when connection is established
            // Initially, user is assigned to MainHall
            String allocateRoom = new ClientMessage().requestJoin("MainHall");
            out.writeUTF(allocateRoom);
            out.flush();

            //When the user is connected to the server and join the MainHall, show current MainHall's information
            String mainHallContents = new ClientMessage().requestRoomContents("MainHall");
            out.writeUTF(mainHallContents);
            out.flush();

            while (true) {

                messageToServer = keyboard.nextLine();
                /*
                Check the user input message is a command to request function from server
                , or it is just a line of plain message for chat
                 */
                char initialChar = messageToServer.charAt(0);
                // if the first character of the user input message is '#', then this message is a command
                if (initialChar == '#') {
                    String[] strTokens = messageToServer.split(" ");
                    //get the user's request
                    String request = strTokens[0];
                    switch (request) {

                        case "#identitychange":
                            try {
                                //get new user ID
                                String newId = strTokens[1];
                                if (!newId.equals(null)) {

                                    ClientMessage clientMessage1 = new ClientMessage();
                                    //covert the request into JSON format encoded text, then send it to server
                                    String reqNewUserId = clientMessage1.requestNewID(newId);
                                    out.writeUTF(reqNewUserId);
                                    out.flush();

                                }
                            } catch (Exception e) {
                                System.out.println("Empty name is not allowed.");
                            }
                            break;

                        case "#createroom":
                            try {
                                String newRoomId = strTokens[1];
                                if (newRoomId != null) {

                                    ClientMessage clientMessage2 = new ClientMessage();
                                    //covert the request into JSON format encoded text, then send it to server
                                    String reqCreateRoom = clientMessage2.requestCreate(newRoomId);
                                    out.writeUTF(reqCreateRoom);
                                    out.flush();

                                }

                            } catch (Exception e) {
                                System.out.println("Please enter a room name.");
                            }
                            break;

                        case "#join":
                            try {
                                String roomId = strTokens[1];
                                if (!roomId.equals(null)) {
                                    ClientMessage clientMessage3 = new ClientMessage();
                                    //covert the request into JSON format encoded text, then send it to server
                                    String reqJoin = clientMessage3.requestJoin(roomId);
                                    out.writeUTF(reqJoin);
                                    out.flush();
                                }
                            } catch (Exception e) {
                                System.out.println("Please enter the room name to join in.");
                            }
                            break;

                        case "#delete":
                            try {
                                String roomId = strTokens[1];
                                if (!roomId.equals(null)) {
                                    ClientMessage clientMessage4 = new ClientMessage();
                                    //covert the request into JSON format encoded text, then send it to server
                                    String reqDelete = clientMessage4.requestDelete(roomId);
                                    out.writeUTF(reqDelete);
                                    out.flush();
                                }
                            } catch (Exception e) {
                                System.out.println("Please enter the room name to delete.");
                            }
                            break;

                        case "#who":
                            try {
                                String roomId = strTokens[1];
                                if (!roomId.equals(null)) {

                                    ClientMessage clientMessage5 = new ClientMessage();
                                    //covert the request into JSON format encoded text, then send it to server
                                    String reqContents = clientMessage5.requestRoomContents(roomId);
                                    out.writeUTF(reqContents);
                                    out.flush();

                                }
                            } catch (Exception e) {
                                System.out.println("Please enter the room name to check room contents.");
                            }
                            break;

                        case "#list":
                            ClientMessage clientMessage6 = new ClientMessage();
                            //covert the request into JSON format encoded text, then send it to server
                            String reqList = clientMessage6.requestRoomList();
                            out.writeUTF(reqList);
                            out.flush();
                            break;

                        case "#quit":
                            ClientMessage clientMessage7 = new ClientMessage();
                            //covert the request into JSON format encoded text, then send it to server
                            String reqQuit = clientMessage7.requestQuit();
                            out.writeUTF(reqQuit);
                            out.flush();
                            break;

                        default:
                            System.out.println("Wrong command");
                            break;

                    }
                } else {
                    //send normal chat messages rather than a command or request
                    ClientMessage clientMessage8 = new ClientMessage();
                    //covert the chat message into JSON format encoded text, then send it to server
                    String chatMsg = clientMessage8.plainMessage(messageToServer);
                    out.writeUTF(chatMsg);
                    out.flush();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

