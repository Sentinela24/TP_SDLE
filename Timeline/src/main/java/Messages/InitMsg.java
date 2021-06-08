package Messages;

import java.io.Serializable;
import java.util.List;

public class InitMsg implements Serializable {

    private String username;
    private Integer id;

    private boolean isSuper;
    //private Object obj;
    private String addr;
    private List<String> become;


    //1st client msg
    public InitMsg(String username, Integer id) {
        this.username = username;
        this.id = id;
    }

    // bs server resp
    public InitMsg(boolean isSuper, String addr) {
        this.isSuper = isSuper;
        this.addr = addr;
    }

    // alt bs server resp
    public InitMsg(boolean isSuper, String username, String addr) {
        this.isSuper = isSuper;
        this.username = username;
        this.addr = addr;
    }

    public InitMsg(String username, List<String> become) {
        this.username = username;
        this.become = become;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isSuper() {
        return isSuper;
    }

    public void setSuper(boolean aSuper) {
        isSuper = aSuper;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public List<String> getBecome() {
        return become;
    }

    public void setBecome(List<String> become) {
        this.become = become;
    }
}
