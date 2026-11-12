package yagen.waitmydawn.dont_hurt_them.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
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
        FriendshipNetwork.sendToServer(new FriendshipNetwork.Rename(uuid, input.getValue())); onClose();
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        renderBackground(graphics);
        graphics.fill(width / 2 - 120, height / 2 - 42, width / 2 + 120, height / 2 + 42, 0xFF1B2936);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 32, 0xFFE1EDF4);
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, tick);
    }
    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { save(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
