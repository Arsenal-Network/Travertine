package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf>
{

    @Setter
    private Protocol protocol;
    private final boolean server;
    @Setter
    private int protocolVersion;
    @Setter
    private boolean supportsForge = false;
    private final boolean allowEmptyPackets; // Waterfall

    public MinecraftDecoder(Protocol protocol, boolean server, int protocolVersion) {
        // Waterfall start
        this(protocol, server, protocolVersion, false);
    }
    public MinecraftDecoder(Protocol protocol, boolean server, int protocolVersion, boolean allowEmptyPackets) {
        // Waterfall end
        this.protocol = protocol;
        this.server = server;
        this.protocolVersion = protocolVersion;
        this.allowEmptyPackets = allowEmptyPackets; // Waterfall
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        Protocol.DirectionData prot = ( server ) ? protocol.TO_SERVER : protocol.TO_CLIENT;
        ByteBuf slice = in.copy(); // Can't slice this one due to EntityMap :(

        Object packetTypeInfo = null;
        try
        {
            // Waterfall start
            if (in.readableBytes() == 0) {
                if (!allowEmptyPackets) throw new BadPacketException("Empty minecraft packet!");
                return;
            }
            // Waterfall end

            int packetId = DefinedPacket.readVarInt( in );
            packetTypeInfo = packetId;

            DefinedPacket packet = prot.createPacket( packetId, protocolVersion, supportsForge );
            if ( packet != null )
            {
                packetTypeInfo = packet.getClass();
                packet.read0( in, prot.getDirection(), protocolVersion );

                if ( in.isReadable() )
                {
                    throw new BadPacketException( "Did not read all bytes from packet " + packet.getClass() + " " + packetId + " Protocol " + protocol + " Direction " + prot.getDirection() );
                }
            } else
            {
                in.skipBytes( in.readableBytes() );
            }

            out.add( new PacketWrapper( packet, slice ) );
            slice = null;
        } catch (BadPacketException | IndexOutOfBoundsException e) {
            final String packetTypeStr;
            if (packetTypeInfo instanceof Integer) {
                packetTypeStr = "id " + Integer.toHexString((Integer) packetTypeInfo);
            } else if (packetTypeInfo instanceof Class) {
                packetTypeStr = "class " + ((Class) packetTypeInfo).getSimpleName();
            } else {
                packetTypeStr = "unknown";
            }
            throw new DecoderException("Error decoding packet " + packetTypeStr + " with contents:\n" + ByteBufUtil.prettyHexDump(slice), e);
        } finally
        {
            if ( slice != null )
            {
                slice.release();
            }
        }
    }
}
