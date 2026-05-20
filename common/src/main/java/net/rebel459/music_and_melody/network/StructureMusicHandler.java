package net.rebel459.music_and_melody.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.rebel459.unified.platform.UnifiedEvents;
import net.rebel459.unified.platform.UnifiedHelpers;
import net.rebel459.unified.util.EventType;

import java.util.*;

public class StructureMusicHandler {

    private static int serverTicks = 0;
    private static boolean shouldUpdateStructures = true;
    private static List<Holder.Reference<Structure>> structures = new ArrayList<>();

    public static void init() {
        UnifiedHelpers.NETWORKING.registerPlayToClient(StructureMusicPacket.TYPE, StructureMusicPacket.CODEC, (packet, player) -> {
            CURRENT_STRUCTURES = new Info(packet.structures(), packet.tags());
        });
        UnifiedEvents.Server.onDatapackLoad(server -> {
            shouldUpdateStructures = true;
        });
        UnifiedEvents.Server.onTick(EventType.PRE, server -> {
            if (++serverTicks <= 20) return;
            serverTicks = 0;
            server.getAllLevels().forEach(level -> {
                Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
                StructureManager structureManager = level.structureManager();
                List<Holder.Reference<Structure>> structures = getStructures(registry);

                for (ServerPlayer player : level.players()) {
                    BlockPos pos = BlockPos.containing(player.position());
                    HashSet<Identifier> structureIds = new HashSet<>();
                    HashSet<Identifier> structureTags = new HashSet<>();
                    for (Holder<Structure> structure : structures) {
                        if (structureManager.getStructureWithPieceAt(pos, structure.value()).isValid()) {
                            Identifier id = structure.unwrapKey().get().identifier();
                            structureIds.add(id);
                            structure.tags().toList().forEach(tag -> structureTags.add(tag.location()));
                        }
                    }
                    Info info = new Info(structureIds, structureTags);
                    if (SENT_STRUCTURES.get(player) == null || !SENT_STRUCTURES.get(player).equals(info)) {
                        UnifiedHelpers.NETWORKING.send(new StructureMusicPacket(info.structures, info.tags), player);
                        SENT_STRUCTURES.put(player, info);
                    }
                }
            });
        });
    }

    private static List<Holder.Reference<Structure>> getStructures(Registry<Structure> registry) {
        if (shouldUpdateStructures) {
            structures = registry.listElements()
                    .filter(Objects::nonNull)
                    .toList();
            shouldUpdateStructures = false;
        }
        return structures;
    }

    private static final Map<Player, Info> SENT_STRUCTURES = new HashMap<>();
    private static Info CURRENT_STRUCTURES = new Info(Set.of(), Set.of());

    public static Info getClientStructures() {
        return CURRENT_STRUCTURES;
    }

    public record Info(Set<Identifier> structures, Set<Identifier> tags) {}
}