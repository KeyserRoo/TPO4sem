


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Server {
    private final String host;
    private final int port;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private Thread serverThread;
    private final Map<SocketChannel, StringBuilder> clientLogs = new LinkedHashMap<>();
    private final StringBuilder serverLog;

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
        this.serverLog = new StringBuilder();
    }

    public void startServer() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new java.net.InetSocketAddress(host, port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        serverThread = new Thread(this::runServer);
        serverThread.start();
    }

    private void runServer() {
        try {
            while (!serverThread.isInterrupted()) {
                if (selector.isOpen()){
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isAcceptable()) {
                            SocketChannel clientChannel = serverSocketChannel.accept();
                            clientChannel.configureBlocking(false);

                            if (selector.isOpen()) {
                                clientChannel.register(selector, SelectionKey.OP_READ);
                            }

                            clientLogs.put(clientChannel, new StringBuilder());
                            continue;
                        }

                        if (key.isReadable()) {
                            String req = readReq((SocketChannel) key.channel());
                            readClient(req, (SocketChannel) key.channel());
                        }
                    }

                }
            }
            if (selector.isOpen()) {
                selector.close();
            }
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readClient(String req, SocketChannel clientChannel) throws IOException {
        StringBuilder log = clientLogs.get(clientChannel);
        String login;
        String[] parts = req.split("\n");
        login = parts[0];

        if (log.length() == 0) {
            log.append("=== ").append(login).append(" log start ===\n");
        }

        String pattern = "\\d{4}-\\d{2}-\\d{2} \\d{4}-\\d{2}-\\d{2}";
        String pattern1 = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}";

        if (Pattern.matches(pattern, parts[1]) || Pattern.matches(pattern1, parts[1])) {
            String response = Time.passed(parts[1].split(" ")[0], parts[1].split(" ")[1]);
            log.append("Request: ").append(parts[1]).append("\n");
            appendToServerLog(login, "request at", parts[1]);
            log.append("Result:\n");
            log.append(response);

            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
            clientChannel.write(responseBuffer);
        } else if (parts[1].contains("bye and log transfer")) {
            log.append("logged out").append("\n");
            appendToServerLog(login, "logged out at");
            log.append("=== ").append(login).append(" log end ===\n");

            ByteBuffer responseBuffer = ByteBuffer.wrap(clientLogs.get(clientChannel).toString().getBytes(StandardCharsets.UTF_8));
            clientChannel.write(responseBuffer);

            clientChannel.close();
            clientChannel.socket().close();
        }else if (parts[1].contains("bye")) {
            log.append("logged out").append("\n");
            appendToServerLog(login, "logged out at");
            log.append("=== ").append(login).append(" log end ===\n");
            ByteBuffer responseBuffer = ByteBuffer.wrap("logged out".getBytes(StandardCharsets.UTF_8));
            clientChannel.write(responseBuffer);

            clientChannel.close();
            clientChannel.socket().close();
        } else if (parts[1].contains("login")) {
            log.append("logged in\n");
            appendToServerLog(login, "logged in at");
            ByteBuffer responseBuffer = ByteBuffer.wrap("logged in".getBytes(StandardCharsets.UTF_8));
            clientChannel.write(responseBuffer);
        }else{
            ByteBuffer responseBuffer = ByteBuffer.wrap(" ".getBytes(StandardCharsets.UTF_8));
            clientChannel.write(responseBuffer);
        }
    }

    private String readReq(SocketChannel socketChannel) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        Charset charset = StandardCharsets.UTF_8;

        while (true) {
            int n = socketChannel.read(buffer);
            if (n > 0) {
                buffer.flip();
                CharBuffer cbuf = charset.decode(buffer);
                while (cbuf.hasRemaining()) {
                    char c = cbuf.get();
                    stringBuilder.append(c);
                }
                return stringBuilder.toString();
            }
        }
    }

    private void appendToServerLog(String login, String action) {
        LocalDateTime curr = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        String formattedTime = curr.format(formatter);
        serverLog.append(login).append(" ").append(action).append(" ").append(formattedTime);
        serverLog.append("\n");
    }

    private void appendToServerLog(String login, String action, String req) {
        LocalDateTime curr = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        String formattedTime = curr.format(formatter);
        serverLog.append(login).append(" ").append(action).append(" ").append(formattedTime).append(": ").append('"')
                .append(req).append('"');
        serverLog.append("\n");
    }

    public String getServerLog() {
        return serverLog.toString();
    }

    public void stopServer() {
        serverThread.interrupt();
    }
}

