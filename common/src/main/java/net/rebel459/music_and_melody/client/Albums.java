package net.rebel459.music_and_melody.client;

import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.MaMConfig;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

public class Albums {

    public static Album MUSIC_AND_MELODY = new Album(MusicAndMelody.id("music_and_melody"), MaMConfig.get().client.albums.music_and_melody);

    public static Album SPINOFF_MUSIC = new Album(MusicAndMelody.id("spinoff_music"), MaMConfig.get().client.albums.spinoff_music, List.of(
            melody("creative/earth"),
            melody("menu/halland")
    ));

    public static Album VOLUME_ALPHA = new Album(Identifier.withDefaultNamespace("volume_alpha"), MaMConfig.get().client.albums.volume_alpha, List.of(
            vanilla("clark"),
            vanilla("danny"),
            vanilla("dry_hands"),
            vanilla("haggstrom"),
            vanilla("key"),
            vanilla("living_mice"),
            vanilla("mice_on_venus"),
            vanilla("minecraft"),
            vanilla("oxygene"),
            vanilla("subwoofer_lullaby"),
            vanilla("sweden"),
            vanilla("wet_hands")
    ));

    public static Album VOLUME_BETA = new Album(Identifier.withDefaultNamespace("volume_beta"), MaMConfig.get().client.albums.volume_beta, List.of(
            vanilla("creative/aria_math"),
            vanilla("creative/biome_fest"),
            vanilla("creative/blind_spots"),
            vanilla("creative/dreiton"),
            vanilla("creative/haunt_muskie"),
            vanilla("creative/taswell"),
            vanilla("nether/ballad_of_the_cats"),
            vanilla("nether/concrete_halls"),
            vanilla("nether/dead_voxel"),
            vanilla("nether/warmth"),
            vanilla("end/the_end"),
            vanilla("end/boss"),
            vanilla("end/alpha"),
            Identifier.withDefaultNamespace("music/menu/beginning_2"),
            Identifier.withDefaultNamespace("music/menu/floating_trees"),
            Identifier.withDefaultNamespace("music/menu/moog_city_2"),
            Identifier.withDefaultNamespace("music/menu/mutation")
    ));

    public static Album UPDATE_AQUATIC = new Album(Identifier.withDefaultNamespace("update_aquatic"), MaMConfig.get().client.albums.update_aquatic, List.of(
            vanilla("water/axolotl"),
            vanilla("water/dragon_fish"),
            vanilla("water/shunji")
    ));

    public static Album NETHER_UPDATE = new Album(Identifier.withDefaultNamespace("nether_update"), MaMConfig.get().client.albums.nether_update, List.of(
            vanilla("nether/crimson_forest/chrysopoeia"),
            vanilla("nether/nether_wastes/rubedo"),
            vanilla("nether/soulsand_valley/so_below")
    ));

    public static Album CAVES_AND_CLIFFS = new Album(Identifier.withDefaultNamespace("caves_and_cliffs"), MaMConfig.get().client.albums.caves_and_cliffs, List.of(
            vanilla("ancestry"),
            vanilla("an_ordinary_day"),
            vanilla("comforting_memories"),
            vanilla("floating_dream"),
            vanilla("infinite_amethyst"),
            vanilla("left_to_bloom"),
            vanilla("one_more_day"),
            vanilla("stand_tall"),
            vanilla("wending")
    ));

    public static Album WILD_UPDATE = new Album(Identifier.withDefaultNamespace("wild_update"), MaMConfig.get().client.albums.wild_update, List.of(
            vanilla("swamp/aerie"),
            vanilla("swamp/firebugs"),
            vanilla("swamp/labyrinthine")
    ));

    public static Album TRAILS_AND_TALES = new Album(Identifier.withDefaultNamespace("trails_and_tales"), MaMConfig.get().client.albums.trails_and_tales, List.of(
            vanilla("a_familiar_room"),
            vanilla("bromeliad"),
            vanilla("crescent_dunes"),
            vanilla("echo_in_the_wind")
    ));

    public static Album TRICKY_TRIALS = new Album(Identifier.withDefaultNamespace("tricky_trials"), MaMConfig.get().client.albums.tricky_trials, List.of(
            vanilla("deeper"),
            vanilla("eld_unknown"),
            vanilla("endless"),
            vanilla("featherfall"),
            vanilla("komorebi"),
            vanilla("pokopoko"),
            vanilla("puzzlebox"),
            vanilla("watcher"),
            vanilla("yakusoku")
    ));

    public static Album CHASE_THE_SKIES = new Album(Identifier.withDefaultNamespace("chase_the_skies"), MaMConfig.get().client.albums.chase_the_skies, List.of(
            vanilla("below_and_above"),
            vanilla("broken_clocks"),
            vanilla("fireflies"),
            vanilla("lilypad"),
            vanilla("os_piano")
    ));

    public static Album CHAOS_CUBED = new Album(Identifier.withDefaultNamespace("chaos_cubed"), MaMConfig.get().client.albums.chaos_cubed, List.of(
            vanilla("ebb"),
            vanilla("home"),
            vanilla("memories"),
            vanilla("nightly"),
            vanilla("shores")
    ));

    public static Identifier vanilla(String path) {
        return Identifier.withDefaultNamespace("music/game/" + path);
    }

    public static Identifier melody(String path) {
        return MusicAndMelody.id("music/" + path);
    }

    public static void init() {}
}
