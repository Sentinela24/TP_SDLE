import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import spread.SpreadException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class Bootstrap {

    private Address addr;
    private NettyMessagingService ms;
    private ScheduledExecutorService es;
    private Serializer s;
    private Map<String, String> SuperPeers;


    public Bootstrap() {

        this.addr = Address.from(Integer.parseInt("10000"));

        this.es = Executors.newScheduledThreadPool(1);
        this.ms = new NettyMessagingService("Bootstrap", this.addr, new MessagingConfig());

        //Colocar o Serializer -- é preciso um formato de msg
        this.s = Serializer.builder().withTypes(InitMsg.class).build();

        //formato username - IP:Port, pq em larga escala pode haver users com ips iguais
        this.SuperPeers = new HashMap<String, String>();


        //#######################   ENTRY REQUEST       #######################//
        ms.registerHandler("handle-entry-req", (a,m)->{

            InitMsg msg = this.s.decode(m);

            if(SuperPeers.size() == 0) {

                this.SuperPeers.put(msg.getUsername(), a.toString());


                InitMsg rsp = new InitMsg(true, "null");
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));

            }else if(SuperPeers.size() < 4){

                // Get Random SP se existir.
                Object[] keys = SuperPeers.keySet().toArray();
                Object key = keys[new Random().nextInt(keys.length)];
                //System.out.println("************ Random Value ************ \n" + key + " :: " + SuperPeers.get(key));

                this.SuperPeers.put(msg.getUsername(), a.toString());

                InitMsg rsp = new InitMsg(true, SuperPeers.get(key));
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));


            }else{
                // Get Random SP se existir.
                Object[] keys = SuperPeers.keySet().toArray();
                Object key = keys[new Random().nextInt(keys.length)];
                System.out.println("************ Random Value ************ \n" + key + " :: " + SuperPeers.get(key));

                InitMsg rsp = new InitMsg(false, SuperPeers.get(key));
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));

            }

        }, this.es);

        this.ms.start();

    }

    /* Para propósitos de Debug */
    public void showState()
    {
        System.out.println("------------------");
        for(Map.Entry<String, String> e : this.SuperPeers.entrySet())
        {
            System.out.println(e.getKey() + " : " + e.getValue());
        }
    }

    public static void main(String args[]) throws IOException, SpreadException, ExecutionException, InterruptedException {

        Bootstrap bootstrap = new Bootstrap();

    }

}
