package net.md_5.bungee.netty;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.protocol.KickStringWriter;
import net.md_5.bungee.protocol.LegacyDecoder;
import net.md_5.bungee.protocol.MinecraftDecoder;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.Varint21FrameDecoder;
import net.md_5.bungee.protocol.Varint21LengthFieldPrepender;

public class PipelineUtils
{

    public static final AttributeKey<ListenerInfo> LISTENER = AttributeKey.newInstance("ListerInfo");
    public static final AttributeKey<UserConnection> USER = AttributeKey.newInstance("User");
    public static final AttributeKey<BungeeServerInfo> TARGET = AttributeKey.newInstance("Target");
    public static final ChannelInitializer<Channel> SERVER_CHILD = new ChannelInitializer<Channel>()
    {
        @Override
        protected void initChannel(Channel ch) throws Exception
        {
            if ( BungeeCord.getInstance().getConnectionThrottle() != null && BungeeCord.getInstance().getConnectionThrottle().throttle( ( (InetSocketAddress) ch.remoteAddress() ).getAddress() ) )
            {
                ch.close();
                return;
            }

            ListenerInfo listener = ch.attr( LISTENER ).get();

            BASE.initChannel( ch );
            ch.pipeline().addBefore( FRAME_DECODER, LEGACY_DECODER, new LegacyDecoder() );
            ch.pipeline().addAfter( FRAME_DECODER, PACKET_DECODER, new MinecraftDecoder( Protocol.HANDSHAKE, true, ProxyServer.getInstance().getProtocolVersion() ) );
            ch.pipeline().addAfter( FRAME_PREPENDER, PACKET_ENCODER, new MinecraftEncoder( Protocol.HANDSHAKE, true, ProxyServer.getInstance().getProtocolVersion() ) );
            ch.pipeline().addBefore( FRAME_PREPENDER, LEGACY_KICKER, legacyKicker );
            ch.pipeline().get( HandlerBoss.class ).setHandler( new InitialHandler( BungeeCord.getInstance(), listener ) );

            if ( listener.isProxyProtocol() )
            {
                ch.pipeline().addFirst( new HAProxyMessageDecoder() );
            }
        }
    };
    public static final Base BASE = new Base();
    private static final KickStringWriter legacyKicker = new KickStringWriter();
    private static final Varint21LengthFieldPrepender framePrepender = new Varint21LengthFieldPrepender();
    public static final String TIMEOUT_HANDLER = "timeout";
    public static final String PACKET_DECODER = "packet-decoder";
    public static final String PACKET_ENCODER = "packet-encoder";
    public static final String BOSS_HANDLER = "inbound-boss";
    public static final String ENCRYPT_HANDLER = "encrypt";
    public static final String DECRYPT_HANDLER = "decrypt";
    public static final String FRAME_DECODER = "frame-decoder";
    public static final String FRAME_PREPENDER = "frame-prepender";
    public static final String LEGACY_DECODER = "legacy-decoder";
    public static final String LEGACY_KICKER = "legacy-kick";

    private static boolean epoll;

    static
    {
        if ( !PlatformDependent.isWindows() && Boolean.parseBoolean( System.getProperty( "bungee.epoll", "true" ) ) )
        {
            ProxyServer.getInstance().getLogger().info( "Not on Windows, attempting to use enhanced EpollEventLoop" );

            if ( epoll = Epoll.isAvailable() )
            {
                ProxyServer.getInstance().getLogger().info( "Epoll is working, utilising it!" );
            } else
            {
                ProxyServer.getInstance().getLogger().log( Level.WARNING, "Epoll is not working, falling back to NIO: {0}", Util.exception( Epoll.unavailabilityCause() ) );
            }
        }
    }

    public static EventLoopGroup newEventLoopGroup(int threads, ThreadFactory factory)
    {
        return epoll ? new EpollEventLoopGroup( threads, factory ) : new NioEventLoopGroup( threads, factory );
    }

    public static Class<? extends ServerChannel> getServerChannel()
    {
        return epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    public static Class<? extends Channel> getChannel()
    {
        return epoll ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    public static Class<? extends io.netty.channel.socket.DatagramChannel> getDatagramChannel() // Waterfall - change to DatagramChannel
    {
        return epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
    }

    private static final int LOW_MARK = Integer.getInteger( "net.md_5.bungee.low_mark", 2 << 18 ); // 0.5 mb
    private static final int HIGH_MARK = Integer.getInteger( "net.md_5.bungee.high_mark", 2 << 20 ); // 2 mb
    private static final WriteBufferWaterMark MARK = new WriteBufferWaterMark( LOW_MARK, HIGH_MARK );

    public static final class Base extends ChannelInitializer<Channel>
    {

        @Override
        public void initChannel(Channel ch) throws Exception
        {
            try
            {
                ch.config().setOption( ChannelOption.IP_TOS, 0x18 );
            } catch ( ChannelException ex )
            {
                // IP_TOS is not supported (Windows XP / Windows Server 2003)
            }
            ch.config().setOption( ChannelOption.TCP_NODELAY, true );
            ch.config().setAllocator( PooledByteBufAllocator.DEFAULT );
            ch.config().setWriteBufferWaterMark( MARK );

            ch.pipeline().addLast( TIMEOUT_HANDLER, new ReadTimeoutHandler( BungeeCord.getInstance().config.getTimeout(), TimeUnit.MILLISECONDS ) );
            ch.pipeline().addLast( FRAME_DECODER, new Varint21FrameDecoder( BungeeCord.getInstance().getConfig().isAllowEmptyPackets()) ); // Waterfall
            ch.pipeline().addLast( FRAME_PREPENDER, framePrepender );

            ch.pipeline().addLast( BOSS_HANDLER, new HandlerBoss() );
        }
    }
}
