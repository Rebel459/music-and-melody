package net.rebel459.music_and_melody.platform;

import com.google.common.base.Suppliers;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.platform.event.ServerEvents;
import net.rebel459.music_and_melody.platform.helper.NetworkingHelper;
import net.rebel459.music_and_melody.platform.helper.PackHelper;
import net.rebel459.music_and_melody.platform.helper.PlatformHelper;
import net.rebel459.music_and_melody.platform.registry.SoundEventRegistry;
import net.rebel459.music_and_melody.platform.util.EventType;
import net.rebel459.music_and_melody.platform.util.PackType;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MaMFabricPlatform {

    public static void init() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((minecraftServer, closeableResourceManager, b) -> ServerEvents.passOnDatapackLoad(minecraftServer));
        ServerTickEvents.START_SERVER_TICK.register((server) -> ServerEvents.passOnTick(EventType.PRE, server));
        ServerTickEvents.END_SERVER_TICK.register((server) -> ServerEvents.passOnTick(EventType.POST, server));
        MaMPlatform.SOUND_EVENTS = new FabricSoundEventRegistry();
        MaMPlatform.PLATFORM = new FabricPlatformHelper();
        MaMPlatform.PACKS = new FabricPackHelper();
        MaMPlatform.NETWORKING = new FabricNetworkingHelper();
    }

    public static class FabricSoundEventRegistry implements SoundEventRegistry {

        @Override
        public Supplier<SoundEvent> register(String path) {
            return register(path, -1F);
        }
        @Override
        public Supplier<SoundEvent> register(String path, float fixedRange) {
            ResourceKey<SoundEvent> key = ResourceKey.create(Registries.SOUND_EVENT, MusicAndMelody.id(path));
            SoundEvent rangeType = SoundEvent.createVariableRangeEvent(key.location());
            if (fixedRange >= 0F) rangeType = SoundEvent.createFixedRangeEvent(key.location(), fixedRange);
            SoundEvent finalRangeType = rangeType;
            SoundEvent sound = Registry.register(BuiltInRegistries.SOUND_EVENT, key, finalRangeType);
            Supplier<SoundEvent> supplied = Suppliers.memoize(() -> sound);
            supplied.get();
            return supplied;
        }

        @Override
        public Holder<SoundEvent> registerForHolder(String path) {
            return registerForHolder(path, -1F);
        }
        @Override
        public Holder<SoundEvent> registerForHolder(String path, float fixedRange) {
            ResourceLocation identifier = MusicAndMelody.id(path);
            SoundEvent rangeType = SoundEvent.createVariableRangeEvent(identifier);
            if (fixedRange >= 0F) rangeType = SoundEvent.createFixedRangeEvent(identifier, fixedRange);
            SoundEvent finalRangeType = rangeType;
            return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, identifier, finalRangeType);
        }

        @Override
        public Holder<SoundEvent> registerVanilla(String path) {
            if (FabricLoader.getInstance().isModLoaded("vanillabackport")) return null;
            ResourceLocation identifier = ResourceLocation.withDefaultNamespace(path);
            return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
        }
    }

    public static class FabricNetworkingHelper implements NetworkingHelper {

        @Override
        public <T extends CustomPacketPayload> void registerPlayToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, Player> handler) {
            PayloadTypeRegistry.playS2C().register(type, codec);

            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
                    handler.accept(payload, context.player());
                });
            }
        }

        @Override
        public void send(CustomPacketPayload payload, ServerPlayer player) {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        }
    }

    public static class FabricPlatformHelper implements PlatformHelper {

        @Override
        public boolean isModLoaded(String modId) {
            return FabricLoader.getInstance().isModLoaded(modId);
        }
    }

    public static class FabricPackHelper implements PackHelper {

        @Override
        public void add(ResourceLocation id, PackType info) {
            if (FabricLoader.getInstance().getModContainer(id.getNamespace()).isEmpty()) return;
            ResourceManagerHelper.registerBuiltinResourcePack(
                    id,
                    FabricLoader.getInstance().getModContainer(MusicAndMelody.MOD_ID).get(),
                    Component.translatable("pack." + id.getNamespace() + "." + id.getPath()),
                    getActivationType(info)
            );
        }

        public static ResourcePackActivationType getActivationType(PackType info) {
            return switch (info) {
                case REQUIRED_DATA, REQUIRED_RESOURCES -> ResourcePackActivationType.ALWAYS_ENABLED;
                case OPTIONAL_DATA, OPTIONAL_RESOURCES -> ResourcePackActivationType.DEFAULT_ENABLED;
            };
        }
    }
}
