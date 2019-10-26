package io.github.waterfallmc.travertine.protocol;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

public abstract class MultiVersionPacketV17 extends DefinedPacket {
    public MultiVersionPacketV17() {
    }

    protected void v17Read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.v17Read(buf);
    }

    public void read0(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        switch(protocolVersion) {
            case 4:
            case 5:
                this.v17Read(buf, direction, protocolVersion);
                break;
            default:
                this.read(buf, direction, protocolVersion);
        }

    }

    protected void v17Write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.v17Write(buf);
    }

    public void write0(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        switch(protocolVersion) {
            case 4:
            case 5:
                this.v17Write(buf, direction, protocolVersion);
                break;
            default:
                this.write(buf, direction, protocolVersion);
        }

    }

    protected void v17Read(ByteBuf buf) {
        throw new UnsupportedOperationException("Packet must implement read method");
    }

    protected void v17Write(ByteBuf buf) {
        throw new UnsupportedOperationException("Packet must implement write method");
    }

    public static void v17writeArray(byte[] b, ByteBuf buf, boolean allowExtended) {
        if (allowExtended) {
            Preconditions.checkArgument(b.length <= 2097050, "Cannot send array longer than 2097050 (got %s bytes)", b.length);
        } else {
            Preconditions.checkArgument(b.length <= 32767, "Cannot send array longer than Short.MAX_VALUE (got %s bytes)", b.length);
        }

        writeVarShort(buf, b.length);
        buf.writeBytes(b);
    }

    public static byte[] v17readArray(ByteBuf buf) {
        int len = readVarShort(buf);
        Preconditions.checkArgument(len <= 2097050, "Cannot receive array longer than 2097050 (got %s bytes)", len);
        byte[] ret = new byte[len];
        buf.readBytes(ret);
        return ret;
    }
}
