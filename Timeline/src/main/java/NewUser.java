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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NewUser {

    private Address addr;
    private NettyMessagingService ms;
    private ScheduledExecutorService es;
    private Serializer s;

    private Address bootStrapAddr;
    private Address ownAddr;
    private Integer lastId;
    private String username;
    private boolean isSuper;

    private SpreadConnection conn;
    private CompletableFuture<byte[]> request;
    private boolean[] first;
    private BufferedReader reader;
    private SpreadGroup super_user_group;

    private Following following;
    private Followers followers;

    public NewUser(String usr, String ownAddr) throws UnknownHostException, SpreadException {

        this.username = usr;
        this.ownAddr = Address.from(Integer.parseInt(ownAddr));
        this.bootStrapAddr = Address.from(Integer.parseInt("10000"));
        //this.bootStrapAddr = Address.from(bootStrapAddr);
        this.isSuper = false;

        this.es = Executors.newScheduledThreadPool(1);
        this.ms = new NettyMessagingService("Peer", this.ownAddr, new MessagingConfig());
        this.s = Serializer.builder().withTypes(InitMsg.class).build();
        this.lastId = 0;

        // ################### ATOMIX ####################
        ms.registerHandler("entry-resp", (a,m)->{

            InitMsg msg = this.s.decode(m);
            this.isSuper = msg.isSuper();
            // if true become super ?
            // else recolher

            //System.out.println(msg.getAddr());

        }, this.es);

        this.reader = new BufferedReader(new InputStreamReader(System.in));
        ms.start();
    }


    public void super_group_conn() throws UnknownHostException, SpreadException {

        // ################### SPREAD ####################

        this.conn = new SpreadConnection();
        conn.connect(InetAddress.getByName("localhost"), 4803, "client" + this.ownAddr, false, false);
        this.first = new boolean[] {true};

        this.super_user_group = new SpreadGroup();

        //Necessário acrescentar qual o superuser que o bootstrap forneceu à frente de "SuperGroup" - usar o username desse superpeer
        this.super_user_group.join(this.conn, "SuperGroup");

        //Juntar-se ao seu grupo de seguidores (para lhes enviar msgs)
        this.followers.entry();

        //Juntar-se aos grupos de quem está a seguir para receber as msgs
        this.following.entry();


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


    // ######################## //

    /*
    public void check(String msg) throws ExecutionException, InterruptedException {
        this.request = new CompletableFuture<>();
        this.first[0] = true;

        String group = "servers";

        SpreadMessage message = new SpreadMessage();

        byte[] send_msg = msg.getBytes();

        message.setData(send_msg);
        message.setSafe();
        message.addGroup(group);

        try{
            this.conn.multicast(message);
        }
        catch (SpreadException e){
            System.out.println("Unable to MULTICAST Check request to group " + group);
            e.printStackTrace();
        }

        this.request.get();
    }
    // ########################### //
    */

    /*
    public void increment(String msg) throws ExecutionException, InterruptedException {
        this.request = new CompletableFuture<>();
        this.first[0] = true;

        String group = "servers";

        SpreadMessage message = new SpreadMessage();

        byte[] send_msg = msg.getBytes();

        message.setData(send_msg);
        message.setSafe();
        message.addGroup(group);

        try{
            this.conn.multicast(message);
        }
        catch (SpreadException e){
            System.out.println("Unable to MULTICAST Increment request to group " + group);
            e.printStackTrace();
        }

        this.request.get();
    }
     */

    // ########################### //

    public void menu() throws IOException, SpreadException, ExecutionException, InterruptedException {
        String opcao, valor;
        boolean valido = true;

        reqSuperPeer();
        super_group_conn();

        while (valido) {
            System.out.println("\n----------- MENU -----------\n");
            System.out.println("1-Post");
            System.out.println("2-Subscribe");
            System.out.println("3-Unsubscribe");
            System.out.print("Escolha uma das opções: ");

            opcao = this.reader.readLine();

            switch (opcao) {
                case "1":
                    //String msg = ("check");
                    //check(msg);

                    this.following.post();
                    break;
                case "2":
                    //System.out.print("Valor: ");
                    //valor = this.reader.readLine();
                    //String msg = ("increment" + " " + valor);
                    //increment(msg);

                    this.followers.follow();
                    break;
                case "3":

                    this.followers.unfollow();
                    break;
                default:
                    valido = false;
                    break;
            }
        }
    }

    public String getSpreadMsgUsername(String private_group){
        String[] tokens = private_group.substring(1).split("#");

        return tokens[0];
    }

    public void message_process(SpreadMessage spread_msg) {
        if (spread_msg.isRegular()) {
            message_process_regular(spread_msg);
        }
        else {
            //message_process_membership(spread_msg);
        }
    }

    private void message_process_regular(SpreadMessage spread_msg){
        Message msg = this.s.decode(spread_msg.getData());
        String following = msg.getFollowing();
        String type = msg.getType();

        switch (type) {
            case "POST" : //POST recebido de alguém que estamos a seguir (following)
                this.followers.update_posts(getSpreadMsgUsername(spread_msg.getSender().toString()), msg.getPosts());
                break;

            case "POSTS" :
                // Recebido do nosso following (quem estámos a seguir)
                if (following.equals(getSpreadMsgUsername(spread_msg.getSender().toString()))) {
                    this.followers.update_posts(getSpreadMsgUsername(spread_msg.getSender().toString()), msg.getPosts());
                }

                // Recebido de um follower do nosso following


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

                // Caso de ser um follower e não o following direto

                break;

            // Mensagens do servidor bootstrap........

            default :
                break;
        }
    }


}
