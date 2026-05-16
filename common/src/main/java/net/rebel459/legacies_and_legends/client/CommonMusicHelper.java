package net.rebel459.legacies_and_legends.client;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.MultipliedFloats;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class CommonMusicHelper {
    public static final Identifier BASE_POOL = LaLSounds.COMMON_MUSIC.identifier();
    public static final Set<Identifier> FILTERED_POOLS = new HashSet<>();
    private static final Map<Identifier, List<Sound>> SOUND_POOLS = new HashMap<>();

    public static class Instance extends SimpleSoundInstance {
        public Instance(SoundEvent soundEvent) {
            super(soundEvent.location(), SoundSource.MUSIC, 1.0F, 1.0F, SoundInstance.createUnseededRandom(), false, 0, Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager soundManager) {
            WeighedSoundEvents event = soundManager.getSoundEvent(this.identifier);
            if (event == null) {
                this.sound = SoundManager.EMPTY_SOUND;
                return null;
            }

            WeighedSoundEvents common = soundManager.getSoundEvent(BASE_POOL);
            if (common == null || this.identifier.equals(BASE_POOL)) {
                this.sound = event.getSound(this.random);
                return event;
            }

            Set<SoundKey> commonSounds = getCommonSounds(BASE_POOL);

            this.sound = pickRawFilteredSound(this.identifier, commonSounds, soundManager, new HashSet<>());
            return event;
        }

        private Sound pickRawFilteredSound(Identifier event, Set<SoundKey> commonSounds, SoundManager soundManager, Set<Identifier> visitedEvents) {
            if (!visitedEvents.add(event)) return SoundManager.EMPTY_SOUND;

            List<Weighted<Sound>> candidates = new ArrayList<>();
            for (Sound sound : SOUND_POOLS.getOrDefault(event, Collections.emptyList())) {
                if (commonSounds.contains(SoundKey.of(sound))) continue;

                if (sound.getType() == Sound.Type.FILE) {
                    candidates.add(sound);
                } else if (sound.getType() == Sound.Type.SOUND_EVENT && containsNonCommonSound(sound.getLocation(), commonSounds, new HashSet<>(visitedEvents))) {
                    candidates.add(new FilteredEventSound(sound, commonSounds, soundManager));
                }
            }

            return pickWeighted(candidates);
        }

        private Sound pickWeighted(List<Weighted<Sound>> candidates) {
            int totalWeight = candidates.stream().mapToInt(Weighted::getWeight).sum();
            if (candidates.isEmpty() || totalWeight <= 0) return SoundManager.EMPTY_SOUND;

            int choice = this.random.nextInt(totalWeight);
            for (Weighted<Sound> candidate : candidates) {
                choice -= candidate.getWeight();
                if (choice < 0) return candidate.getSound(this.random);
            }

            return SoundManager.EMPTY_SOUND;
        }

        private class FilteredEventSound implements Weighted<Sound> {
            private final Sound sound;
            private final Set<SoundKey> commonSounds;
            private final SoundManager soundManager;

            private FilteredEventSound(Sound sound, Set<SoundKey> commonSounds, SoundManager soundManager) {
                this.sound = sound;
                this.commonSounds = commonSounds;
                this.soundManager = soundManager;
            }

            @Override
            public int getWeight() {
                return this.sound.getWeight();
            }

            @Override
            public Sound getSound(RandomSource random) {
                Sound wrappedSound = pickRawFilteredSound(this.sound.getLocation(), this.commonSounds, this.soundManager, new HashSet<>());
                if (wrappedSound == SoundManager.EMPTY_SOUND) return SoundManager.EMPTY_SOUND;

                return new Sound(
                        wrappedSound.getLocation(),
                        new MultipliedFloats(wrappedSound.getVolume(), this.sound.getVolume()),
                        new MultipliedFloats(wrappedSound.getPitch(), this.sound.getPitch()),
                        this.sound.getWeight(),
                        Sound.Type.FILE,
                        wrappedSound.shouldStream() || this.sound.shouldStream(),
                        wrappedSound.shouldPreload(),
                        wrappedSound.getAttenuationDistance()
                );
            }

            @Override
            public void preloadIfRequired(@NonNull SoundEngine soundEngine) {
                WeighedSoundEvents event = this.soundManager.getSoundEvent(this.sound.getLocation());
                if (event != null) event.preloadIfRequired(soundEngine);
            }
        }
    }

    public static boolean filterWeighedSounds(WeighedSoundEvents weighed, Set<SoundKey> commonSounds) {
        int original = weighed.list.size();
        boolean filtered = false;
        for (Weighted<Sound> weighted : weighed.list) {
            if (weighted instanceof WeighedSoundEvents event) {
                if (filterWeighedSounds(event, commonSounds)) filtered = true;
            }
        }
        weighed.list.removeIf(weightedSound -> {
            if (weightedSound instanceof WeighedSoundEvents event) {
                return event.list.isEmpty();
            }
            return weightedSound instanceof Sound sound && commonSounds.contains(SoundKey.of(sound));
        });
        return original != weighed.list.size() || filtered;
    }

    public static void clearSoundPools() {
        SOUND_POOLS.clear();
    }

    public static void addSoundPool(Identifier event, List<Sound> sounds, boolean replace) {
        if (replace || !SOUND_POOLS.containsKey(event)) {
            SOUND_POOLS.put(event, new ArrayList<>(sounds));
        } else {
            SOUND_POOLS.get(event).addAll(sounds);
        }
    }

    public static Set<SoundKey> getCommonSounds(Identifier event) {
        Set<SoundKey> common = new HashSet<>();
        collectSoundKeys(event, common, new HashSet<>());
        return common;
    }

    public static boolean containsCommonSound(Identifier event, Set<SoundKey> commonSounds) {
        return containsCommonSound(event, commonSounds, new HashSet<>());
    }

    private static boolean containsNonCommonSound(Identifier event, Set<SoundKey> commonSounds, Set<Identifier> visitedEvents) {
        if (!visitedEvents.add(event)) return false;

        for (Sound sound : SOUND_POOLS.getOrDefault(event, Collections.emptyList())) {
            if (commonSounds.contains(SoundKey.of(sound))) continue;
            if (sound.getType() == Sound.Type.FILE) return true;
            if (sound.getType() == Sound.Type.SOUND_EVENT && containsNonCommonSound(sound.getLocation(), commonSounds, visitedEvents)) return true;
        }

        return false;
    }

    private static void collectSoundKeys(Identifier event, Set<SoundKey> common, Set<Identifier> visitedEvents) {
        if (!visitedEvents.add(event)) return;

        for (Sound sound : SOUND_POOLS.getOrDefault(event, Collections.emptyList())) {
            SoundKey key = SoundKey.of(sound);
            common.add(key);
            if (sound.getType() == Sound.Type.SOUND_EVENT) {
                collectSoundKeys(sound.getLocation(), common, visitedEvents);
            }
        }
    }

    private static boolean containsCommonSound(Identifier event, Set<SoundKey> commonSounds, Set<Identifier> visitedEvents) {
        if (!visitedEvents.add(event)) return false;

        for (Sound sound : SOUND_POOLS.getOrDefault(event, Collections.emptyList())) {
            if (commonSounds.contains(SoundKey.of(sound))) return true;
            if (sound.getType() == Sound.Type.SOUND_EVENT && containsCommonSound(sound.getLocation(), commonSounds, visitedEvents)) return true;
        }

        return false;
    }

    public record SoundKey(Identifier location, Sound.Type type) {
        public static SoundKey of(Sound sound) {
            return new SoundKey(sound.getLocation(), sound.getType());
        }
    }
}
