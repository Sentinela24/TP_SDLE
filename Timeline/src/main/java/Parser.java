import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class Parser {

    private Followers followers;
    @JsonProperty("following")
    private Following following;

    public Parser(Followers followers, Following folllowing) {
        this.followers = followers;
        this.following = folllowing;
    }

    public Parser()
    {
        super();
    }

    public Followers getFollowers() {
        return followers;
    }

    public void setFollowers(Followers followers) {
        this.followers = followers;
    }

    public Following getFollowing() {
        return following;
    }

    public void setFollowing(Following folllowing) {
        this.following = folllowing;
    }
}

