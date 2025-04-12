import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Client {
    private final String host;
    private final int port;
    private final String id;
    private SocketChannel socketChannel;

    public Client(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public void connect() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.configureBlocking(false);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public String send(String request) {
        try {
            if (!socketChannel.isConnected()) {
                connect();
            }
            request = id + '\n' + request + '\n';
            byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);

            ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);

            socketChannel.write(requestBuffer);

            ByteBuffer responseBuffer = ByteBuffer.allocate(1024);
            StringBuilder response = new StringBuilder();
            Charset charset = StandardCharsets.UTF_8;

            while (true) {
                int n = socketChannel.read(responseBuffer);
                if (n > 0) {
                    responseBuffer.flip();
                    CharBuffer cbuf = charset.decode(responseBuffer);
                    while (cbuf.hasRemaining()) {
                        char c = cbuf.get();
                        response.append(c);
                    }
                    break;
                }
            }
            return response.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error sending request", e);
        }
    }

    public String getId() {
        return id;
    }
}
