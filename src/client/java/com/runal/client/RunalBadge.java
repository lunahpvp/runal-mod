package com.runal.client;

//? if 1.21.4 {
//?} else {
import net.minecraft.network.chat.FontDescription;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class RunalBadge {
    public static final String GLYPH = "\uE000";

    private static final Identifier ICON_FONT =
            Identifier.fromNamespaceAndPath("runal", "icons");

    //? if 1.21.4 {
    /*private static final Style ICON_STYLE = Style.EMPTY
            .withFont(ICON_FONT)
            .withColor(0xFFFFFF);
    *///?} else {
    private static final Style ICON_STYLE = Style.EMPTY
            .withFont(new FontDescription.Resource(ICON_FONT))
            .withColor(0xFFFFFF);
    //?}

    private RunalBadge() {
    }

    public static Component append(Component name) {
        if (name == null || name.getString().contains(GLYPH)) return name;

        MutableComponent result = Component.empty();
        result.append(Component.literal(GLYPH).withStyle(ICON_STYLE));
        result.append(Component.literal(" "));
        result.append(name.copy());
        return result;
    }
}
