package Peer;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

import java.io.InterruptedIOException;

public class Listener implements Runnable{
    private Peer user;
    private SpreadConnection conn;
    private boolean shutdown;

    public Listener(Peer u, SpreadConnection conn){
        this.user = u;
        this.conn = conn;
    }

    @Override
    public void run() {
        while(!shutdown){

            try {
                SpreadMessage spread_msg = this.conn.receive();
                this.user.spread_recv(spread_msg);
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