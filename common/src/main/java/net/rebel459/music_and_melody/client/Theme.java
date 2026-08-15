package net.rebel459.music_and_melody.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class Theme {

    public final Identifier theme;
    public final Component name;
    public final Identifier icon;
    public final Identifier parent;
    public final Panels panels;
    public final Elements elements;
    public final Text text;

    public Theme(Identifier theme, Component name, Identifier icon, Identifier parent, Panels panels, Elements elements, Text text) {
        this.theme = theme;
        this.name = name;
        this.icon = icon;
        this.parent = parent;
        this.panels = panels;
        this.elements = elements;
        this.text = text;
    }

    public record Record(Identifier id, Component name, Identifier icon, Identifier parent, Panels panels, Elements elements, Text text) {
/*        public static final Codec<Playlist.Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Theme.Record::name),
                Identifier.CODEC.optionalFieldOf("icon", Identifier.withDefaultNamespace("textures/misc/unknown_pack.png")).forGetter(Theme.Record::icon),
                Identifier.CODEC.optionalFieldOf("parent", MusicAndMelody.id("themes/default.json")).forGetter(Theme.Record::parent),
                Panels.CODEC.optionalFieldOf("panels").forGetter(Record::panels),
                Elements.CODEC.optionalFieldOf("elements").forGetter(Record::elements),
                Text.CODEC.optionalFieldOf("text").forGetter(Record::text)
        ).apply(instance, Playlist.Record::new));*/
    }

    public record Panels(String background, float backgroundOpacity, String panelBackground, String panelOutline, float panelOpacity, String popupPanelBackground, float popupPanelOpacity, String popupOverlay, float popupOverlayOpacity) { }

    public record Elements(String buttonBackground, String buttonHighlight, String outline, String barBackground, String barThumb, float elementOpacity, Optional<Identifier> buttonTexture, Optional<Identifier> buttonHighlightTexture, Optional<Identifier> buttonInactiveTexture) { }

    public record Text(String selected, String title, String primary, String description, String header, String favourite, String example, String disabled, String warning) { }
}
