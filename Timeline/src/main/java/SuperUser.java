import spread.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SuperUser {
    public static void main(String[] args) throws UnknownHostException, SpreadException {
        /*

        Message account = new Message();
        int port = Integer.parseInt(args[0]);
        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        SpreadConnection conn = new SpreadConnection();
        conn.connect(InetAddress.getByName("localhost"), 4803, "server" + port, false, false);

        conn.add(new BasicMessageListener() {
            @Override
            public void messageReceived(SpreadMessage spreadMessage) {
                String msg = new String(spreadMessage.getData(), StandardCharsets.UTF_8);
                String[] fields = msg.split(" ");
                byte[] response;

                response = msg.getBytes();

                if(fields[0].equals("check")){
                    response = ("Value is " + account.check()).getBytes();
                }
                else{
                    int value = Integer.parseInt(fields[1]);
                    account.increment(value);

                    response = "Increment done!".getBytes();
                }

                SpreadMessage send = new SpreadMessage();
                send.setData(response);
                send.setReliable();
                send.addGroup(spreadMessage.getSender());

                try{
                    conn.multicast(send);
                }
                catch (SpreadException e){
                    e.printStackTrace();
                }
            }
        });

        SpreadGroup group = new SpreadGroup();
        group.join(conn, "servers");

        es.scheduleAtFixedRate(() -> {
            System.out.println("Server " + port + " has " + account.check());
        }, 0 ,5, TimeUnit.SECONDS);
    */
    }
}
