import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import spread.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class NewUser {

    private NettyMessagingService ms;
    private ScheduledExecutorService es;
    private Serializer s;

    private Address bootStrapAddr;
    private Address ownAddr;
    private Integer lastId;
    private String username;

    private boolean isSuper;
    private String sp_user;
    private String sp_user_unicast;
    private int sp_user_port;
    private Map<String, List<String>> peers_reg;

    private SpreadConnection conn;
    private BufferedReader reader;

    public NewUser(String usr, String ownAddr) throws UnknownHostException, SpreadException, InterruptedException {

        this.username = usr;
        this.ownAddr = Address.from(Integer.parseInt(ownAddr));
        this.bootStrapAddr = Address.from(Integer.parseInt("10000"));
        this.isSuper = false;

        this.es = Executors.newScheduledThreadPool(1);
        this.ms = new NettyMessagingService("Peer", this.ownAddr, new MessagingConfig());
        this.s = Serializer.builder().withTypes(InitMsg.class, Message.class, LocalDateTime.class).build();
        this.lastId = 0;
        this.conn = new SpreadConnection();


        // ################### ATOMIX ####################
        ms.registerHandler("entry-resp", (a,m)->{

            InitMsg msg = this.s.decode(m);
            this.isSuper = msg.isSuper();

            this.sp_user = msg.getUsername(); //username of SP - se for super n vai ter nada
            this.sp_user_port = Integer.parseInt(msg.getAddr().split("#")[0]);
            this.sp_user_unicast = "#" + this.sp_user + "#" + msg.getAddr().split("#")[1];

        }, this.es);


        ms.registerHandler("elect", (a,m)->{

            System.out.println("Election");
            //Select peer in peer_reg wit max uptime & max resources
            String key = null;
            int max = 0;
            LocalDateTime max_uptime = java.time.LocalDateTime.now();

            if(peers_reg.size() > 0){

                for (Map.Entry<String, List<String>> entry : peers_reg.entrySet()) {
                    if (Integer.parseInt(entry.getValue().get(0)) > max && LocalDateTime.parse(entry.getValue().get(1)).isBefore(max_uptime) ){
                        key = entry.getKey();
                        max = Integer.parseInt(entry.getValue().get(0));
                        max_uptime = LocalDateTime.parse(entry.getValue().get(1));
                    }
                }

                System.out.println("Peer " + key + " with " + max + " and " + max_uptime);

                // comunicar ao peer & ao BS
                Message msg = new Message("BECOME");

                SpreadMessage message = new SpreadMessage();
                message.setData(this.s.encode(msg));
                message.setSafe();
                message.addGroup(key);

                try{
                    this.conn.multicast(message);
                }
                catch (SpreadException e){
                    System.out.println("Failure!");
                    e.printStackTrace();
                }

                peers_reg.remove(key);

            }
        }, this.es);

        // ################### END ATOMIX ####################

        this.reader = new BufferedReader(new InputStreamReader(System.in));
        ms.start();

        reqSuperPeer();
        Thread.sleep(200);

        if(isSuper){

            System.out.println("SUPER");
            this.peers_reg = new HashMap<>();

            //conectar-se ao daemon
            //criar spread group & join spread group
            try {

                conn.connect(InetAddress.getByName("localhost"), this.sp_user_port, "" + this.username, false, false);
                SpreadGroup group = new SpreadGroup();
                group.join(conn, username + "_SUPERGROUP");

            } catch (SpreadException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        }

        else /////// SE FOR PEER NORMAL //////

            {

            //connect to given spread daemon & join given userNameSuperGroup
            try {

                conn.connect(InetAddress.getByName("localhost"), this.sp_user_port, "" + this.username, false, false);
                SpreadGroup group = new SpreadGroup();
                group.join(conn, this.sp_user + "_SUPERGROUP");

            } catch (SpreadException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            // send my hw stats
            pushSpecs();

            // create my group for followers to join
            // enter followees groups

            }

        // ################## THREAD TO LISTEN ##################
        Listener_User l_User = new Listener_User(this, this.conn);
        Thread t = new Thread(l_User);
        t.start();

    }


    // ################### req super peer ####################
    public void reqSuperPeer(){

        InitMsg msg = new InitMsg(this.username, ++this.lastId);
        ms.sendAsync(bootStrapAddr, "handle-entry-req", this.s.encode(msg));

    }


    // ################### push specs ####################
    public void pushSpecs(){

        //Send Peer Specs to SuperPeer
        int cpu = Runtime.getRuntime().availableProcessors();
        Random random = new Random();
        int rnd = random.nextInt(5 - 1) + 1;
        cpu = cpu * rnd;
        LocalDateTime boot = java.time.LocalDateTime.now();

        Message send_msg = new Message("SPECS", cpu, boot.toString());

        SpreadMessage message = new SpreadMessage();
        message.setData(this.s.encode(send_msg));
        message.setSafe();
        message.addGroup(this.sp_user_unicast);

        try{
            this.conn.multicast(message);
        }
        catch (SpreadException e){
            System.out.println("Failure!");
            e.printStackTrace();
        }

    }
    // ###################   ####################

    // ################### BECOME SP #################### //
    private void becomeSP() throws SpreadException, InterruptedException {

        // Disconnect from Current Spread_Conn, initialize SP vars
        conn.disconnect();
        this.isSuper = true;
        this.peers_reg = new HashMap<>();

        //conectar-se ao daemon + criar spread group & join spread group
        try {

            conn.connect(InetAddress.getByName("localhost"), this.sp_user_port, "" + this.username, false, false);
            SpreadGroup group = new SpreadGroup();
            group.join(conn, username + "_SUPERGROUP");

        } catch (SpreadException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        List<String> values = new ArrayList<String>();
        values.add(ownAddr.toString());
        values.add(sp_user_port + "#" + sp_user_unicast.split("#")[2]);

        InitMsg new_msg = new InitMsg(this.username, values);
        ms.sendAsync(bootStrapAddr, "joined-sp", this.s.encode(new_msg));       //Notify Bootstrap

    }



    // ################### PROCESS MSGS #################### //

    public void message_process(SpreadMessage spread_msg) throws SpreadException, InterruptedException {
        if (spread_msg.isRegular()) {
            message_process_regular(spread_msg);
        }
        else {
            //message_process_membership(spread_msg);
        }
    }

    private void message_process_regular(SpreadMessage spread_msg) throws SpreadException, InterruptedException {

        Message msg = this.s.decode(spread_msg.getData());
        //MyMSG msg = this.s.decode(spread_msg.getData());

        //String following = msg.getFollowing();
        String type = msg.getType();
        System.out.println(type);
        System.out.println(spread_msg.getSender());

        switch (type) {
            case "POST" : //POST recebido de alguém que estamos a seguir (following)
                System.out.println("new");
                //this.followers.update_posts(getSpreadMsgUsername(spread_msg.getSender().toString()), msg.getPosts());
                break;

            case "POSTS" :
                // Recebido do nosso following (quem estámos a seguir)
                /*
                if (following.equals(getSpreadMsgUsername(spread_msg.getSender().toString()))) {
                    this.followers.update_posts(getSpreadMsgUsername(spread_msg.getSender().toString()), msg.getPosts());
                }
                 */

                // Recebido de um follower do nosso following


                break;

            case "REQUEST" : //REQUEST de algum follower
                //Message response = new Message();

                /*
                response.setType("POSTS");

                // Verificar se sou eu o following
                if (this.username.equals(following)){
                    response.setPosts(this.following.get_posts(msg.getLast_post_ID()));
                    // Status...
                    response.setFollowing(following);
                    this.following.send_message(response, spread_msg.getSender().toString());
                }*/

                // Caso de ser um follower e não o following direto

                break;

            case "SPECS" :

                List<String> values = new ArrayList<String>();
                values.add(""+msg.getCpu());
                values.add(msg.getBoot());
                peers_reg.put(spread_msg.getSender().toString(), values);

                break;

            case "BECOME" :

                //Become SP
                becomeSP();
                break;


            // Mensagens do servidor bootstrap........

            default :
                break;
        }
    }








    // ########################### //

    public void menu() throws IOException, SpreadException, ExecutionException, InterruptedException {
        String opcao, valor;
        boolean valido = true;

        while (valido) {
            System.out.println("\n----------- MENU -----------\n");
            System.out.println("1-Check");
            System.out.println("2-Increment");
            System.out.print("Escolha uma das opções: ");
            opcao = this.reader.readLine();

            if (opcao.equals("1")) {
                // DEBUG //
                System.out.println("------------------");
                for(Map.Entry<String, List<String>> e : peers_reg.entrySet())
                {
                    System.out.println(e.getKey() + " : " + e.getValue());
                }
            }

            else if (opcao.equals("2")) {
                System.out.print("Valor: ");
                valor = this.reader.readLine();
                String msg = ("increment" + " " + valor);
                //increment(msg);
            }
            else {
                valido = false;
                ms.stop();
                es.shutdownNow();
            }
        }

    }


}
