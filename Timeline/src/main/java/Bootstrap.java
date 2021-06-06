import aux.Pair;
import aux.Triple;
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
    //private Map<String, String> superPeers;
    private List<String> spreadDaemons = Arrays.asList("4803", "4804", "4805");
    private Map<String, Pair<String, String>> superPeers; // Username - (Endereço, Daemon)
    private Map<String, Triple<String, Boolean, String>> peers;



    public Bootstrap() {

        this.addr = Address.from(Integer.parseInt("10000"));
        this.es = Executors.newScheduledThreadPool(1);
        this.ms = new NettyMessagingService("Bootstrap", this.addr, new MessagingConfig());

        //Colocar o Serializer -- é preciso um formato de msg
        this.s = Serializer.builder().withTypes(InitMsg.class, Message.class).build();

        //formato username - IP : Netty_Port : Spread_Daemon
        this.superPeers = new HashMap<>();
        this.peers = new HashMap<>();

        ms.registerHandler("handle-Register", (a, m) -> {
            Message msg = this.s.decode(m);

            String username = msg.getUsername();
            String pass = msg.getPass();

            if (this.peers.containsKey(username)){
                ms.sendAsync(a, "response-log", this.s.encode(false));
            }
            else {
                this.peers.put(username, new Triple<>(pass, false, a.toString()));
                login(username, pass, a);
            }

        }, this.es);

        ms.registerHandler("handle-LogIn", (a, m) -> {
            Message msg = this.s.decode(m);

            String username = msg.getUsername();
            String pass = msg.getPass();

            login(username, pass, a);

        }, this.es);

        //#######################   ENTRY REQUEST       #######################//
        ms.registerHandler("handle-entry-req", (a,m)->{

            InitMsg msg = this.s.decode(m);

            if(superPeers.size() == 0) {

                String spread_port = getSpreadPort(msg, a);
                InitMsg rsp = new InitMsg(true, spread_port);
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));

            }else if(superPeers.size() < 2){

                String spread_port = getSpreadPort(msg, a);
                InitMsg rsp = new InitMsg(true, spread_port);
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));

            }else{

                // Get Random SP se existir.
                Object[] keys = superPeers.keySet().toArray();
                Object key = keys[new Random().nextInt(keys.length)];
                //System.out.println("************ Random Value ************ \n" + key + " :: " + superPeers.get(key));

                // GET do Endereço do Spread Daemon
                InitMsg rsp = new InitMsg(false, key.toString(), superPeers.get(key).getSecond());
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));

            }

        }, this.es);

        ms.registerHandler("handle-logout", (a,m) -> {

        }, this.es);

        this.ms.start();

    }


    private String getSpreadPort(InitMsg m, Address ad){

        Random rand = new Random();
        String spread_port = spreadDaemons.get(rand.nextInt(spreadDaemons.size()));
        Pair values = new Pair<>(ad.toString(), spread_port);

        this.superPeers.put(m.getUsername(), values);

        return spread_port;
    }

    private void login(String username, String pass, Address address){
        Random rand = new Random();

        if (this.peers.containsKey(username) && pass.equals(this.peers.get(username).getFirst())){

            this.peers.put(username, new Triple<>(pass, true, address.toString()));
            ms.sendAsync(address, "response-log", this.s.encode(true));
        }

        else {
            ms.sendAsync(address, "response-log", this.s.encode(false));
        }
    }

    /* Para propósitos de Debug */
    public void showState()
    {
        System.out.println("------------------");
        for(Map.Entry<String, Pair<String, String>> e : this.superPeers.entrySet())
        {
            System.out.println(e.getKey() + " : " + e.getValue());
        }
    }

    public static void main(String[] args) throws IOException, SpreadException, ExecutionException, InterruptedException {

        Bootstrap bootstrap = new Bootstrap();

    }

}