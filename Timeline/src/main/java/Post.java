import java.io.Serializable;
import java.util.Calendar;

public class Post implements Serializable {
    private int id_post;
    private String username;
    private Calendar date;
    private String text;

    public Post(int id_post, String username, Calendar date, String text) {
        this.id_post = id_post;
        this.username = username;
        this.date = date;
        this.text = text;
    }

    public int getId_post() {
        return id_post;
    }

    public void setId_post(int id_post) {
        this.id_post = id_post;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
