package net.rebel459.music_and_melody.platform;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.platform.event.ServerEvents;
import net.rebel459.music_and_melody.platform.helper.NetworkingHelper;
import net.rebel459.music_and_melody.platform.helper.PackHelper;
import net.rebel459.music_and_melody.platform.helper.PlatformHelper;
import net.rebel459.music_and_melody.platform.registry.SoundEventRegistry;
import net.rebel459.music_and_melody.platform.util.EventType;
import net.rebel459.music_and_melody.platform.util.PackType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MaMNeoForgePlatform {

    public static void init(IEventBus bus) {
        NeoForgeSoundEventRegistry.SOUNDS.register(bus);
        NeoForgeSoundEventRegistry.VANILLA_SOUNDS.register(bus);
        bus.addListener(NeoForgePackHelper::addFeaturePacks);
        bus.addListener(NeoForgeNetworkingHelper::registerWithHandler);

        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD && server != null) {
                ServerEvents.passOnDatapackLoad(server);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> {
            ServerEvents.passOnTick(EventType.PRE, event.getServer());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            ServerEvents.passOnTick(EventType.POST, event.getServer());
        });

        MaMPlatform.SOUND_EVENTS = new NeoForgeSoundEventRegistry();
        MaMPlatform.PLATFORM = new NeoForgePlatformHelper();
        MaMPlatform.PACKS = new NeoForgePackHelper();
        MaMPlatform.NETWORKING = new NeoForgeNetworkingHelper();
    }

    public static class NeoForgeSoundEventRegistry implements SoundEventRegistry {

        public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MusicAndMelody.MOD_ID);
        public static final DeferredRegister<SoundEvent> VANILLA_SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "minecraft");

        @Override
        public Supplier<SoundEvent> register(String path) {
            return SOUNDS.register(path, SoundEvent::createVariableRangeEvent);
        }
        @Override
        public Supplier<SoundEvent> register(String path, float fixedRange) {
            return SOUNDS.register(path, () -> SoundEvent.createFixedRangeEvent(MusicAndMelody.id(path), fixedRange));
        }

        @Override
        public Holder<SoundEvent> registerForHolder(String path) {
            return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(MusicAndMelody.id(path)));
        }
        @Override
        public Holder<SoundEvent> registerForHolder(String path, float fixedRange) {
            return SOUNDS.register(path, () -> SoundEvent.createFixedRangeEvent(MusicAndMelody.id(path), fixedRange));
        }

        @Override
        public Holder<SoundEvent> registerVanilla(String path) {
            if (ModList.get().isLoaded("vanillabackport")) return null;
            return VANILLA_SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(MusicAndMelody.id(path)));
        }
    }

    public static class NeoForgeNetworkingHelper implements NetworkingHelper {

        @Override
        public void send(CustomPacketPayload payload, ServerPlayer player) {
            player.connection.send(new ClientboundCustomPayloadPacket(payload));
        }

        private static final List<HandledToClient<?>> HANDLED_TO_CLIENT_LIST = new ArrayList<>();
        private record HandledToClient<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec, BiConsumer<T, Player> handler, boolean play) {}

        @Override
        public void registerPlayToClient(CustomPacketPayload.Type type, StreamCodec codec, BiConsumer handler) {
            HANDLED_TO_CLIENT_LIST.add(new HandledToClient<>(type, codec, handler, true));
        }

        @SubscribeEvent
        public static void registerWithHandler(RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");

            for (HandledToClient handled : HANDLED_TO_CLIENT_LIST) {
                if (handled.play) {
                    registrar.playToClient(
                            handled.type,
                            handled.codec,
                            (payload, context) -> handled.handler.accept(payload, context.player())
                    );
                } else {
                    registrar.configurationToClient(
                            handled.type,
                            handled.codec,
                            (payload, context) -> handled.handler.accept(payload, context.player())
                    );
                }
            }
        }
    }

    public static class NeoForgePlatformHelper implements PlatformHelper {

        @Override
        public boolean isModLoaded(String modId) {
            var modList = ModList.get();
            boolean loadingModCheck = FMLLoader.getCurrent().getLoadingModList().getModFileById(modId) != null;
            if (modList == null) return loadingModCheck;
            else return ModList.get().isLoaded(modId) || loadingModCheck;
        }
    }

    public static class NeoForgePackHelper implements PackHelper {

        public static List<Pair<Identifier, PackType>> PACK_LIST = new ArrayList<>();

        @Override
        public void add(Identifier id, PackType info) {
            PACK_LIST.add(Pair.of(id, info));
        }

        public static boolean getBoolean(PackType info) {
            return switch (info) {
                case REQUIRED_DATA, REQUIRED_RESOURCES -> true;
                case OPTIONAL_DATA, OPTIONAL_RESOURCES -> false;
            };
        }

        public static net.minecraft.server.packs.PackType getType(PackType info) {
            return switch (info) {
                case REQUIRED_DATA, OPTIONAL_DATA -> net.minecraft.server.packs.PackType.SERVER_DATA;
                case REQUIRED_RESOURCES, OPTIONAL_RESOURCES -> net.minecraft.server.packs.PackType.CLIENT_RESOURCES;
            };
        }

        @SubscribeEvent
        public static void addFeaturePacks(AddPackFindersEvent event) {
            for (Pair<Identifier, PackType> pair : PACK_LIST) {
                Identifier id = pair.getFirst();
                PackType info = pair.getSecond();

                event.addPackFinders(
                        Identifier.fromNamespaceAndPath(id.getNamespace(), "resourcepacks/" + id.getPath()),
                        getType(info),
                        Component.translatable("pack." + id.getNamespace() + "." + id.getPath()),
                        PackSource.BUILT_IN,
                        getBoolean(info),
                        Pack.Position.TOP
                );
            }
        }
    }
}
