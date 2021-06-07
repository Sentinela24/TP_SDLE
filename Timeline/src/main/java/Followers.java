import io.atomix.utils.serializer.Serializer;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Followers {

    private String username;
    private SpreadConnection conn;
    private Serializer seri;
    private Map<String, Map<Integer, Post>> followings;
    private Map<String, SpreadGroup> followings_groups;
    private BufferedReader in;
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
                    System.out.println("Não Segue O Utilizador Indicado, Tente Novamente!");
                    done = 0;
                }
                else {
                    this.followings_groups.get(username).leave();
                    this.followings_groups.remove(username);
                    this.followings.remove(username);
                }
            }
            catch (IOException | SpreadException e){
                System.out.println("Erro!");
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
        for(Post p : posts){

            m.put(p.getId_post(), p);

            System.out.println("****************** POST RECEIVED ******************");
            System.out.println("FROM: " + following);
            System.out.println("CONTENT: " + p.getText());
            System.out.println("DATE : " + p.getDate());
        }
    }

    public void logout() throws SpreadException {
        for (SpreadGroup sg : this.followings_groups.values()){
            sg.leave();
        }
    }

}