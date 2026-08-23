package net.rebel459.music_and_melody.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record StructureMusicPacket(Set<Identifier> structures, Set<Identifier> tags) implements CustomPacketPayload {

        public static final Type<StructureMusicPacket> TYPE = new Type<>(MusicAndMelody.id("structure_music"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureMusicPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC),
                    StructureMusicPacket::structures,
                    ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC),
                    StructureMusicPacket::tags,
                    StructureMusicPacket::new
            );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }