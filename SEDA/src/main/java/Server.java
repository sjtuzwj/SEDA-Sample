import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
//https://www.cnblogs.com/gmhappy/p/11864094.html
public class Server {
    private Selector selector;
    public void start() throws IOException {
        // 打开服务器套接字通道
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // 服务器配置为非阻塞
        ssc.configureBlocking(false);
        // 进行服务的绑定
        ssc.bind(new InetSocketAddress("localhost", 8001));

        // 通过open()方法找到Selector
        selector = Selector.open();
        // 注册到selector，等待连接
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    // Clear out our read buffer so it's ready for new data
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    // Attempt to read off the channel
                    int numRead;
                    try {
                        numRead = socketChannel.read(readBuffer);
                    } catch (IOException ex) {
                        // The remote forcibly closed the connection, cancel
                        // the selection key and close the channel.
                        key.cancel();
                        socketChannel.close();
                        return;
                    }
                    String str = new String(readBuffer.array(), 0, numRead);
                    Event event = new Event(key, Event.Type.Read);
                    event.Packet = str;
                    System.out.println("Sync "+str);
                    StageMap.getInstance().stageMap.get("read").Enqueue(event);
                }
                keyIterator.remove(); //该事件已经处理，可以丢弃
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("a new client connected "+clientChannel.getRemoteAddress());
    }

    public static void main(String[] args) throws IOException {
        System.out.println("server started...");
        new AppStage();
        new WriteStage();
        new ReadStage();
        new Server().start();
    }
}
