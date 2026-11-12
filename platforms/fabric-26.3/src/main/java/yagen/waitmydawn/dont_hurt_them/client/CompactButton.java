package yagen.waitmydawn.dont_hurt_them.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;

public final class CompactButton extends Button {
    private final BooleanSupplier selected;
    public CompactButton(int x, int y, int width, int height, Component label, OnPress action, BooleanSupplier selected) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        this.selected = selected;
    }
    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        int color = selected.getAsBoolean() ? 0xFF365B69 : isHoveredOrFocused() ? 0xFF36495C : 0xFF263747;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
        if (selected.getAsBoolean()) graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), 0xFF91D8BE);
        float scale = 0.8f;
        var font = Minecraft.getInstance().font;
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX() + getWidth() / 2f, getY() + (getHeight() - font.lineHeight * scale) / 2f);
        graphics.pose().scale(scale, scale);
        graphics.text(font, getMessage(), -font.width(getMessage()) / 2, 0, active ? 0xFFE1EDF4 : 0xFF777777, false);
        graphics.pose().popMatrix();
    }
}
