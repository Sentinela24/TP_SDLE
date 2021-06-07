public class Parser {

    private Followers followers;
    private Following following;

    public Parser(Followers followers, Following following) {
        this.followers = followers;
        this.following = following;
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

    public void setFollowing(Following following) {
        this.following = following;
    }
}

