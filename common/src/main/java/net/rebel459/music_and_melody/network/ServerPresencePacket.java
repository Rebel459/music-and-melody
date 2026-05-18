package net.rebel459.music_and_melody.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rebel459.music_and_melody.MusicAndMelody;

public record ServerPresencePacket() implements CustomPacketPayload {

    public static final Type<ServerPresencePacket> TYPE = new Type<>(MusicAndMelody.id("server_presence"));
    public static final StreamCodec<FriendlyByteBuf, ServerPresencePacket> CODEC = StreamCodec.unit(new ServerPresencePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
