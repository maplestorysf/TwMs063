package handling.netty;

/**
 *
 * @author o黯淡o
 */
import constants.WorldConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ServerConnection {

    private final String ip;
    private final int port;
    private int world = -1;
    private int channels = -1;
    private ServerBootstrap boot;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1); //The initial connection thread where all the new connections go to
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(); //Once the connection thread has finished it will be moved over to this group where the thread will be managed
    private Channel channel;

    public ServerConnection(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public ServerConnection(String ip, int port, int world, int channels) {
        this.ip = ip;
        this.port = port;
        this.world = world;
        this.channels = channels;
    }

    public void run() {
        try {
            boot = new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, WorldConstants.UserLimit)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ServerInitializer(world, channels));
            try {
                channel = boot.bind(ip, port).sync().channel().closeFuture().channel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
//                System.out.println("Listening to port: " + port);
            }
        } catch (Exception e) {
            System.out.printf("Connection to %s failed.", channel == null ? e.toString() : channel.remoteAddress());
            // Shut down all event loops to terminate all threads.
            close();
        }
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
