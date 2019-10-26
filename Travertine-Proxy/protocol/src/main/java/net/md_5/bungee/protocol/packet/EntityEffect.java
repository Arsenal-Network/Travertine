package net.md_5.bungee.protocol.packet;

import io.github.waterfallmc.travertine.protocol.MultiVersionPacketV17;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.ProtocolConstants;

public class EntityEffect extends MultiVersionPacketV17 {
    private int entityId;
    private int effectId;
    private int amplifier;
    private int duration;
    private boolean hideParticles;

    protected void v17Read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.entityId = buf.readInt();
        this.effectId = buf.readUnsignedByte();
        this.amplifier = buf.readUnsignedByte();
        this.duration = buf.readShort();
    }

    public void read(ByteBuf buf) {
        this.entityId = readVarInt(buf);
        this.effectId = buf.readUnsignedByte();
        this.amplifier = buf.readUnsignedByte();
        this.duration = readVarInt(buf);
        this.hideParticles = buf.readBoolean();
    }

    protected void v17Write(ByteBuf buf) {
        buf.writeInt(this.effectId);
        buf.writeByte(this.effectId);
        buf.writeByte(this.amplifier);
        buf.writeShort(this.duration);
    }

    public void write(ByteBuf buf) {
        writeVarInt(this.entityId, buf);
        buf.writeByte(this.effectId);
        buf.writeByte(this.amplifier);
        writeVarInt(this.duration, buf);
        buf.writeBoolean(this.hideParticles);
    }

    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public int getEffectId() {
        return this.effectId;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public int getDuration() {
        return this.duration;
    }

    public boolean isHideParticles() {
        return this.hideParticles;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public void setEffectId(int effectId) {
        this.effectId = effectId;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setHideParticles(boolean hideParticles) {
        this.hideParticles = hideParticles;
    }

    public String toString() {
        return "EntityEffect(entityId=" + this.getEntityId() + ", effectId=" + this.getEffectId() + ", amplifier=" + this.getAmplifier() + ", duration=" + this.getDuration() + ", hideParticles=" + this.isHideParticles() + ")";
    }

    public EntityEffect() {
    }

    public EntityEffect(int entityId, int effectId, int amplifier, int duration, boolean hideParticles) {
        this.entityId = entityId;
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.duration = duration;
        this.hideParticles = hideParticles;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof EntityEffect)) {
            return false;
        } else {
            EntityEffect other = (EntityEffect)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getEntityId() != other.getEntityId()) {
                return false;
            } else if (this.getEffectId() != other.getEffectId()) {
                return false;
            } else if (this.getAmplifier() != other.getAmplifier()) {
                return false;
            } else if (this.getDuration() != other.getDuration()) {
                return false;
            } else {
                return this.isHideParticles() == other.isHideParticles();
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof EntityEffect;
    }

    public int hashCode() {
        int result = 1 * 59 + this.getEntityId();
        result = result * 59 + this.getEffectId();
        result = result * 59 + this.getAmplifier();
        result = result * 59 + this.getDuration();
        result = result * 59 + (this.isHideParticles() ? 79 : 97);
        return result;
    }
}
