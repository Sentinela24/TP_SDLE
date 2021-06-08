import spread.SpreadException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public class UserMain {
    public static void main(String args[]) throws IOException, SpreadException, ExecutionException, InterruptedException, NoSuchAlgorithmException {

        NewUser nUser = new NewUser();
        nUser.initial_menu();

    }
}
