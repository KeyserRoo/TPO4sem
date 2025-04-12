import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChatClient {

	private final String host;
	private final int port;
	private final String id;
	private final List<String> chatView = new ArrayList<>();
	private SocketChannel channel;

	public ChatClient(String host, int port, String id) {
		this.host = host;
		this.port = port;
		this.id = id;
	}

	public void login() {
		chatView.clear();
		try {
			connect();
			String message = id + " " + SessionAction.LOGIN.message() + "\n";
			ByteBuffer encoded = StandardCharsets.UTF_8.encode(message);
			channel.write(encoded);
			readMessages();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void connect() throws IOException {
		channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(host, port));
		while (!channel.finishConnect()) {
		}
	}

	private void readMessages() throws IOException {
		Thread receiverThread = new Thread(() -> {
			ByteBuffer allocate = ByteBuffer.allocate(1024);
			try {
				while (channel.isOpen()) {
					allocate.clear();
					int read = channel.read(allocate);
					if (read == -1) {
						break;
					} else if (read > 0) {
						allocate.flip();
						String decoded = StandardCharsets.UTF_8.decode(allocate).toString();
						for (String s : decoded.split("\n")) {
							logChat(s);
						}
					}
				}
			} catch (IOException e) {
				try {
					channel.socket().close();
					channel.close();
				} catch (IOException ignored) {
				}
			}
		});
		receiverThread.start();
	}

	public void logout() {
		String message = id + " " + SessionAction.LOGOUT.message() + "\n";
		ByteBuffer encoded = StandardCharsets.UTF_8.encode(message);
		try {
			channel.write(encoded);
			Thread.sleep(10);
			logChat(message);
			channel.socket().close();
			channel.close();
		} catch (Exception ignored) {
		}
	}

	public void send(String req) {
		try {
			ByteBuffer buffer = StandardCharsets.UTF_8.encode(id + ": " + req + "\n");
			channel.write(buffer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void logChat(String msg) {
		chatView.add(msg + "\n");
	}

	public String getChatView() {
		StringBuilder sb = new StringBuilder("=== " + id + " chat view\n");
		for (String log : chatView)
			sb.append(log);
		return sb.toString();
	}

}
