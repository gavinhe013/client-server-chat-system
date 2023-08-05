package chatclient;

import org.json.simple.JSONObject;

public class ClientMessage {

    JSONObject jsonFormatMsg;

    /**
     * Default Constructor method for ClientMessage
     */
    public ClientMessage(){

    }

    /**
     * Method for sending request to create a new ID or change ID
     * @param newId potential new ID
     * @return JSON format encoded text
     */
    public String requestNewID(String newId) {
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "identitychange");
        jsonFormatMsg.put("identity", newId);
        return jsonFormatMsg.toJSONString() + "\n";
    }

    /**
     * Method for sending request to create a new chatroom
     * @param newRoomId new chatroom ID
     * @return JSON format encoded text
     */
    public String requestCreate(String newRoomId){
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "createroom");
        jsonFormatMsg.put("roomid", newRoomId);
        return jsonFormatMsg.toJSONString() + "\n";
    }

    /**
     * Method for sending request to join a chatroom
     * @param roomId chatroom ID
     * @return JSON format encoded text
     */
    public String requestJoin(String roomId) {
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "join");
        jsonFormatMsg.put("roomid", roomId);
        return jsonFormatMsg.toJSONString() + "\n";
    }

    /**
     * Method for sending request to get a room list
     * @return JSON format encoded text
     */
    public String requestRoomList() {
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "list");
        return jsonFormatMsg.toJSONString() + "\n";
    }

    /**
     * Method for sending request to get a specific chatroom's contents
     * @param roomId A specific chatroom ID
     * @return JSON format encoded text
     */
    public String requestRoomContents(String roomId) {
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "who");
        jsonFormatMsg.put("roomid", roomId);
        return jsonFormatMsg.toJSONString() + "\n";
    }

    /**
     * Method for sending request to delete a chatroom
     * @param roomId A specific chatroom ID
     * @return JSON format encoded text
     */
    public String requestDelete(String roomId){
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "delete");
        jsonFormatMsg.put("roomid", roomId);
        return jsonFormatMsg.toJSONString() + "\n";
    }

    /**
     * Method for sending request to quit the system and disconnect in a normal way
     * @return JSON format encoded text
     */
    public String requestQuit() {
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "quit");
        return jsonFormatMsg.toJSONString() + "\n";
    }

    /**
     * Method for sending normal message for communicating in the chatroom rather than a request to server
     * @param msg normal chat message
     * @return JSON format encoded text
     */
    public String plainMessage(String msg){
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "message");
        jsonFormatMsg.put("content", msg);
        return jsonFormatMsg.toJSONString() + "\n";
    }





}
