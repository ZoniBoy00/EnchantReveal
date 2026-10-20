package fi.zoniboy.enchantreveal.client;

import fi.zoniboy.enchantreveal.wire.Protocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.io.IOException;

final class Packets {
    record Request(Protocol.Request value) implements CustomPacketPayload {
        static final Type<Request> TYPE = new Type<>(Identifier.parse(Protocol.REQUEST));
        static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buf, packet) -> buf.writeBytes(Protocol.encodeRequest(packet.value())),
                buf -> {
                    if (buf.readableBytes() != Protocol.REQUEST_BYTES) throw new IllegalArgumentException("Invalid request length");
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    try { return new Request(Protocol.decodeRequest(bytes)); }
                    catch (IOException e) { throw new IllegalArgumentException("Invalid preview request", e); }
                });
        @Override public Type<Request> type() { return TYPE; }
    }

    record Preview(Protocol.Preview value) implements CustomPacketPayload {
        static final Type<Preview> TYPE = new Type<>(Identifier.parse(Protocol.PREVIEW));
        static final StreamCodec<RegistryFriendlyByteBuf, Preview> CODEC = StreamCodec.of(
                (buf, packet) -> buf.writeBytes(Protocol.encodePreview(packet.value())),
                buf -> {
                    if (buf.readableBytes() > Protocol.MAX_BYTES) {
                        buf.skipBytes(buf.readableBytes());
                        return new Preview(null);
                    }
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    try { return new Preview(Protocol.decodePreview(bytes)); }
                    catch (IOException e) { return new Preview(null); }
                });
        @Override public Type<Preview> type() { return TYPE; }
    }
}
