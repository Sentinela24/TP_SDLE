import spread.SpreadException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public class UserMain {
    public static void main(String args[]) throws IOException, SpreadException, ExecutionException, InterruptedException, NoSuchAlgorithmException {

        //User user = new User(args[0]);
        //user.menu();

        //NewUser nUser = new NewUser(args[0], args[1]);
        //NewUser nUser = new NewUser(args[1]);
        NewUser nUser = new NewUser();
        //NewUser nUser2 = new NewUser("cv1", "15551");

        nUser.initial_menu();

    }
}
