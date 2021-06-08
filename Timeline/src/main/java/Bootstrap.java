import Messages.InitMsg;
import Messages.Message;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Bootstrap {

    private Address addr;
    private NettyMessagingService ms;
    private final ScheduledExecutorService es;
    private Serializer s;
    private Map<String, List<String>> SuperPeers;
    private Map<String, String> Peers;
    private List<String> spreadDaemons = Arrays.asList("4803#alfa", "4804#bravo", "4805#charlie");
    private int leafs_cnt;
    private int sp_cnt;


    public Bootstrap() {

        this.addr = Address.from(Integer.parseInt("10000"));
        this.es = Executors.newScheduledThreadPool(1);
        this.ms = new NettyMessagingService("Bootstrap", this.addr, new MessagingConfig());
        this.s = Serializer.builder().withTypes(InitMsg.class, Message.class).build();

        this.SuperPeers = new HashMap<>();                  //formato username - IP : Netty_Port : Spread_Daemon
        this.Peers = new HashMap<>();


        // ##### REGISTER #####//
        ms.registerHandler("handle-Register", (a, m) -> {

            Message msg = this.s.decode(m);

            String username = msg.getUsername();
            String pass = msg.getPass();

            if (this.Peers.containsKey(username)){
                ms.sendAsync(a, "response-log", this.s.encode(false));
            }
            else {
                this.Peers.put(username, pass);
                login(username, pass, a);
            }

        }, this.es);

        ms.registerHandler("handle-LogIn", (a, m) -> {

            Message msg = this.s.decode(m);

            String username = msg.getUsername();
            String pass = msg.getPass();

            login(username, pass, a);

        }, this.es);

        //#######################   ENTRY REQUEST   #######################//
        ms.registerHandler("handle-entry-req", (a,m)->{

            InitMsg msg = this.s.decode(m);

            if(SuperPeers.size() == 0) {

                this.sp_cnt++;
                String spread_port = getSpreadPort(msg, a);
                InitMsg rsp = new InitMsg(true, spread_port);
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));


            }else if(SuperPeers.size() < 2){

                this.sp_cnt++;
                String spread_port = getSpreadPort(msg, a);
                InitMsg rsp = new InitMsg(true, spread_port);
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));

            }else{

                // Get Random SP se existir.
                Object[] keys = SuperPeers.keySet().toArray();
                Object key = keys[new Random().nextInt(keys.length)];

                // GET do Endereço do Spread Daemon
                InitMsg rsp = new InitMsg(false, key.toString(), SuperPeers.get(key).get(1));
                ms.sendAsync(a, "entry-resp", this.s.encode(rsp));

                this.leafs_cnt++;

                //SE RACIO < 10 - REQ ELECTION
                if((double) leafs_cnt/sp_cnt > 2.0){

                    //Request Random SupeerPeer to make election
                    Message payload = new Message("null");
                    ms.sendAsync(Address.from(SuperPeers.get(key).get(0)), "elect", this.s.encode(payload));

                }
            }

            showState();

        }, this.es);

        //##################   HANDLE ELECTION PROMOTION   ##################//
        ms.registerHandler("joined-sp", (a,m)->{

            InitMsg msg = this.s.decode(m);

            this.SuperPeers.put(msg.getUsername(), msg.getBecome());
            this.leafs_cnt--;
            this.sp_cnt++;

        }, this.es);


        ms.registerHandler("handle-peer-logout", (a,m) -> {

            this.leafs_cnt--;

        }, this.es);

        ms.registerHandler("handle-SP-logout", (a,m) -> {

            Message msg = this.s.decode(m);

            //Remover SP do Map
            this.SuperPeers.remove(msg.getUsername());
            showState();

            this.sp_cnt--;

        }, this.es);

        this.ms.start();
    }


    private String getSpreadPort(InitMsg m, Address ad){

        Random rand = new Random();
        String spread_port = spreadDaemons.get(rand.nextInt(spreadDaemons.size()));
        List<String> values = new ArrayList<>();
        values.add(ad.toString());
        values.add(spread_port);
        this.SuperPeers.put(m.getUsername(), values);

        return spread_port;
    }


    private void login(String username, String pass, Address address){

        if (this.Peers.containsKey(username) && pass.equals(this.Peers.get(username))){

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
        for(Map.Entry<String, List<String>> e : this.SuperPeers.entrySet())
        {
            System.out.println(e.getKey() + " : " + e.getValue());
        }
    }

    //#######################   MAIN   #######################//

    public static void main(String args[]) {

        Bootstrap bootstrap = new Bootstrap();

    }
}