import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import spread.*;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;
import java.util.List;
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
    private String pass;
    private boolean isSuper;
    private String sp_user;

    private SpreadConnection conn;
    private CompletableFuture<byte[]> request;
    private CompletableFuture<byte[]> request2;
    private boolean[] first;
    private BufferedReader reader;
    private SpreadGroup super_user_group;

    private Following following;
    private Followers followers;

    private boolean online;
    private int superuser_connected_users;
    private boolean prepare_to_leave;
    private boolean checked;

    public NewUser(String ownAddr) throws UnknownHostException, SpreadException {

        this.ownAddr = Address.from(Integer.parseInt(ownAddr));
        this.bootStrapAddr = Address.from(Integer.parseInt("10000"));
        //this.bootStrapAddr = Address.from(bootStrapAddr);
        this.isSuper = false;

        this.es = Executors.newScheduledThreadPool(1);
        this.ms = new NettyMessagingService("Peer", this.ownAddr, new MessagingConfig());
        this.s = Serializer.builder().withTypes(InitMsg.class, Message.class, Post.class, GregorianCalendar.class).build();
        this.lastId = 0;

        this.conn = new SpreadConnection();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.super_user_group = new SpreadGroup();

        this.online = false;
        this.superuser_connected_users = 0;
        this.prepare_to_leave = false;
        this.checked = false;

        this.request = new CompletableFuture<>();
        this.request2 = new CompletableFuture<>();

        // ################### ATOMIX ####################

        ms.registerHandler("response-log", (a, m) -> {
            this.checked = this.s.decode(m);
            request.complete(m);

        }, this.es);

        ms.registerHandler("entry-resp", (a,m)->{

            InitMsg msg = this.s.decode(m);
            this.isSuper = msg.isSuper();
            System.out.println(this.isSuper);

            if (this.isSuper){

                try {
                    //conectar-se ao daemon
                    //criar spread group & join spread group
                    System.out.println(Integer.parseInt(msg.getAddr()));

                    conn.connect(InetAddress.getByName("localhost"), Integer.parseInt(msg.getAddr()), this.username, false, true);
                    super_user_group.join(conn, username + "_SUPERGROUP");

                } catch (SpreadException | UnknownHostException e) {
                    e.printStackTrace();
                }

            }

            else { /////// SE FOR PEER NORMAL //////
                this.sp_user = msg.getUsername();

                //connect to given spread daemon & join given userNameSuperGroup
                try {
                    System.out.println(Integer.parseInt(msg.getAddr()));
                    System.out.println(this.sp_user);

                    conn.connect(InetAddress.getByName("localhost"), Integer.parseInt(msg.getAddr()), this.username, false, true);
                    super_user_group.join(conn, this.sp_user + "_SUPERGROUP");

                } catch (SpreadException | UnknownHostException e) {
                    e.printStackTrace();
                }
            }

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
            Listener_User l_User = new Listener_User(this, this.conn);
            Thread t = new Thread(l_User);
            t.start();

            this.request2.complete(m);

        }, this.es);

        this.reader = new BufferedReader(new InputStreamReader(System.in));
        ms.start();
    }

    // ################### req super peer ####################
    public void reqSuperPeer() throws ExecutionException, InterruptedException {

        InitMsg msg = new InitMsg(this.username, ++this.lastId);
        ms.sendAsync(bootStrapAddr, "handle-entry-req", this.s.encode(msg));
        request2.get();
    }

    // ########################### //

    public void initial_menu() throws IOException, ExecutionException, InterruptedException, SpreadException {
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
                break;
        }

        menu();
    }

    public void verify_entry(String type) throws IOException, ExecutionException, InterruptedException {
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

        this.following = new Following(this, this.username, this.conn, this.reader, this.s);
        String following_file = this.username + "_following.db";
        load_following_file(following_file);

        this.followers = new Followers(this.username, this.conn, this.s, this.reader);
        String followers_file = this.username + "_followers.db";
        load_followers_file(followers_file);

        //String user_file = this.username + "_user.db";
        //load_user_file(user_file);
    }

    public void menu() throws IOException, SpreadException, ExecutionException, InterruptedException {
        String opcao;

        reqSuperPeer();

        while (online && !(prepare_to_leave)) {
            System.out.println("\n----------- MENU -----------\n");
            System.out.println("1 - Post");
            System.out.println("2 - Subscribe");
            System.out.println("3 - Unsubscribe");
            System.out.println("4 - Timeline");
            System.out.println("5 - LogOut");
            System.out.print("Escolha uma das opções: ");
            System.out.println("\n----------------------------");

            opcao = this.reader.readLine();

            switch (opcao) {
                case "1":

                    this.following.post();
                    break;

                case "2":

                    this.followers.follow();
                    //save_user();
                    break;

                case "3":

                    this.followers.unfollow();
                    break;

                case "4":
                    this.followers.get_timeline();

                    break;

                case "5":
                    if (this.isSuper) {
                        super_user_logout();
                    }
                    else {
                        user_logout();
                    }
                    break;

                default:
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
        System.out.println(type);

        switch (type) {
            case "POST" : //POST recebido de alguém que estamos a seguir (following)
                this.followers.update_post(getSpreadMsgUsername(spread_msg.getSender().toString()), msg.getPosts());
                break;

            case "POSTS" :
                // Recebido do nosso following (quem estámos a seguir)
                if (following.equals(getSpreadMsgUsername(spread_msg.getSender().toString()))) {
                    this.followers.update_posts(getSpreadMsgUsername(spread_msg.getSender().toString()), msg.getPosts());
                }

                // Recebido de um follower do nosso following
                else {
                    this.followers.update_posts(following, msg.getPosts());
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

                // Caso de ser um follower e não o following direto
                else {
                    List<Post> posts = this.followers.get_posts(following, msg.getLast_post_ID());
                    response.setPosts(posts);
                    response.setFollowing(following);
                    this.followers.send_message(response, spread_msg.getSender().toString());
                }

                break;

            default :
                break;
        }
    }

    private void user_logout() throws SpreadException, IOException {
        this.online = false;

        ms.sendAsync(bootStrapAddr, "handle-logout", this.s.encode(this.username));

        this.followers.logout();
        this.following.logout();
        this.super_user_group.leave();

        save_user();
        System.exit(0);
    }

    public void super_user_logout() throws SpreadException, IOException {
        ms.sendAsync(bootStrapAddr, "handle-logout", this.s.encode(this.username));

        this.followers.logout();
        this.following.logout();

        if (this.superuser_connected_users == 1){
            if (this.super_user_group != null) {
                this.super_user_group.leave();
            }

            this.online = false;
            save_user();
            System.exit(0);
        }
        else{
            this.prepare_to_leave = true;
        }

    }

    private void load_following_file(String following_file) {
        Following from_file;

        try {
            FileInputStream fis = new FileInputStream(following_file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            from_file = (Following) ois.readObject();
            this.following.setMyPosts(from_file.getMyPosts());

            fis.close();
            ois.close();

            System.out.println("Following info read from file!");
        } catch (ClassNotFoundException | IOException e) {
            System.out.println("Following info hasn't been loaded!");
        }
    }

    private void load_followers_file(String followers_file) {
        Followers from_file;

        try {
            FileInputStream fis = new FileInputStream(followers_file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            from_file = (Followers) ois.readObject();
            this.followers.setFollowings(from_file.getFollowings());

            fis.close();
            ois.close();

            System.out.println("Followers info read from file!");
        } catch (ClassNotFoundException | IOException e) {
            System.out.println("Followers info hasn't been loaded!");
        }
    }

    /*

    private void load_user_file(String user_file){
        NewUser from_file;

        FileInputStream fis = new FileInputStream(followers_file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        from_file = (NewUser) ois.readObject();

    }
    */

    private void save_user() throws IOException {

        //String user_file = this.username + "_user.db";
        //FileOutputStream fos = new FileOutputStream(user_file);
        //ObjectOutputStream oos = new ObjectOutputStream(fos);

        //oos.writeObject(this);

        String following_file = this.username + "_following.db";
        FileOutputStream fos = new FileOutputStream(following_file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(this.following);

        String followers_file = this.username + "_followers.db";
        fos = new FileOutputStream(followers_file);
        oos = new ObjectOutputStream(fos);

        oos.writeObject(this.followers);

        fos.close();
        oos.close();

        System.out.println("Info saved in respective files!");
    }


}
