package yagen.waitmydawn.dont_hurt_them.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.UUID;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;

public final class RenameFriendScreen extends Screen {
    private final Screen parent;
    private final UUID uuid;
    private final String currentName;
    private EditBox input;
    public RenameFriendScreen(Screen parent, UUID uuid, String currentName) {
        super(Component.translatable("screen.dont_hurt_them.rename"));
        this.parent = parent; this.uuid = uuid; this.currentName = currentName;
    }
    @Override
    protected void init() {
        int x = width / 2 - 120, y = height / 2 - 42;
        input = new EditBox(font, x + 12, y + 28, 216, 16, title);
        input.setMaxLength(64); input.setValue(currentName); addRenderableWidget(input); setInitialFocus(input);
        addRenderableWidget(new CompactButton(x + 12, y + 55, 104, 16, Component.translatable("gui.done"), b -> save(), () -> false));
        addRenderableWidget(new CompactButton(x + 124, y + 55, 104, 16, Component.translatable("gui.cancel"), b -> onClose(), () -> false));
    }
    private void save() {
        if (input.getValue().isBlank()) return;
        ClientPlayNetworking.send(new FriendshipNetwork.Rename(uuid, input.getValue())); onClose();
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        graphics.fill(width / 2 - 120, height / 2 - 42, width / 2 + 120, height / 2 + 42, 0xFF1B2936);
        graphics.centeredText(font, title, width / 2, height / 2 - 32, 0xFFE1EDF4);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget) widget.extractRenderState(graphics, mouseX, mouseY, tick);
    }
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { save(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
