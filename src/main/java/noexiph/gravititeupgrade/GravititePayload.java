package noexiph.gravititeupgrade;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GravititePayload() implements CustomPayload {
    public static final CustomPayload.Id<GravititePayload> ID = new CustomPayload.Id<>(Identifier.of("gravititeupgrade", "toggle_flight"));

    // A codec for an empty packet
    public static final PacketCodec<RegistryByteBuf, GravititePayload> CODEC = PacketCodec.unit(new GravititePayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}