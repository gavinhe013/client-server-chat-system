package chatserver;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class ServerMessage {

    JSONObject jsonFormatMsg;

    /**
     * Server sends new identity change message to client when client wants change ID or in the first time setup
     * @param formerId former user ID
     * @param newId new user ID
     * @return JSON format encoded text
     */
    public String newId(String formerId, String newId) {
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "newidentity");
        jsonFormatMsg.put("former", formerId);
        jsonFormatMsg.put("identity", newId);
        return jsonFormatMsg.toString();
    }

    /**
     * Server sends room contents information to client
     * @param roomId specific room identity
     * @param owner room owner name
     * @param IDs all users' identity who are currently sitting in this room
     * @return JSON format encoded text
     */
    public String roomContents(String roomId, String owner, String[] IDs) {
        jsonFormatMsg = new JSONObject();
        JSONArray jsonUserIDs = new JSONArray();
        for (String id: IDs) {
            jsonUserIDs.add(id);
        }
        jsonFormatMsg.put("type", "roomcontents");
        jsonFormatMsg.put("roomid", roomId);
        jsonFormatMsg.put("identities", jsonUserIDs);
        jsonFormatMsg.put("owner", owner);
        return jsonFormatMsg.toString();
    }

    /**
     * Server sends room change information to client
     * @param userId user who request room change
     * @param formerRoomId user's former chat room name
     * @param newRoomId user's new chat room name
     * @return JSON format encoded text
     */
    public String roomChange(String userId, String formerRoomId, String newRoomId) {
        jsonFormatMsg = new JSONObject();
        jsonFormatMsg.put("type", "roomchange");
        jsonFormatMsg.put("identity", userId);
        jsonFormatMsg.put("former", formerRoomId);
        jsonFormatMsg.put("roomid", newRoomId);
        return jsonFormatMsg.toJSONString();
    }

    /**
     * Server send room list message to client
     * @param rooms room list and client counts
     * @param msg message or dealing with error
     * @return
     */
    public String roomList(ArrayList<JSONObject> rooms, String msg) {
        jsonFormatMsg = new JSONObject();
        JSONArray list = new JSONArray();
        for (JSONObject room : rooms) {
            list.add(room);
        }
        jsonFormatMsg.put("type", "roomlist");
        jsonFormatMsg.put("rooms", list);
        // for dealing some error.
        jsonFormatMsg.put("words", msg);
        return jsonFormatMsg.toString();
    }


}
