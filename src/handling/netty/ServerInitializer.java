package handling.netty;

/**
 *
 * @author o黯淡o
 */
import handling.MapleServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private int world;
    private int channels;

    public ServerInitializer(int world, int channels) {
        this.world = world;
        this.channels = channels;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipe = channel.pipeline();
        pipe.addLast("decoder", new MaplePacketDecoder()); // decodes the packet
        pipe.addLast("encoder", new MaplePacketEncoder()); //encodes the packet
        pipe.addLast("handler", new MapleServerHandler(world, channels));
    }

}
