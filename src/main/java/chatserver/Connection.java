package chatserver;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Connection extends Thread {

    protected String userId; // user identity
    protected Socket socket; // socket for communication
    //using a thread safe data structure to build a buffer space or message queue to receive messages from clint side
    protected ConcurrentLinkedQueue<String> bufferSpace = new ConcurrentLinkedQueue<String>();
    protected Room locatedRoom; // current located room

    /**
     * Constructor method for Connection
     *
     * @param socket   socket for communication
     * @param identity user identity
     */
    public Connection(Socket socket, String identity) {
        this.userId = identity;
        this.socket = socket;
    }

    /**
     * Accessor method for getting user identity
     *
     * @return user identity
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Method for getting the standard output stream
     *
     * @return data output stream
     * @throws IOException
     */
    public DataOutputStream getOutput() throws IOException {
        return new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Method for checking whether the user input of user ID satisfies the system requirements or not
     *
     * @param inputUserId the new user ID by keyboard input
     * @return true means the new ID is legal, false means the new ID is illegal
     */
    private boolean isLegalUserId(String inputUserId) {
        //user ID should be in the length range of 3 to 16 characters
        boolean lengthIsCorrect = inputUserId.length() <= 16 && inputUserId.length() >= 3;
        //user ID should start with a non digit character
        char firstChar = inputUserId.charAt(0);
        boolean notStartWithDigit = !Character.isDigit(firstChar);
        //user ID should be an alphanumeric string
        boolean isLegalContent = inputUserId.matches("[a-zA-Z0-9]+");

        if (lengthIsCorrect && notStartWithDigit && isLegalContent) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method for checking whether the user input of room ID satisfies the system requirements or not
     *
     * @param inputRoomId the room ID by keyboard input
     * @return true means the room ID is legal, false means the room Id is illegal
     */
    private boolean isLegalRoomId(String inputRoomId) {
        //room ID should be in the length range of 3 to 32 characters
        boolean lengthIsCorrect = inputRoomId.length() <= 32 && inputRoomId.length() >= 3;
        //room ID should start with a non digit character
        char firstChar = inputRoomId.charAt(0);
        boolean notStartWithDigit = !Character.isDigit(firstChar);
        //room ID should be an alphanumeric string
        boolean isLegalContent = inputRoomId.matches("[a-zA-Z0-9]+");

        if (lengthIsCorrect && notStartWithDigit && isLegalContent) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method to judge whether the new user identity has been used
     *
     * @param newUserId new user identity
     * @return true means this user identity has been used, false means not
     */
    private boolean duplicateUserName(String newUserId) {
        for (int i = 0; i < Server.userIdentities.size(); i++) {
            if (Server.userIdentities.get(i).contains(newUserId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method for getting a list of room name and room size paris
     *
     * @return ArrayList of JSON format object
     */
    private ArrayList<JSONObject> getRoomlistSizePairs() {
        ArrayList<JSONObject> roomsWithCount = new ArrayList<>();
        for (Room room : Server.rooms) {
            JSONObject roomWithCount = new JSONObject();
            roomWithCount.put("roomid", room.getRoomID());
            roomWithCount.put("count", room.getRoomMembersList().size());
            roomsWithCount.add(roomWithCount);
        }
        return roomsWithCount;
    }

    /**
     * Method for dealing the request for joining a chatroom
     *
     * @param roomId room identity that user want to join in
     * @throws IOException
     */
    private void userJoin(String roomId) throws IOException {

        if (roomId.equals("MainHall")) {

            if (!isInRoom(userId, roomId)) {

                if (locatedRoom != null) {
                    removeInFormerRoom(locatedRoom.getRoomID(), userId);
                    deleteRoomIfOwnerLeave(locatedRoom);
                }

                Room mainHall = Server.rooms.get(0);
                locatedRoom = mainHall;
                locatedRoom.getMemberThreads().add(Connection.this);
                locatedRoom.getRoomMembersList().add(Connection.this.getUserId());

                // Send room change message to all in the room
                String response = new ServerMessage().roomChange
                        (mainHall.getRoomMembersList().get(mainHall.getRoomMembersList().size() - 1), "", "MainHall");
                Server.rooms.get(0).broadcastWithinRoom(response);

                String msg = "";
                String roomList = new ServerMessage().roomList(getRoomlistSizePairs(), msg);
                getOutput().writeUTF(roomList);
                getOutput().flush();
            }

        } else {
            // if room identity is valid
            if (isLegalRoomId(roomId)) {
                List<Room> rooms = Server.rooms;
                for (int i = 0; i < rooms.size(); i++) {
                    Room room = rooms.get(i);
                    // if roomId existed, (has been used)
                    if (room.getRoomID().equals(roomId)) {
                        if (!roomId.equals(locatedRoom.getRoomID())) {
                            joinRoom(roomId);
                            return;
                        }
                    }
                }
                joinRoom(locatedRoom.getRoomID());
            }
            // if it is invalid, no change, new room Id is former Id
            else {
                joinRoom(locatedRoom.getRoomID());
            }

        }
    }

    /**
     * The execution part of changing a chatroom
     *
     * @param roomId room identity
     * @throws IOException
     */
    private void joinRoom(String roomId) throws IOException {
        Room room = Server.getRoom(roomId);
        if (room != null) {
            if (!room.getRoomID().equals(locatedRoom.getRoomID())) {
                // broadcasting the room change messages
                String roomChangeMsg = new ServerMessage()
                        .roomChange(userId, locatedRoom.getRoomID(), roomId);

                locatedRoom.broadcastWithinRoom(roomChangeMsg);
                room.broadcastWithinRoom(roomChangeMsg);

                // remove the connection thread in the previous room
                locatedRoom.removeMemberThread(userId);

                // record the usr with the new room
                locatedRoom = room;

                // add the user identity and connection thread to the new room
                locatedRoom.getRoomMembersList().add(userId);
                locatedRoom.getMemberThreads().add(Connection.this);
            } else {
                String roomChangeMsg = new ServerMessage()
                        .roomChange(userId, locatedRoom.getRoomID(), roomId);
                getOutput().writeUTF(roomChangeMsg);
                getOutput().flush();
            }
        }
    }

    /**
     * Method for remove the user in the previous room
     *
     * @param roomId room identity
     * @param userId user identity
     */
    private void removeInFormerRoom(String roomId, String userId) {
        if (roomId != null && !roomId.equals("MainHall")) {
            Room previousRoom = Server.getRoom(roomId);
            previousRoom.removeMemberThread(userId);
        }
    }

    /**
     * Method to judge whether the user is already in a specific room
     *
     * @param userId user identity
     * @param roomId room identity
     * @return true means the user is already in, false means not
     */
    private boolean isInRoom(String userId, String roomId) {
        Room room = Server.getRoom(roomId);
        if (room != null) {
            for (String client : room.getRoomMembersList()) {
                if (client.equals(userId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method for dealing the users' request of creating a chatroom
     *
     * @param newRoomId new room identity
     * @throws IOException
     */
    private void createRoom(String newRoomId) throws IOException {
        boolean roomNameInUseOrInvalid = false;
        for (Room room : Server.rooms) {
            if (room.getRoomID().equals(newRoomId) || !isLegalRoomId(newRoomId)) {
                roomNameInUseOrInvalid = true;
            }
        }
        if (roomNameInUseOrInvalid) {
            // room is in use
            ArrayList<JSONObject> roomsResponse = getRoomlistSizePairs();
            String msg = "Room " + newRoomId + " is invalid or already in use.";
            String roomListResponse = new ServerMessage().roomList(roomsResponse, msg);
            getOutput().writeUTF(roomListResponse);
            getOutput().flush();
        } else {
            // create a new room
            Server.createRoom(newRoomId, userId);
            ArrayList<JSONObject> roomsResponse = getRoomlistSizePairs();
            String msg = "Room " + newRoomId + " created.";
            String roomListResponse = new ServerMessage().roomList(roomsResponse, msg);
            getOutput().writeUTF(roomListResponse);
            getOutput().flush();
        }
    }

    /**
     * Method for dealing the user identity change request
     *
     * @param newIdentity    user's requested new identity
     * @param formerIdentity user's former identity
     * @throws IOException
     */
    private void identityChange(String newIdentity, String formerIdentity) throws IOException {
        // first time set up
        if (newIdentity.equals("")) {
            String firstIdResponse = new ServerMessage().newId("", userId);
            getOutput().writeUTF(firstIdResponse);
            getOutput().flush();
        } else {
            // check whether it is a valid name or not used yet
            if (duplicateUserName(newIdentity) || !isLegalUserId(newIdentity)) {
                //has been used or invalid, no change in identity
                String noChangeResponse = new ServerMessage().newId(formerIdentity, formerIdentity);
                getOutput().writeUTF(noChangeResponse);
                getOutput().flush();
            }
            // available to make change in identity
            else {
                // update the server user list
                for (int i = 0; i < Server.userIdentities.size(); i++) {
                    if (Server.userIdentities.get(i).equals(formerIdentity)) {
                        Server.userIdentities.set(i, newIdentity);
                    }
                }
                // update chatroom member list
                for (int i = 0; i < locatedRoom.getRoomMembersList().size(); i++) {
                    if (locatedRoom.getRoomMembersList().get(i).equals(formerIdentity)) {
                        locatedRoom.getRoomMembersList().set(i, newIdentity);
                    }
                }

                // update ownership except MainHall
                for (int i = 1; i < Server.rooms.size(); i++) {
                    if (Server.rooms.get(i).getOwner().equals(formerIdentity)) {
                        Server.rooms.get(i).setOwner(newIdentity);
                    }
                }

                // broadcast identity change to all connected users
                userId = newIdentity;
                String updatedId = new ServerMessage().newId(formerIdentity, userId);
                Server.broadcastToAll(updatedId);

            }
        }
    }

    /**
     * Method for dealing the deleting chatroom request from users
     *
     * @param roomDelete the room identity which need to be deleted
     * @throws IOException
     */
    private void dealDeleteRoom(String roomDelete) throws IOException {
        Room deletedRoom = Server.getRoom(roomDelete);
        // room exists
        if (deletedRoom != null) {
            // delete request is sent by the owner, only owner can trigger delete

            // if the room that intend to be deleted is MainHall, return an error message back to the client
            if (deletedRoom.getRoomID().equals("MainHall")) {
                ArrayList<JSONObject> serverRooms = getRoomlistSizePairs();
                String msg = userId + " doesn't have authority to delete the MainHall";
                String deleteResponse = new ServerMessage().roomList(serverRooms, msg);
                getOutput().writeUTF(deleteResponse);
                getOutput().flush();
                return;
            }
            String owner = deletedRoom.getOwner();
            if (owner.equals(userId)) {
                // push all current users in this room to main hall
                forceToMainHall(roomDelete);
                // delete the chatroom
                deleteRoom(roomDelete);
                // reply a room list message only to the client deleted the room
                ArrayList<JSONObject> serverRooms = getRoomlistSizePairs();
                String deleteResponse = new ServerMessage().roomList(serverRooms, "");
                getOutput().writeUTF(deleteResponse);
                getOutput().flush();
            } else {
                // if the user is not the owner of the room, he/she doesn't have authority to delete, reply a error message
                ArrayList<JSONObject> serverRooms = getRoomlistSizePairs();
                String msg = userId + " doesn't have authority to delete the room";
                String deleteResponse = new ServerMessage().roomList(serverRooms, msg);
                getOutput().writeUTF(deleteResponse);
                getOutput().flush();

            }
        } else {
            // if the room doesn't exist, then return an error message back to the client
            ArrayList<JSONObject> serverRooms = getRoomlistSizePairs();
            String msg = userId + " is trying to delete an invalid room, please try again";
            String deleteResponse = new ServerMessage().roomList(serverRooms, msg);
            getOutput().writeUTF(deleteResponse);
            getOutput().flush();
        }
    }

    /**
     * Method for deleting the chatroom if the owner has quited the system and room doesn't have any memerbers
     *
     * @param room the chatroom object
     */
    private void deleteRoomIfOwnerLeave(Room room) {
        if (room.getRoomID().equals("MainHall")) {
            return;
        }
        if (room.getOwner().equals("") && room.getMemberThreads().size() == 0) {
            deleteRoom(room.getRoomID());
        }
    }

    /**
     * Method for move all the remaining users in the chatroom to the main hall
     *
     * @param roomId room identity
     * @throws IOException
     */
    private void forceToMainHall(String roomId) throws IOException {
        Room chatRoom = Server.getRoom(roomId);
        Room mainHall = Server.rooms.get(0);
        for (int i = 0; i < chatRoom.getMemberThreads().size(); i++) {
            Connection c = chatRoom.getMemberThreads().get(i);
            mainHall.addMemberThread(c);
            //change the room identity as well
            c.locatedRoom = mainHall;
        }
        for (int i = 0; i < chatRoom.getRoomMembersList().size(); i++) {
            String user = chatRoom.getRoomMembersList().get(i);
            mainHall.getRoomMembersList().add(user);
        }

        for (String user : chatRoom.getRoomMembersList()) {
            String roomChangeMsg = new ServerMessage().roomChange(user, chatRoom.getRoomID(), "MainHall");
            chatRoom.broadcastWithinRoom(roomChangeMsg);
        }
    }

    /**
     * The execution part of deleting a room
     *
     * @param roomId
     */
    private void deleteRoom(String roomId) {
        Room chatRoom = Server.getRoom(roomId);
        Server.rooms.remove(Server.rooms.indexOf(chatRoom));
    }

    /**
     * Method for dealing the request of checking room contents from users
     *
     * @param roomId the target chat room iedentity need to be checked
     * @throws IOException
     */
    private void who(String roomId) throws IOException {
        if (roomId.equals("MainHall")) {
            String[] users = Server.rooms.get(0).getRoomMembersList().toArray(
                    new String[Server.rooms.get(0).getRoomMembersList().size()]);
            String whoMainHallResponse = new ServerMessage().roomContents("MainHall", "", users);
            getOutput().writeUTF(whoMainHallResponse);
            getOutput().flush();
        } else {
            Room charRoom = Server.getRoom(roomId);
            if (charRoom != null) {
                String roomOwner = charRoom.getOwner();
                String[] usersInside = charRoom.getRoomMembersList().toArray(
                        new String[Server.getRoom(roomId).getRoomMembersList().size()]);
                String whoResponse = new ServerMessage().roomContents(roomId, roomOwner, usersInside);

                getOutput().writeUTF(whoResponse);
                getOutput().flush();
            }
        }
    }

    /**
     * Method for setting the owner identity to an empty string
     *
     * @param userId user identity
     */
    private void clearOwnership(String userId) {
        List<Room> rooms = Server.rooms;
        // using a while loop to refresh room list when deleted a room
        while (true) {
            for (int i = 0; i < rooms.size(); i++) {
                Room room = rooms.get(i);
                if (room.getOwner() != null) {
                    if (room.getOwner().equals(userId)) {
                        room.setOwner("");
                        deleteRoomIfOwnerLeave(room);
                        break;
                    }
                }
                if (i == rooms.size()-1) {
                    return;
                }
            }
        }

    }

    /**
     * Method for dealing the quit request from the user
     *
     * @throws IOException
     */
    private void quit() throws IOException {

        // remove the user from current chat room
        locatedRoom.removeMemberThread(userId);

        // send the room change message to the user, then it can successfully quit
        String roomChangeQuit = new ServerMessage().roomChange(userId, locatedRoom.getRoomID(), "");
        getOutput().writeUTF(roomChangeQuit);
        getOutput().flush();

        // broadcast the room change message that the user moves to an empty name room
        String roomChangeMsg = new ServerMessage().roomChange(userId, locatedRoom.getRoomID(), "");
        locatedRoom.broadcastWithinRoom(roomChangeMsg);

        // if the user owns any chatroom, the owner variable would be set to an empty string
        clearOwnership(userId);
    }


    // receive the messages from client and put them in the buffer space / message queue
    @Override
    public void run() {

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            Thread readFromQueue = new Thread(new dealRequest());
            readFromQueue.start();

            try {
                while (true) {
                    synchronized (bufferSpace) {
                        String msg = in.readUTF();
                        System.out.println(msg);
                        bufferSpace.add(msg);
                    }
                }
            } catch (EOFException e) {
                quit();
                in.close();
                out.close();
                socket.close();
                System.out.println(userId + " abruptly disconnected");
            }
        } catch (IOException e) {
            System.out.println(userId + " terminated connection");
//            e.printStackTrace();
        }

    }

    // dealing the request from client
    private class dealRequest implements Runnable {

        @Override
        public void run() {

            boolean read = true;

            while (read) {

                while (!bufferSpace.isEmpty() && read) {

                    String message = bufferSpace.poll();
                    Object obj = JSONValue.parse(message);
                    JSONObject jsonMsg = (JSONObject) obj;

                    String type = jsonMsg.get("type").toString();

                    try {

                        switch (type) {

                            case "message":
                                jsonMsg.put("identity", userId);
                                locatedRoom.broadcastWithinRoom(jsonMsg.toString());
                                break;

                            case "join":
                                String roomId = jsonMsg.get("roomid").toString();
                                userJoin(roomId);
                                break;

                            case "list":
                                ArrayList<JSONObject> roomsWithCount = getRoomlistSizePairs();
                                String msg = "";
                                ServerMessage serverMessage = new ServerMessage();
                                String response = serverMessage.roomList(roomsWithCount, msg);
                                getOutput().writeUTF(response);
                                getOutput().flush();
                                break;

                            case "createroom":
                                String newRoomId = jsonMsg.get("roomid").toString();
                                createRoom(newRoomId);
                                break;

                            case "delete":
                                String roomToDelete = jsonMsg.get("roomid").toString();
                                dealDeleteRoom(roomToDelete);
                                break;

                            case "who":
                                String roomRequested = jsonMsg.get("roomid").toString();
                                who(roomRequested);
                                break;

                            case "identitychange":
                                String newIdentityReq = jsonMsg.get("identity").toString();
                                String formerIdentity = userId;
                                identityChange(newIdentityReq, formerIdentity);
                                break;

                            case "quit":
                                quit();
                                read = false;
                                break;

                            default:
                                System.out.println("Error in reading messages from client");
                                break;
                        }
                    } catch (IOException e) {
                        System.out.println("Error in receiving messages");
                        e.printStackTrace();
                    }
                }
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error in socket closing");
                e.printStackTrace();
            }
        }
    }
}




