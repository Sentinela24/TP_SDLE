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
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class NewUser {

    private NettyMessagingService ms;
    private final ScheduledExecutorService es;
    private Serializer s;

    private Address bootStrapAddr;
    private Address ownAddr;
    private Integer lastId;
    private String username;
    private String pass;

    private boolean isSuper;
    private String sp_user;
    private String sp_user_unicast;
    private int sp_user_port;
    private Map<String, List<String>> peers_reg;

    private SpreadConnection conn;
    private BufferedReader reader;

    private CompletableFuture<byte[]> request;
    private CompletableFuture<byte[]> request2;
    private SpreadGroup super_user_group;

    private Following following;
    private Followers followers;

    private boolean online;
    private boolean checked;
    private Listener_User l_User;

    public NewUser() {

        //Ter Endereços Netty Diferentes em Localhost
        Random random = new Random();
        int rnd = random.nextInt(1000 - 1) + 8000;

        this.ownAddr = Address.from(rnd);
        this.bootStrapAddr = Address.from(Integer.parseInt("10000"));
        this.isSuper = false;

        this.es = Executors.newScheduledThreadPool(1);
        this.ms = new NettyMessagingService("Peer", this.ownAddr, new MessagingConfig());
        this.s = Serializer.builder().withTypes(InitMsg.class, Message.class, Post.class, LocalDateTime.class, GregorianCalendar.class).build();
        this.lastId = 0;
        this.conn = new SpreadConnection();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.super_user_group = new SpreadGroup();

        this.online = false;
        this.checked = false;

        this.request = new CompletableFuture<>();
        this.request2 = new CompletableFuture<>();

        // ################### ATOMIX ####################

        ms.registerHandler("response-log", (a, m) -> {

            this.checked = this.s.decode(m);
            request.complete(m);

        }, this.es);

        // ##### ENTRY RESP ##### //
        ms.registerHandler("entry-resp", (a,m)->{

            InitMsg msg = this.s.decode(m);
            this.isSuper = msg.isSuper();

            this.sp_user = msg.getUsername(); //username of SP - se for super n vai ter nada
            this.sp_user_port = Integer.parseInt(msg.getAddr().split("#")[0]);
            this.sp_user_unicast = "#" + this.sp_user + "#" + msg.getAddr().split("#")[1];

            if(isSuper){

                System.out.println("SUPER");
                this.peers_reg = new HashMap<>();

                //conectar-se ao daemon
                //criar spread group & join spread group
                try {

                    conn.connect(InetAddress.getByName("localhost"), this.sp_user_port, "" + this.username, false, false);
                    super_user_group.join(conn, username + "_SUPERGROUP");

                } catch (SpreadException | UnknownHostException e) {
                    e.printStackTrace();
                }

            }

            else /////// SE FOR PEER NORMAL //////

                {
                    //connect to given spread daemon & join given userNameSuperGroup
                    try {

                        conn.connect(InetAddress.getByName("localhost"), this.sp_user_port, this.username, false, false);
                        super_user_group.join(conn, this.sp_user + "_SUPERGROUP");

                    } catch (SpreadException | UnknownHostException e) {
                        e.printStackTrace();
                    }

                    // send my hw stats
                    pushSpecs();

            }

            this.following = new Following(this, this.username, this.conn, this.reader, this.s);
            this.followers = new Followers(this.username, this.conn, this.s, this.reader);

            //Juntar-se ao seu grupo de seguidores (para lhes enviar msgs)
            try {
                this.followers.entry();
            } catch (SpreadException e) {
                e.printStackTrace();
            }

            //Juntar-se aos grupos de quem está a seguir para receber as msgs
            try {
                this.following.entry();
            } catch (SpreadException e) {
                e.printStackTrace();
            }

            this.online = true;

            // ################## THREAD TO LISTEN ##################
            this.l_User = new Listener_User(this, this.conn);
            Thread t = new Thread(l_User);
            t.start();

            this.request2.complete(m);

        }, this.es);


        ms.registerHandler("elect", (a,m)->{

            System.out.println("Election");
            election("ratio");

        }, this.es);

        // ################### END ATOMIX ####################

        ms.start();

        //reqSuperPeer();

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
        sendMsg(send_msg, this.sp_user_unicast);

    }
    // ###################   ####################


    private void election(String type){

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

            Message msg;

            // SE for Logout Notificar os peers para sairem do meu group
            if(type.equals("logout")){

                peers_reg.remove(key);

                // comunicar ao peer & ao BS
                msg = new Message("BECOME", this.peers_reg);
                sendMsg(msg, key);

                String new_sp = key.split("#")[1];
                Message new_msg = new Message("SP_LOGOUT_DISCONNECT", new_sp);
                sendMsg(new_msg, username + "_SUPERGROUP");

            }else{

                // comunicar ao peer & ao BS
                msg = new Message("BECOME");
                sendMsg(msg, key);

                peers_reg.remove(key);

            }

        }else if(type.equals("logout")){

                Message new_msg = new Message("SP_LOGOUT_DISCONNECT", "null");
                sendMsg(new_msg, username + "_SUPERGROUP");

        }

    }

    // ################### BECOME SP #################### //
    private void becomeSP() throws SpreadException {

        // Disconnect from Current Spread_Conn, initialize SP vars
        conn.disconnect();

        //conectar-se ao daemon + criar spread group & join spread group
        try {

            conn.connect(InetAddress.getByName("localhost"), this.sp_user_port, "" + this.username, false, false);
            SpreadGroup group = new SpreadGroup();
            group.join(conn, username + "_SUPERGROUP");

        } catch (SpreadException | UnknownHostException e) {
            e.printStackTrace();
        }

        List<String> values = new ArrayList<>();
        values.add(ownAddr.toString());
        values.add(sp_user_port + "#" + sp_user_unicast.split("#")[2]);

        InitMsg new_msg = new InitMsg(this.username, values);
        ms.sendAsync(bootStrapAddr, "joined-sp", this.s.encode(new_msg));       //Notify Bootstrap


        try {
            this.followers.entry();
            this.following.entry();
        } catch (SpreadException e) {
            e.printStackTrace();
        }

    }


    public String getSpreadMsgUsername(String private_group){
        String[] tokens = private_group.substring(1).split("#");

        return tokens[0];
    }

    // ################### PROCESS MSGS #################### //

    public void message_process(SpreadMessage spread_msg) throws SpreadException {
        if (spread_msg.isRegular()) {
            message_process_regular(spread_msg);
        }
        else {
            //message_process_membership(spread_msg);
        }
    }

    private void message_process_regular(SpreadMessage spread_msg) throws SpreadException {

        Message msg = this.s.decode(spread_msg.getData());

        String following = msg.getFollowing();
        String type = msg.getType();

        switch (type) {
            case "POST" : //POST recebido de alguém que estamos a seguir (following)
                this.followers.update_post(getSpreadMsgUsername(spread_msg.getSender().toString()), msg.getPosts());
                break;

            case "POSTS" :
                // Recebido do nosso following (quem estámos a seguir)
                if (following.equals(getSpreadMsgUsername(spread_msg.getSender().toString()))) {
                    this.followers.update_posts(getSpreadMsgUsername(spread_msg.getSender().toString()), msg.getPosts());
                }

                break;

            case "REQUEST" : //REQUEST de algum follower
                Message response = new Message();
                response.setType("POSTS");

                // Verificar se sou eu o following
                if (this.username.equals(following)){
                    response.setPosts(this.following.get_posts(msg.getLast_post_ID()));
                    // Status...
                    response.setFollowing(following);
                    this.following.send_message(response, spread_msg.getSender().toString());
                }

                break;

            case "SPECS" :

                List<String> values = new ArrayList<>();
                values.add(""+msg.getCpu());
                values.add(msg.getBoot());
                peers_reg.put(spread_msg.getSender().toString(), values);

                break;

            case "BECOME" :

                //Become SP
                this.isSuper = true;
                this.peers_reg = new HashMap<>();

                // Se for por logout receber os estado e meter já no map
                if(msg.getSp_register() != null){

                    this.peers_reg.putAll(msg.getSp_register());
                }

                becomeSP();
                break;

            case "LOGOUT" :

                this.peers_reg.remove(spread_msg.getSender().toString());
                break;

            case "SP_LOGOUT_DISCONNECT":

                //Deixar Grupo SP && Ligar-se ao Novo
                this.super_user_group.leave();

                if(spread_msg.getSender().toString().split("#")[1].equals(username)){
                    this.online = false;
                    System.out.println("Exiting...");
                    System.exit(0);
                }else{
                    super_user_group.join(conn, msg.getUsername() + "_SUPERGROUP");
                }
                break;

            default :
                break;
        }
    }


    public void verify_entry(String type) throws IOException, ExecutionException, InterruptedException, NoSuchAlgorithmException {

        System.out.println("\n---------- " + type + " ----------" );
        Message msg;

        while (!this.checked){

            System.out.println("Enter your username: ");
            this.username = reader.readLine();

            System.out.println("Enter your password: ");
            this.pass = reader.readLine();

            msg = new Message();
            msg.setUsername(this.username);
            msg.setPass(this.pass);

            if (type.equals("LogIn"))
                ms.sendAsync(bootStrapAddr, "handle-LogIn", this.s.encode(msg));

            else
                ms.sendAsync(bootStrapAddr, "handle-Register", this.s.encode(msg));

            request.get();

            if(!this.checked) {
                System.out.println("Something went wrong, try again!");
            }
        }

    }


    private void logout() throws SpreadException {

        //Sair do meu grupo e dos utilizadores que me seguem
        this.followers.logout();
        this.following.logout();

        if(!isSuper){

            this.online = false;
            //Informar SP
            Message send_msg = new Message("LOGOUT");
            sendMsg(send_msg, this.sp_user_unicast);

            ms.sendAsync(bootStrapAddr, "handle-peer-logout", this.s.encode("null".getBytes()));

        }else{

            Message msg = new Message();
            msg.setUsername(this.username);

            ms.sendAsync(bootStrapAddr, "handle-SP-logout", this.s.encode(msg));
            // Seleciona 1, transfere estado, comunica aos peers ligados
            election("logout");

        }

        this.l_User.shutdown();
    }



    private void sendMsg(Message msg, String target){

        SpreadMessage message = new SpreadMessage();
        message.setData(this.s.encode(msg));
        message.setSafe();
        message.setCausal();
        message.addGroup(target);

        try{
            this.conn.multicast(message);
        }
        catch (SpreadException e){
            System.out.println("Failure!");
            e.printStackTrace();
        }

    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    public void initial_menu() throws IOException, ExecutionException, InterruptedException, SpreadException, NoSuchAlgorithmException {
        String opcao;

        System.out.println("\n----------- INITIAL MENU -----------\n");
        System.out.println("1 - LogIn");
        System.out.println("2 - Register");

        opcao = this.reader.readLine();

        switch (opcao) {
            case "1":
                verify_entry("LogIn");
                break;

            case "2":
                verify_entry("Register");
                break;

            default:
                System.exit(0);
                break;
        }

        menu();
    }


    // ########################### //

    public void menu() throws IOException, SpreadException, InterruptedException, NoSuchAlgorithmException {

        String opcao;

        reqSuperPeer();
        Thread.sleep(100);

        while (online) {
            System.out.println("\n----------- MENU -----------\n");
            System.out.println("1 - Post");
            System.out.println("2 - Subscribe");
            System.out.println("3 - Unsubscribe");
            System.out.println("4 - LogOut");
            if(isSuper){
                System.out.println("5 - DEBUG");
            }
            System.out.print("Escolha uma das opções: ");
            System.out.println("\n----------------------------");

            opcao = this.reader.readLine();

            switch (opcao) {
                case "1":

                    this.following.post();
                    break;

                case "2":

                    this.followers.follow();
                    break;

                case "3":

                    this.followers.unfollow();
                    break;

                case "4":

                        logout();

                    break;

                case "5":
                    // DEBUG //
                    System.out.println("------------------");
                    for(Map.Entry<String, List<String>> e : peers_reg.entrySet())
                    {
                        System.out.println(e.getKey() + " : " + e.getValue());
                    }
                    break;

                default:
                    break;
            }
        }

        System.out.println("Finnished with Success!");
        System.exit(0);

    }

}
