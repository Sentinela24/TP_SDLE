import spread.SpreadException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class UserMain {
    public static void main(String[] args) throws IOException, SpreadException, ExecutionException, InterruptedException {

        //User user = new User(args[0]);
        //user.menu();

        NewUser nUser = new NewUser(args[0]);
        nUser.initial_menu();

    }
}
