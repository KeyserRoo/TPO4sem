import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<Long> {

	private final ChatClient chatClient;

	public ChatClientTask(Callable<Long> callable, ChatClient chatClient) {
		super(callable);
		this.chatClient = chatClient;
	}

	public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
		Callable<Long> callable = () -> {
			long start = System.currentTimeMillis();
			c.login();
			sleep(wait);
			for (String message : msgs) {
				c.send(message);
				sleep(wait);
			}
			c.logout();
			sleep(wait);
			long end = System.currentTimeMillis();
			return end - start;
		};
		return new ChatClientTask(callable, c);
	}

	private static void sleep(long millis) {
		if (millis <= 0)
			return;
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exception) {
			System.err.println(Thread.currentThread()+" sleep was interrupted");
			exception.printStackTrace();
		}
	}

	public ChatClient getClient() {
		return chatClient;
	}
}
