import io.atomix.utils.serializer.Serializer;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Followers implements Serializable {
    private transient String username;
    private transient SpreadConnection conn;
    private transient Serializer seri;
    private Map<String, Map<Integer, Post>> followings;
    private transient Map<String, SpreadGroup> followings_groups;
    private transient BufferedReader in;
    private int n_posts_received;
    private List<Post> posts_received;

    public Followers(String username, SpreadConnection conn, Serializer seri, BufferedReader in) {
        this.username = username;
        this.conn = conn;
        this.seri = seri;
        this.in = in;

        this.followings = new HashMap<>();
        this.followings_groups = new HashMap<>();
        this.n_posts_received = 0;
        this.posts_received = new ArrayList<>();
    }

    public Map<String, Map<Integer, Post>> getFollowings() {
        return followings;
    }

    public void setFollowings(Map<String, Map<Integer, Post>> followings) {
        this.followings = followings;
    }

    public void entry() throws SpreadException {
        SpreadGroup g;

        for (Map.Entry <String, Map<Integer, Post>> entry : this.followings.entrySet()) {
            g = new SpreadGroup();
            g.join(this.conn, "Group" + entry.getKey());
            this.followings_groups.put(entry.getKey(), g);
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

        this.followings.put(username, new HashMap<>());
        this.followings_groups.put(username, g);
    }

    public void unfollow(){
        int done = 0;
        String username;
        SpreadGroup g = null;

        while (done == 0){
            System.out.println("Username: ");
            done = 1;

            try {
                username = this.in.readLine();

                if(this.followings_groups.get(username) == null){
                    done = 0;
                }
                else {
                    this.followings_groups.get(username).leave();
                    this.followings_groups.remove(username);
                    this.followings.remove(username);
                }
            }
            catch (IOException | SpreadException e){
                System.out.println("Try again.");
                done = 0;
            }
        }
    }

    public void update_post(String following, List<Post> posts){
        Map<Integer, Post> m = this.followings.get(following);

        if (m == null) {
            this.followings.put(following, new HashMap<>());
        }

        for (Post p : posts){
            this.followings.get(following).put(p.getId_post(), p);

            System.out.println("\n\n****************** POST RECEIVED ******************\n");
            System.out.println("FROM: " + following);
            System.out.println("CONTENT: " + p.getText());
            System.out.println("DATE : " + p.getDate().getTime().toString());
        }
    }

    public void update_posts(String following, List<Post> posts){
        Map<Integer, Post> m = this.followings.get(following);
        boolean self_post = true;

        if (m != null){
            self_post = false;
        }

        for(Post p : posts) {
            if (!self_post) {
                m.put(p.getId_post(), p);
            }

            if (this.n_posts_received < this.followings.size()) {
                this.posts_received.add(p);
            }
        }

        if (!self_post){
            this.n_posts_received++;
            this.followings.put(following, m);
        }

        if (this.n_posts_received == this.followings.size()){
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
        for (SpreadGroup sg : this.followings_groups.values()){
            sg.leave();
        }
    }

    public void get_timeline() throws SpreadException {
        Message msg;
        for (Map.Entry <String, Map<Integer, Post>> entry : this.followings.entrySet()) {
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
        if (this.followings.get(username).size() == 0) {
            return 0;
        }
        else {
            return Collections.max(this.followings.get(username).keySet()) + 1;
        }
    }

    public List<Post> get_posts(String following, int last_post_id){
        List<Post> response = new ArrayList<>();

        for (Map.Entry<Integer, Post> e : this.followings.get(following).entrySet()){
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
