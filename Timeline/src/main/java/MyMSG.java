import java.io.Serializable;
import java.time.LocalDateTime;

public class MyMSG implements Serializable {

    private String type;
    private int cpu;
    private LocalDateTime boot;
    private String addr;


    public MyMSG(String type) {
        this.type = type;
    }

    public MyMSG(String type, int cpu) {
        this.type = type;
        this.cpu = cpu;
    }

    public MyMSG(String type, int cpu, LocalDateTime dt) {
        this.type = type;
        this.cpu = cpu;
        this.boot = dt;
    }

    public MyMSG(String type, int cpu, LocalDateTime boot, String addr) {
        this.type = type;
        this.cpu = cpu;
        this.boot = boot;
        this.addr = addr;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public LocalDateTime getBoot() {
        return boot;
    }

    public void setBoot(LocalDateTime boot) {
        this.boot = boot;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }
}
