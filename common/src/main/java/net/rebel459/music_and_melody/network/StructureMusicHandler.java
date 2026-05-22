package net.rebel459.music_and_melody.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.music_and_melody.platform.MaMPlatform;
import net.rebel459.music_and_melody.platform.event.ServerEvents;
import net.rebel459.music_and_melody.platform.util.EventType;

import java.util.*;

public class StructureMusicHandler {

    private static int serverTicks = 0;
    private static boolean shouldUpdateStructures = true;
    private static final Map<ResourceKey<Level>, List<Holder.Reference<Structure>>> STRUCTURES = new HashMap<>();

    public static void init() {
        if (!MaMServerConfig.get().sync_structures) return;
        MaMPlatform.NETWORKING.registerPlayToClient(StructureMusicPacket.TYPE, StructureMusicPacket.CODEC, (packet, player) -> {
            CURRENT_STRUCTURES = new Info(packet.structures(), packet.tags());
        });
        ServerEvents.onDatapackLoad(server -> {
            shouldUpdateStructures = true;
            STRUCTURES.clear();
            LAST_STRUCTURES.clear();
            LAST_POSITION.clear();
        });
        ServerEvents.onTick(EventType.PRE, server -> {
            if (++serverTicks < 20) return;
            serverTicks = 0;
            Map<ResourceKey<Level>, List<Holder.Reference<Structure>>> structures = getStructures(server);
            server.getAllLevels().forEach(level -> {
                StructureManager structureManager = level.structureManager();
                List<Holder.Reference<Structure>> levelStructures = structures.get(level.dimension());
                if (levelStructures == null) return;

                for (ServerPlayer player : level.players()) {
                    UUID playerId = player.getUUID();
                    BlockPos pos = BlockPos.containing(player.position());
                    if (LAST_POSITION.get(playerId) != null && LAST_POSITION.get(playerId).equals(player.blockPosition())) continue;
                    HashSet<ResourceLocation> structureIds = new HashSet<>();
                    HashSet<ResourceLocation> structureTags = new HashSet<>();
                    for (Holder<Structure> structure : levelStructures) {
                        if (structureManager.getStructureWithPieceAt(pos, structure.value()).isValid()) {
                            if (structure.unwrapKey().isEmpty()) continue;
                            ResourceLocation id = structure.unwrapKey().get().location();
                            structureIds.add(id);
                            structure.tags().toList().forEach(tag -> structureTags.add(tag.location()));
                        }
                    }
                    Info info = new Info(structureIds, structureTags);
                    LAST_POSITION.put(playerId, player.blockPosition());
                    if (LAST_STRUCTURES.get(playerId) == null || !LAST_STRUCTURES.get(playerId).equals(info)) {
                        MaMPlatform.NETWORKING.send(new StructureMusicPacket(info.structures, info.tags), player);
                        LAST_STRUCTURES.put(playerId, info);
                    }
                }
            });
        });
    }

    private static Map<ResourceKey<Level>, List<Holder.Reference<Structure>>> getStructures(MinecraftServer server) {
        if (shouldUpdateStructures) {
            server.getAllLevels().forEach(level -> STRUCTURES.put(level.dimension(), level.registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements().toList()));
            shouldUpdateStructures = false;
        }
        return STRUCTURES;
    }

    private static final Map<UUID, Info> LAST_STRUCTURES = new HashMap<>();
    private static final Map<UUID, BlockPos> LAST_POSITION = new HashMap<>();

    private static Info CURRENT_STRUCTURES = new Info(Set.of(), Set.of());

    public static Info getClientStructures() {
        return CURRENT_STRUCTURES;
    }

    public record Info(Set<ResourceLocation> structures, Set<ResourceLocation> tags) {}
}