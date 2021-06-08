package Peer;

import Messages.Message;
import Messages.Post;
import io.atomix.utils.serializer.Serializer;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class Followers {

    private SpreadConnection conn;
    private Serializer seri;
    private Map<String, Map<Integer, Post>> subscriptions_data;
    private Map<String, SpreadGroup> subscriptions_groups;
    private BufferedReader in;
    private int n_posts_received;
    private List<Post> posts_received;

    public Followers(SpreadConnection conn, Serializer seri, BufferedReader in) {
        this.conn = conn;
        this.seri = seri;
        this.in = in;

        this.subscriptions_data = new HashMap<>();
        this.subscriptions_groups = new HashMap<>();
        this.n_posts_received = 0;
        this.posts_received = new ArrayList<>();

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {

                System.out.println("Exec");

                for (Map.Entry<String, Map<Integer, Post>> usr : subscriptions_data.entrySet()) {

                    // getting entrySet() into Set
                    Set<Map.Entry<Integer, Post>> entrySet = usr.getValue().entrySet();

                    // Collection Iterator
                    Iterator<Map.Entry<Integer, Post>> itr = entrySet.iterator();

                    while(itr.hasNext()) {

                        Map.Entry<Integer, Post> entry = itr.next();

                        int post_id = entry.getKey();
                        Post p = entry.getValue();

                        //Se o tempo do post for mais antigo que há 10 min, apagar
                        long tenMinAgo = Calendar.getInstance().getTimeInMillis() - 60000L;

                        if(tenMinAgo > p.getDate().getTimeInMillis()){
                            System.out.println("Clean");
                            itr.remove();
                        }
                    }

                }

            }
        }, 0, 60000);

    }

    // Usado pelo jackson para serializar
    public Followers()
    {
        super();
    }

    public Map<String, Map<Integer, Post>> getSubscriptions_data() {
        return subscriptions_data;
    }

    public void setSubscriptions_data(Map<String, Map<Integer, Post>> followings) {
        this.subscriptions_data = followings;
    }


    public void entry() throws SpreadException {

        SpreadGroup g;

        for (Map.Entry <String, Map<Integer, Post>> entry : this.subscriptions_data.entrySet()) {
            g = new SpreadGroup();
            g.join(this.conn, "Group" + entry.getKey());
            this.subscriptions_groups.put(entry.getKey(), g);
        }
    }

    public void follow(){
        int done = 0;
        String username = null;
        SpreadGroup g = null;

        while (done == 0){
            System.out.println("Username: ");
            done = 1;

            try {
                username = this.in.readLine();
                g = new SpreadGroup();
                g.join(this.conn, "Group" + username);
            }
            catch (IOException | SpreadException e){
                System.out.println("Try again.");
                done = 0;
            }
        }

        this.subscriptions_data.put(username, new HashMap<>());
        this.subscriptions_groups.put(username, g);
    }

    public void unfollow(){
        int done = 0;
        String username;

        while (done == 0){
            System.out.println("Username: ");
            done = 1;

            try {
                username = this.in.readLine();

                if(this.subscriptions_groups.get(username) == null){
                    System.out.println("Não Segue O Utilizador Indicado, Tente Novamente!");
                    done = 0;
                }
                else {
                    this.subscriptions_groups.get(username).leave();
                    this.subscriptions_groups.remove(username);
                    this.subscriptions_data.remove(username);
                }
            }
            catch (IOException | SpreadException e){
                System.out.println("Erro!");
                done = 0;
            }
        }
    }

    public void update_post(String following, List<Post> posts){
        Map<Integer, Post> m = this.subscriptions_data.get(following);

        if (m == null) {
            this.subscriptions_data.put(following, new HashMap<>());
        }

        for (Post p : posts){
            this.subscriptions_data.get(following).put(p.getId_post(), p);

            System.out.println("\n\n****************** POST RECEIVED ******************\n");
            System.out.println("FROM: " + following);
            System.out.println("CONTENT: " + p.getText());
            System.out.println("DATE : " + p.getDate().getTime().toString());
        }
    }


    public void update_posts(String following, List<Post> posts){
        Map<Integer, Post> m = this.subscriptions_data.get(following);
        boolean self_post = true;

        if (m != null){
            self_post = false;
        }

        for(Post p : posts) {
            if (!self_post) {
                m.put(p.getId_post(), p);
            }

            if (this.n_posts_received < this.subscriptions_data.size()) {
                this.posts_received.add(p);
            }
        }

        if (!self_post){
            this.n_posts_received++;
            this.subscriptions_data.put(following, m);
        }

        if (this.n_posts_received == this.subscriptions_data.size()){
            this.n_posts_received++;
            List<Post> result = this.posts_received.stream().sorted(Comparator.comparing(Post::getDate)).
                    collect(Collectors.toList());

            for (Post p : result){
                System.out.println("****************** POST RECEIVED ******************");
                System.out.println("FROM: " + p.getUsername());
                System.out.println("CONTENT: " + p.getText());
                System.out.println("DATE : " + p.getDate().getTime().toString());
            }
        }
    }


    public void logout() throws SpreadException {
        for (SpreadGroup sg : this.subscriptions_groups.values()){
            sg.leave();
        }
    }


    public void get_timeline() throws SpreadException {
        Message msg;
        for (Map.Entry <String, Map<Integer, Post>> entry : this.subscriptions_data.entrySet()) {
            msg = new Message();
            msg.setType("REQUEST");
            msg.setFollowing(entry.getKey());
            msg.setLast_post_ID(find_last_post_ID(entry.getKey()));

            SpreadMessage message = new SpreadMessage();

            message.setData(this.seri.encode(msg));
            message.addGroup("Group" + entry.getKey());
            message.setCausal();
            message.setReliable();

            this.conn.multicast(message);
        }
    }


    private int find_last_post_ID(String username){
        if (this.subscriptions_data.get(username).size() == 0) {
            return 0;
        }
        else {
            return Collections.max(this.subscriptions_data.get(username).keySet()) + 1;
        }
    }


    public List<Post> get_posts(String following, int last_post_id){
        List<Post> response = new ArrayList<>();

        for (Map.Entry<Integer, Post> e : this.subscriptions_data.get(following).entrySet()){
            if (e.getValue().getId_post() >= last_post_id){
                response.add(e.getValue());
            }
        }

        return response;
    }


    public void send_message(Message m, String group) {
        SpreadMessage msg = new SpreadMessage();

        msg.setData(this.seri.encode(m));
        msg.addGroup(group);
        msg.setCausal();
        msg.setReliable();

        try {
            this.conn.multicast(msg);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

}