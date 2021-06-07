import io.atomix.utils.serializer.Serializer;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Following implements Serializable {
    private transient NewUser user;
    private transient String username;
    private transient SpreadConnection conn;
    private Map<Integer, Post> myPosts;
    private transient SpreadGroup myGroup;
    private transient BufferedReader in;
    private transient Serializer seri;

    public Following(NewUser user, String username, SpreadConnection conn, BufferedReader in, Serializer seri) {
        this.user = user;
        this.username = username;
        this.conn = conn;
        this.in = in;
        this.seri = seri;

        this.myPosts = new HashMap<>();
        this.myGroup = null;
    }

    public String getUsername() {
        return username;
    }

    public Map<Integer, Post> getMyPosts() {
        return myPosts;
    }

    public void setMyPosts(Map<Integer, Post> myPosts) {
        this.myPosts = myPosts;
    }

    public void entry() throws SpreadException {
        this.myGroup = new SpreadGroup();
        this.myGroup.join(this.conn, "Group" + this.username);
    }

    private int getPostID(){
        int res;

        if (this.myPosts.isEmpty()){
            res = 0;
        }
        else {
            res = this.myPosts.keySet().size();
        }

        return res;
    }

    public void post() throws IOException, SpreadException {
        System.out.println("Write post here: ");
        String text = this.in.readLine();
        Calendar date = Calendar.getInstance();
        int post_id = getPostID();
        Post p = new Post(post_id, this.username, date, text);

        this.myPosts.put(post_id, p);

        send_post(p);
    }

    public void send_post(Post p){
        Message msg = new Message();

        msg.setType("POST");

        List<Post> posts = new ArrayList<>();
        posts.add(p);
        msg.setPosts(posts);

        msg.setFollowing(this.username);

        //SEND SPREAD MESSAGE TO ALL FOLLOWERS
        SpreadMessage spread_msg = new SpreadMessage();

        spread_msg.setData(this.seri.encode(msg));
        spread_msg.addGroup("Group" + this.username);
        spread_msg.setCausal();
        spread_msg.setReliable();

        try {
            this.conn.multicast(spread_msg);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    public List<Post> get_posts(int lastPostId) {
        List<Post> response = new ArrayList<>();

        for (Map.Entry<Integer, Post> e : this.myPosts.entrySet()){
            if (e.getValue().getId_post() >= lastPostId){
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

    public void logout() throws SpreadException {
        this.myGroup.leave();
    }
}
