import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChatServer {

	private final String host;
	private final int port;
	private final Collection<String> logs = new ArrayList<>();;
	private final Map<String, SocketChannel> loggedInUsers = new ConcurrentHashMap<>();
	private Selector selector;

	private ExecutorService executorService;
	private Future<?> future;

	public ChatServer(String host, int port) {
		this.host = host;
		this.port = port;
		
		this.executorService = Executors.newSingleThreadExecutor();
	}

	public void startServer() {
		Runnable serverTask = () -> {
			try (ServerSocketChannel server = ServerSocketChannel.open()) {
				selector = Selector.open();
				server.socket().bind(new InetSocketAddress(host, port));
				server.configureBlocking(false);
				server.register(selector, SelectionKey.OP_ACCEPT);
				System.out.println("Server started\n");
				
				while (!Thread.currentThread().isInterrupted()) {
					processEvents();
				}
			} catch (IOException ignored) {
			}
		};
		future = executorService.submit(serverTask);
	}

	public void stopServer() {
		future.cancel(true);
		executorService.shutdown();

		System.out.println("Server stopped");
	}

	public String getServerLog() {
		return String.join("\n", logs) + "\n";
	}


	private void processEvents() throws IOException {
		selector.select();
		Set<SelectionKey> selectionKeys = selector.selectedKeys();
		Iterator<SelectionKey> iterator = selectionKeys.iterator();
		while (iterator.hasNext()) {
			SelectionKey key = iterator.next();
			iterator.remove();
			try {
				if (key.isAcceptable()) {
					handleAccept(key, selector);
				} else if (key.isReadable()) {
					readMessage(key, loggedInUsers.values());
				}
			} catch (CancelledKeyException e) {
				key.cancel();
			}
		}
	}

	private void handleAccept(SelectionKey key, Selector selector) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = serverChannel.accept();
		if (channel != null) {
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_READ);
		}
	}

	private ByteBuffer readData(SocketChannel channel) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int bytesRead = channel.read(buffer);
		if (bytesRead == -1) {
			channel.close();
			return null;
		}
		buffer.flip();
		byte[] data = new byte[bytesRead];
		buffer.get(data);
		return ByteBuffer.wrap(data);
	}

	private void parseMessages(ByteBuffer data, SelectionKey key, Collection<SocketChannel> channels) {
		String[] receivedLines = new String(data.array(), StandardCharsets.UTF_8).split("\n");
		for (String receivedString : receivedLines) {
			receivedString = receivedString.trim();

			if (receivedString.endsWith(SessionAction.LOGIN.message())) {
				actionLogin(receivedString, channels, (SocketChannel) key.channel());

			} else if (receivedString.endsWith(SessionAction.LOGOUT.message())) {
				actionLogout(receivedString, channels);

			} else {
				actionContinue(receivedString, channels);
			}
		}
	}

	private void actionLogin(String receivedString, Collection<SocketChannel> channels, SocketChannel clientChannel) {
		String[] split = receivedString.split(" ", 2);
		String id = split[0];
		log(id + " " + SessionAction.LOGIN.message());
		loggedInUsers.put(id, clientChannel);
		broadcast(channels, id + " " + SessionAction.LOGIN.message());
	}

	private void actionLogout(String receivedString, Collection<SocketChannel> channels) {
		String[] split = receivedString.split(" ", 2);
		String id = split[0];
		log(id + " " + SessionAction.LOGOUT.message());
		loggedInUsers.remove(id);
		broadcast(channels, id + " " + SessionAction.LOGOUT.message());
	}

	private void actionContinue(String receivedString, Collection<SocketChannel> channels) {
		String[] split = receivedString.split(":", 2);
		String id = split[0];
		String msg = split[1].trim();
		log(id + ": " + msg);
		broadcast(channels, id + ": " + msg);
	}

	private void readMessage(SelectionKey key, Collection<SocketChannel> channels) {
		SocketChannel channel = (SocketChannel) key.channel();
		try {
			ByteBuffer data = readData(channel);
			if (data != null)
				parseMessages(data, key, channels);
		} catch (IOException e) {
			try {
				channel.close();
			} catch (IOException ignored) {
			}
		}
	}

	private void broadcast(Collection<SocketChannel> channels, String message) {
		ByteBuffer encodedUTF_8 = StandardCharsets.UTF_8.encode(message + "\n");
		for (SocketChannel channel : channels) {
			try {
				channel.write(encodedUTF_8.duplicate());
			} catch (IOException e) {
				try {
					channel.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	private void log(String message) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
		logs.add(LocalTime.now().format(formatter) + " " + message);
	}
}
