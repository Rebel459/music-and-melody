package net.rebel459.music_and_melody.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rebel459.music_and_melody.MusicAndMelody;

public record CombatMusicPacket(boolean playerTrackedByMob) implements CustomPacketPayload {

    public static final Type<CombatMusicPacket> TYPE = new Type<>(MusicAndMelody.id("combat_music"));
    public static final StreamCodec<FriendlyByteBuf, CombatMusicPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CombatMusicPacket::playerTrackedByMob,
                    CombatMusicPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
