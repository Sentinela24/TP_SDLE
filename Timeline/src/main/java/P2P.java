import Peer.Peer;
import spread.SpreadException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public class P2P {
    public static void main(String args[]) throws IOException, SpreadException, ExecutionException, InterruptedException, NoSuchAlgorithmException {

        Peer nUser = new Peer();
        nUser.initial_menu();

    }
}
