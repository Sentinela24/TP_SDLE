import io.atomix.utils.net.Address;

import java.time.LocalDateTime;
import java.util.List;

public class Message {
    private int value;
    private String type;
    private String pass;
    private List<Post> posts;
    private int last_post_ID;

    private String super_peer;
    private String super_peer_ID;

    private boolean status;
    private String following;

    //
    private String cpu;
    private String boot;
    private String addr;


    public Message() {
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public int getLast_post_ID() {
        return last_post_ID;
    }

    public void setLast_post_ID(int last_post_ID) {
        this.last_post_ID = last_post_ID;
    }

    public String getSuper_peer() {
        return super_peer;
    }

    public void setSuper_peer(String super_peer) {
        this.super_peer = super_peer;
    }

    public String getSuper_peer_ID() {
        return super_peer_ID;
    }

    public void setSuper_peer_ID(String super_peer_ID) {
        this.super_peer_ID = super_peer_ID;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getFollowing() {
        return following;
    }

    public void setFollowing(String following) {
        this.following = following;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public String getBoot() {
        return boot;
    }

    public void setBoot(String boot) {
        this.boot = boot;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public Message(String type, String cpu, String boot, String addr) {
        this.type = type;
        this.cpu = cpu;
        this.boot = boot;
        this.addr = addr;
    }

    /*
    public Message(){
        value = 0;
    }
    public void increment(int val){
        this.value += val;
    }
    public int check(){
        return this.value;
    }
     */
}