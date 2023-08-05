package chatserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {

    private String roomID; // room identity
    private String owner;  // room owner's identity
    // using thread safe list to record all users' identity and connection thread in the room
    private List<String> roomMembers = Collections.synchronizedList(new ArrayList<String>());
    private List<Connection> memberThreads = Collections.synchronizedList(new ArrayList<Connection>());

    /**
     * Chat room constructor method
     * @param roomID
     */
    public Room(String roomID){
        this.roomID = roomID;
    }

    /**
     * Accessor method for getting chatroom's identity
     * @return chat room name
     */
    public String getRoomID() {
        return roomID;
    }

    /**
     * Mutator method for setting chatroom's owner
     * @param owner user ID who is going to be set as chat room owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Accessor method for getting chatroom's owner
     * @return the owner of the chat room
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Accessor method for getting all members (or users) in this chatroom
     * @return a list of all members' identities
     */
    public List<String> getRoomMembersList() {
        return roomMembers;
    }

    /**
     * Getting all users' identities in a string array form, for the ease of dealing a list of users when writing JSON
     * @return a String type of array contains all users identities in this chatroom
     */
    public String[] getRoomMembersIDs() {
        int membersAmount = roomMembers.size();
        String[] emptyArr = new String[membersAmount];
        //put the member identities from List into a same size String type array
        String[] members = roomMembers.toArray(emptyArr);
        return members;
    }

    /**
     * Accessor method for getting all connected threads in this room
     * @return a list of connection threads in this chat room
     */
    public List<Connection> getMemberThreads() {
        return memberThreads;
    }

    /**
     * Method for adding member (or user) in this room
     * @param conn user's connection thread
     */
    public void addMemberThread(Connection conn) {
        memberThreads.add(conn);
    }

    /**
     * Method for removing specific user's thread from this chatroom
     * @param userID specific user need to be removed from chat room
     */
    public void removeMemberThread(String userID){
        roomMembers.remove(roomMembers.indexOf(userID));
        for (int i = 0; i < memberThreads.size(); i++) {
            if (memberThreads.get(i).getUserId().equals(userID)) {
                memberThreads.remove(i);
            }
        }

    }

    /**
     * Send message to all members in this chatroom
     * @param msg message need to be broadcast
     */
    public void broadcastWithinRoom(String msg) throws IOException {
        for (Connection c: memberThreads) {
            ServerSend ss = new ServerSend(c.getOutput(), msg);
            Thread send = new Thread(ss);
            send.start();
        }
    }

}
