import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

import java.io.InterruptedIOException;

public class Listener_User implements Runnable{
    private NewUser user;
    private SpreadConnection conn;
    private boolean shutdown;

    public Listener_User(NewUser u, SpreadConnection conn){
        this.user = u;
        this.conn = conn;
    }

    @Override
    public void run() {
        while(!shutdown){

            try {
                SpreadMessage spread_msg = this.conn.receive();
                this.user.message_process(spread_msg);
            }
            catch (SpreadException | InterruptedIOException e){
                e.printStackTrace();
            }

        }
    }

    public void shutdown() {
        shutdown = true;
    }


}