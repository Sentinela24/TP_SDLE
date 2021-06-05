import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class User {
    private SpreadConnection conn;
    private CompletableFuture<byte[]> request;
    private boolean[] first;
    private BufferedReader reader;

    public User(String port) throws UnknownHostException, SpreadException {
        this.conn = new SpreadConnection();
        conn.connect(InetAddress.getByName("localhost"), 4803, "client" + port, false, false);

        this.first = new boolean[] {true};

        conn.add(new BasicMessageListener() {
            @Override
            public void messageReceived(SpreadMessage spreadMessage) {
                if (first[0]){
                    synchronized (first){
                        first[0] = false;
                    }

                    String receivedData = new String(spreadMessage.getData(), StandardCharsets.UTF_8);

                    //Msg q vem do server
                    System.out.println(receivedData);

                    request.complete(spreadMessage.getData());
                }
            }
        });

        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }


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
            }
        }

    }
}