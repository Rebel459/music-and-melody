package net.rebel459.music_and_melody.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.util.List;
import java.util.Optional;

public class Theme {

    public static final Identifier DEFAULT_ID = MusicAndMelody.id("default");
    public static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

    public final Identifier theme;
    public final Component name;
    public final Component description;
    public final Identifier icon;
    public final Identifier parent;
    public final Panels panels;
    public final Elements elements;
    public final Text text;
    public final Record record;
    public final boolean valid;
    public final List<String> errors;

    public Theme(Identifier theme, Component name, Component description, Identifier icon, Identifier parent, Panels panels, Elements elements, Text text, Record record, boolean valid, List<String> errors) {
        this.theme = theme;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.parent = parent;
        this.panels = panels;
        this.elements = elements;
        this.text = text;
        this.record = record;
        this.valid = valid;
        this.errors = List.copyOf(errors);
    }

    public boolean isCustom() {
        return this.theme != null && "config".equals(this.theme.getNamespace());
    }

    public record Panels(String background, String panelBackground, String panelOutline, String panelHighlighted, String popupPanelBackground, String popupOutline, String popupOverlay) {}

    public record Elements(String buttonBackground, String buttonHighlighted, String buttonDisabled, String outline, String barBackground, String barThumb, boolean buttonTextures) {}

    public record Text(String selected, String title, String primary, String primaryHighlighted, String description, String header, String headerSecondary, String favourite, String example, String disabled, String warning, boolean shadow) {}

    public record Record(Identifier id, Component name, Optional<Component> description, Optional<String> icon, Optional<String> parent, Optional<RawPanels> panels, Optional<RawElements> elements, Optional<RawText> text) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Record::name),
                ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(Record::description),
                Codec.STRING.optionalFieldOf("icon").forGetter(Record::icon),
                Codec.STRING.optionalFieldOf("parent").forGetter(Record::parent),
                RawPanels.CODEC.optionalFieldOf("panels").forGetter(Record::panels),
                RawElements.CODEC.optionalFieldOf("elements").forGetter(Record::elements),
                RawText.CODEC.optionalFieldOf("text").forGetter(Record::text)
        ).apply(instance, (name, description, icon, parent, panels, elements, text) ->
                new Record(null, name, description, icon, parent, panels, elements, text)));

        public Record withId(Identifier identifier) {
            return new Record(identifier, this.name, this.description, this.icon, this.parent,
                    this.panels, this.elements, this.text);
        }
    }

    public record RawPanels(Optional<String> background, Optional<String> panelBackground, Optional<String> panelOutline, Optional<String> panelHighlighted, Optional<String> popupPanelBackground, Optional<String> popupOutline, Optional<String> popupOverlay) {
        public static final Codec<RawPanels> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("background").forGetter(RawPanels::background),
                Codec.STRING.optionalFieldOf("panel_background").forGetter(RawPanels::panelBackground),
                Codec.STRING.optionalFieldOf("panel_outline").forGetter(RawPanels::panelOutline),
                Codec.STRING.optionalFieldOf("panel_highlighted").forGetter(RawPanels::panelHighlighted),
                Codec.STRING.optionalFieldOf("popup_panel_background").forGetter(RawPanels::popupPanelBackground),
                Codec.STRING.optionalFieldOf("popup_outline").forGetter(RawPanels::popupOutline),
                Codec.STRING.optionalFieldOf("popup_overlay").forGetter(RawPanels::popupOverlay)
        ).apply(instance, RawPanels::new));
    }

    public record RawElements(Optional<String> buttonBackground, Optional<String> buttonHighlighted, Optional<String> buttonDisabled, Optional<String> outline, Optional<String> barBackground, Optional<String> barThumb, Optional<Boolean> buttonTextures) {
        public static final Codec<RawElements> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("button_background").forGetter(RawElements::buttonBackground),
                Codec.STRING.optionalFieldOf("button_highlighted").forGetter(RawElements::buttonHighlighted),
                Codec.STRING.optionalFieldOf("button_disabled").forGetter(RawElements::buttonDisabled),
                Codec.STRING.optionalFieldOf("outline").forGetter(RawElements::outline),
                Codec.STRING.optionalFieldOf("bar_background").forGetter(RawElements::barBackground),
                Codec.STRING.optionalFieldOf("bar_thumb").forGetter(RawElements::barThumb),
                Codec.BOOL.optionalFieldOf("button_textures").forGetter(RawElements::buttonTextures)
        ).apply(instance, RawElements::new));
    }

    public record RawText(Optional<String> selected, Optional<String> title, Optional<String> primary, Optional<String> primaryHighlighted, Optional<String> description, Optional<String> header, Optional<String> headerSecondary, Optional<String> favourite, Optional<String> example, Optional<String> disabled, Optional<String> warning, Optional<Boolean> shadow) {
        public static final Codec<RawText> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("selected").forGetter(RawText::selected),
                Codec.STRING.optionalFieldOf("title").forGetter(RawText::title),
                Codec.STRING.optionalFieldOf("primary").forGetter(RawText::primary),
                Codec.STRING.optionalFieldOf("primary_highlighted").forGetter(RawText::primaryHighlighted),
                Codec.STRING.optionalFieldOf("description").forGetter(RawText::description),
                Codec.STRING.optionalFieldOf("header").forGetter(RawText::header),
                Codec.STRING.optionalFieldOf("header_secondary").forGetter(RawText::headerSecondary),
                Codec.STRING.optionalFieldOf("favourite").forGetter(RawText::favourite),
                Codec.STRING.optionalFieldOf("example").forGetter(RawText::example),
                Codec.STRING.optionalFieldOf("disabled").forGetter(RawText::disabled),
                Codec.STRING.optionalFieldOf("warning").forGetter(RawText::warning),
                Codec.BOOL.optionalFieldOf("shadow").forGetter(RawText::shadow)
        ).apply(instance, RawText::new));
    }
}
