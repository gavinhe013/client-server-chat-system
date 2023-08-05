package chatserver;

import java.io.DataOutputStream;
import java.io.IOException;

public class ServerSend implements Runnable {

    //outputstream for sending message
    private DataOutputStream output;
    //content of message
    private String message;

    /**
     * Constructor method for ServerSend
     * @param output outputstream
     * @param message content of message
     */
    public ServerSend(DataOutputStream output, String message) {
        this.output = output;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            //writing message into the outputstream
            output.writeUTF(message);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
