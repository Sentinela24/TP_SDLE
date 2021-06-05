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
import java.util.concurrent.*;

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
    private String sp_user;

    private SpreadConnection conn;
    private CompletableFuture<byte[]> request;
    private boolean[] first;
    private BufferedReader reader;

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

        // ################### SPREAD ####################
        this.conn = new SpreadConnection();
        //conn.connect(InetAddress.getByName("localhost"), 4803, "client" + ownAddr, false, false);
        //this.first = new boolean[] {true};


        // ################### ATOMIX ####################
        ms.registerHandler("entry-resp", (a,m)->{

            InitMsg msg = this.s.decode(m);
            this.isSuper = msg.isSuper();

            if(isSuper){

                Message account = new Message();

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

                //conectar-se ao daemon
                //criar spread group & join spread group
                try {

                    conn.connect(InetAddress.getByName("localhost"), Integer.parseInt(msg.getAddr()), "client" + ownAddr, false, false);
                    SpreadGroup group = new SpreadGroup();
                    group.join(conn, username + "_SUPERGROUP");

                } catch (SpreadException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                es.scheduleAtFixedRate(() -> {
                    System.out.println("Server " + username + " has " + account.check());
                }, 0 ,5, TimeUnit.SECONDS);

            }
             else /////// SE FOR PEER NORMAL //////
                 {

                     this.first = new boolean[] {true};
                     this.sp_user = msg.getUsername();

                     conn.add(new BasicMessageListener() {
                         @Override
                         public void messageReceived(SpreadMessage spreadMessage) {

                             String receivedData = new String(spreadMessage.getData(), StandardCharsets.UTF_8);

                             //Msg q vem do server
                             System.out.println(receivedData);

                             request.complete(spreadMessage.getData());

                         }
                     });

                     //connect to given spread daemon & join given userNameSuperGroup
                     try {

                         conn.connect(InetAddress.getByName("localhost"), Integer.parseInt(msg.getAddr()), "client" + ownAddr, false, false);
                         SpreadGroup group = new SpreadGroup();
                         group.join(conn, this.sp_user + "_SUPERGROUP");

                     } catch (SpreadException e) {
                         e.printStackTrace();
                     } catch (UnknownHostException e) {
                         e.printStackTrace();
                     }
                     // send my hw stats
                     // create my group for followers to join
                     // enter followees groups

            }

        }, this.es);

        this.reader = new BufferedReader(new InputStreamReader(System.in));
        ms.start();
    }


    public boolean isSuper() {
        return isSuper;
    }

    // ################### req super peer ####################
    public void reqSuperPeer(){

        InitMsg msg = new InitMsg(this.username, ++this.lastId);
        ms.sendAsync(bootStrapAddr, "handle-entry-req", this.s.encode(msg));

    }


    // ######################## //
    public void check(String msg) throws ExecutionException, InterruptedException {
        this.request = new CompletableFuture<>();
        this.first[0] = true;

        //String group = "servers";
        String group = this.sp_user + "_SUPERGROUP";

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

    public void increment(String msg) throws ExecutionException, InterruptedException {
        this.request = new CompletableFuture<>();
        this.first[0] = true;

        //String group = "servers";
        String group = this.sp_user + "_SUPERGROUP";

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

    // ########################### //

    public void menu() throws IOException, SpreadException, ExecutionException, InterruptedException {
        String opcao, valor;
        boolean valido = true;

        reqSuperPeer();

        while (valido) {
            System.out.println("\n----------- MENU -----------\n");
            System.out.println("1-Check");
            System.out.println("2-Increment");
            System.out.print("Escolha uma das opções: ");
            opcao = this.reader.readLine();

            if (opcao.equals("1")) {
                String msg = ("check");
                check(msg);
            }

            else if (opcao.equals("2")) {
                System.out.print("Valor: ");
                valor = this.reader.readLine();
                String msg = ("increment" + " " + valor);
                increment(msg);
            }
            else {
                valido = false;
                ms.stop();
                es.shutdownNow();
            }
        }

    }


}
