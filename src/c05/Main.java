import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static void main(String[] args) throws Exception {
    String testFileName = System.getProperty("user.home") + "/ChatTest.txt";
    List<String> test = Files.readAllLines(Paths.get(testFileName));
    String host = test.remove(0);
    int port = Integer.valueOf(test.remove(0));
    ChatServer s = new ChatServer(host, port);
    s.startServer();
    ExecutorService es = Executors.newCachedThreadPool();
    List<ChatClientTask> ctasks = new ArrayList<>();

    for (String line : test) {
      String[] elts = line.split("\t");
      String id = elts[0];
      int wait = Integer.valueOf(elts[1]);
      List<String> msgs = new ArrayList<>();
      for (int i = 2; i < elts.length; i++) msgs.add(elts[i] + ", mówię ja, " +id);
      ChatClient c = new ChatClient(host, port, id);
      ChatClientTask ctask = ChatClientTask.create(c, msgs, wait);
      ctasks.add(ctask);
      es.execute(ctask);
    }
    ctasks.forEach( task -> {
      try {
        task.get();
      } catch (InterruptedException | ExecutionException exc) {
        System.out.println("*** " + exc);
      }
    });
    es.shutdown();
    s.stopServer();

    System.out.println("\n=== Server log ===");
    System.out.println(s.getServerLog());

    ctasks.forEach(t -> System.out.println(t.getClient().getChatView()));
  }
}
