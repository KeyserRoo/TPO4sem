


import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ClientTask extends FutureTask<String> {
    public ClientTask(Callable<String> callable) {
        super(callable);
    }
    public static ClientTask create(Client c, List<String> requests, boolean showResponse) {
        return new ClientTask(() -> {
            c.connect();
            c.send("login " + c.getId());

            for (String message : requests) {
                String response = c.send(message);
                if (showResponse) {
                    System.out.println(response);
                }
            }

            return c.send("bye and log transfer");
        });
    }
}
