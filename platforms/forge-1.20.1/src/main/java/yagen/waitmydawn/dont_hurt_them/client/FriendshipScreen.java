package yagen.waitmydawn.dont_hurt_them.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.config.ClientConfigs;
import yagen.waitmydawn.dont_hurt_them.core.AlbumFilter;
import yagen.waitmydawn.dont_hurt_them.core.AlbumLayout;
import yagen.waitmydawn.dont_hurt_them.core.AlbumQuery;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipEntry;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.core.RegistryCompletion;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;

public final class FriendshipScreen extends Screen {
    private final Map<String, Entity> previews = new HashMap<>();
    private final Set<String> failedPreviews = new HashSet<>();
    private final Map<UUID, Entity> tracked = new HashMap<>();
    private final Map<String, AlbumQuery.EntityKind> entityKinds = new HashMap<>();
    private net.minecraft.client.multiplayer.ClientLevel previewLevel;
    private EditBox registry, search;
    private String searchText = "";
    private AlbumQuery.NonLivingMode nonLivingMode = AlbumQuery.NonLivingMode.INCLUDE;
    private AlbumQuery.SearchField searchField = AlbumQuery.SearchField.NAME;
    private RegistryCompletion completion;
    private CompactButton deadButton, clearDeadButton, nonLivingButton;
    private boolean individuals = true, types = true, favoritesOnly, deadOnly;
    private boolean applyingCompletion, tabCompleted, layoutDirty;
    private int suggestionOffset;
    private List<Component> hoveredTooltip = List.of();
    private Component feedback = Component.empty();
    private int left, top, panelWidth, panelHeight, columns, rows, cardWidth, cardHeight, cardLeft, page, capacity;
    private float uiScale, canvasWidth, canvasHeight;

    public FriendshipScreen() { super(text("title")); }

    @Override
    protected void init() {
        String previousInput = registry == null ? "" : registry.getValue();
        capacity = ClientConfigs.CARDS_PER_PAGE.get();
        AlbumLayout layout = AlbumLayout.create(width, height, capacity);
        columns = layout.columns(); rows = layout.rows(); uiScale = layout.scale();
        canvasWidth = layout.canvasWidth(); canvasHeight = layout.canvasHeight();
        panelWidth = layout.panelWidth(); panelHeight = layout.panelHeight(); left = layout.left(); top = layout.top();
        cardWidth = layout.cardWidth(); cardHeight = layout.cardHeight(); cardLeft = layout.cardLeft();
        addRenderableWidget(new CompactButton(left + 12, top + 23, 78, 16, text("individuals"), b -> { individuals = !individuals; page = 0; }, () -> individuals));
        addRenderableWidget(new CompactButton(left + 95, top + 23, 78, 16, text("types"), b -> { types = !types; page = 0; }, () -> types));
        addRenderableWidget(new CompactButton(left + 178, top + 23, 78, 16, text("favorites"), b -> { favoritesOnly = !favoritesOnly; page = 0; }, () -> favoritesOnly));
        deadButton = addRenderableWidget(new CompactButton(left + 261, top + 23, 68, 16, text("dead"), b -> { deadOnly = !deadOnly; page = 0; }, () -> deadOnly));
        clearDeadButton = addRenderableWidget(new CompactButton(left + 334, top + 23, 82, 16, text("clear_deceased"), b -> send("clear_deceased", ""), () -> false));
        nonLivingButton = addRenderableWidget(new CompactButton(left + 421, top + 23, 174, 16, text(nonLivingMode.labelKey()), b -> {
            nonLivingMode = nonLivingMode.next(); b.setMessage(text(nonLivingMode.labelKey())); page = 0;
        }, () -> nonLivingMode != AlbumQuery.NonLivingMode.INCLUDE));
        int bottom = top + panelHeight;
        registry = new EditBox(font, left + 12, bottom - 42, 180, 16, text("registry"));
        registry.setMaxLength(256); registry.setHint(Component.literal("namespace:entity"));
        completion = new RegistryCompletion(BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .filter(id -> BuiltInRegistries.ENTITY_TYPE.get(id) != net.minecraft.world.entity.EntityType.PLAYER)
                .map(ResourceLocation::toString).toList());
        registry.setResponder(this::updateCompletion); registry.setValue(previousInput); addRenderableWidget(registry);
        search = new EditBox(font, left + 148, top + 5, 196, 16, text("search"));
        search.setMaxLength(256); search.setHint(text("search")); search.setValue(searchText);
        search.setResponder(value -> { searchText = value; page = 0; }); addRenderableWidget(search);
        addRenderableWidget(new CompactButton(left + 350, top + 5, 86, 16, text(searchField.labelKey()), b -> {
            searchField = searchField.next(); b.setMessage(text(searchField.labelKey())); page = 0;
        }, () -> false));
        addRenderableWidget(new CompactButton(left + 441, top + 5, 22, 16, Component.literal("×"), b -> search.setValue(""), () -> false));
        addRenderableWidget(new CompactButton(left + 198, bottom - 42, 64, 16, text("add"), b -> addType(), () -> false));
        addRenderableWidget(new CompactButton(left + panelWidth - 106, bottom - 42, 94, 16, text("page_size").append(" " + capacity), b -> {
            ClientConfigs.setCardsPerPage(capacity == 2 ? 4 : capacity == 4 ? 8 : 2); page = 0; layoutDirty = true;
        }, () -> false));
        addRenderableWidget(new CompactButton(left + 12, bottom - 21, 26, 14, Component.literal("<"), b -> page = Math.max(0, page - 1), () -> false));
        addRenderableWidget(new CompactButton(left + 43, bottom - 21, 26, 14, Component.literal(">"), b -> page = Math.min(pageCount() - 1, page + 1), () -> false));
        addRenderableWidget(new CompactButton(left + panelWidth - 72, bottom - 21, 60, 14, text("close"), b -> onClose(), () -> false));
    }

    private List<FriendshipEntry> entries() {
        AlbumQuery query = new AlbumQuery(new AlbumFilter(individuals, types, favoritesOnly, deadOnly), nonLivingMode, searchField, searchText);
        return ClientFriendships.list().sorted().stream().filter(entry -> query.matches(entry, ClientFriendships.dead(entry.key()), entityKind(entry),
                query.needsName() ? displayName(entry, ClientFriendships.dead(entry.key()) ? null : findLive(entry), ClientFriendships.live(entry.key())) : "")).toList();
    }
    private int pageCount() { return Math.max(1, (entries().size() + capacity - 1) / capacity); }
    private void drawText(GuiGraphics g, Component value, float x, float y, int color, float scale, int maxWidth) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        g.drawString(font, font.plainSubstrByWidth(value.getString(), (int) (maxWidth / scale)), 0, 0, color, false); g.pose().popPose();
    }
    private void updateCompletion(String value) {
        if (applyingCompletion) return;
        completion.update(value); suggestionOffset = 0; tabCompleted = false;
    }
    private void applyCompletion() {
        String selected = completion.selected(); if (selected == null) return;
        applyingCompletion = true;
        try { registry.setValue(selected); } finally { applyingCompletion = false; }
        tabCompleted = true;
    }
    private void addType() {
        String value = registry.getValue().trim(); ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !value.contains(":") || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) { feedback = text("invalid"); return; }
        if (BuiltInRegistries.ENTITY_TYPE.get(id) == net.minecraft.world.entity.EntityType.PLAYER) { feedback = text("player_type_forbidden"); return; }
        if (ClientFriendships.list().contains("type/" + id)) { feedback = text("exists"); return; }
        if (ClientFriendships.list().entries().size() >= FriendshipList.MAX_ENTRIES) { feedback = text("full"); return; }
        send("add_type", id.toString()); registry.setValue(""); feedback = text("sent");
    }
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float tick) {
        if (layoutDirty) { layoutDirty = false; rebuildWidgets(); }
        renderBackground(g);
        int mx = (int) (mouseX / uiScale), my = (int) (mouseY / uiScale);
        refreshTracked();
        boolean hasDead = false, canClearDead = false, hasNonLiving = false;
        for (FriendshipEntry entry : ClientFriendships.list().entries()) {
            if (entityKind(entry) == AlbumQuery.EntityKind.NON_LIVING) hasNonLiving = true;
            if (!entry.individual() || !ClientFriendships.dead(entry.key())) continue;
            hasDead = true;
            if (!entry.favorite()) canClearDead = true;
        }
        deadButton.visible = hasDead; if (!hasDead) deadOnly = false;
        clearDeadButton.visible = hasDead;
        clearDeadButton.active = canClearDead;
        nonLivingButton.visible = hasNonLiving; nonLivingButton.setX(left + (hasDead ? 421 : 261));
        if (!hasNonLiving && nonLivingMode != AlbumQuery.NonLivingMode.INCLUDE) {
            nonLivingMode = AlbumQuery.NonLivingMode.INCLUDE; nonLivingButton.setMessage(text(nonLivingMode.labelKey())); page = 0;
        }
        hoveredTooltip = List.of(); page = Math.min(page, pageCount() - 1);
        g.pose().pushPose(); g.pose().scale(uiScale, uiScale, 1);
        try {
            g.fill(left, top, left + panelWidth, top + panelHeight, 0xF21B2633);
            g.fill(left, top, left + panelWidth, top + 2, 0xFF7BDCB5);
            drawText(g, title, left + 12, top + 8, 0xFFE5F4EC, 0.9f, 128);
            List<FriendshipEntry> shown = entries();
            if (shown.isEmpty()) drawText(g, text(ClientFriendships.list().entries().isEmpty() ? "empty" : "no_matches"), left + 18, top + 64, 0xFFABC2D2, 0.8f, panelWidth - 36);
            for (int i = 0; i < capacity && page * capacity + i < shown.size(); i++) {
                drawCard(g, shown.get(page * capacity + i), cardLeft + i % columns * (cardWidth + 8), top + 46 + i / columns * (cardHeight + 8), mx, my);
            }
            drawText(g, feedback, left + 12, top + panelHeight - 55, 0xFFE5D7A2, 0.7f, panelWidth - 24);
            drawText(g, Component.literal((page + 1) + " / " + pageCount()), left + panelWidth / 2f - 12, top + panelHeight - 18, 0xFFABC2D2, 0.75f, 60);
            String selected = completion.selected(), input = registry.getValue();
            int available = Math.max(0, registry.getInnerWidth() - font.width(input));
            registry.setSuggestion(registry.isFocused() && completion.visible() && selected != null && selected.startsWith(input)
                    && registry.getCursorPosition() == input.length() && registry.getHighlighted().isEmpty()
                    ? font.plainSubstrByWidth(selected.substring(input.length()), available) : null);
            // Screen.render would blur the already drawn foreground in this API generation.
            for (var widget : renderables) widget.render(g, mx, my, tick);
            renderSuggestions(g, mx, my);
            if (!hoveredTooltip.isEmpty() && !suggestionsVisible()) renderCardTooltip(g, mx, my);
        } finally { g.pose().popPose(); }
    }
    private String displayName(FriendshipEntry entry, Entity live, CompoundTag info) {
        if (live != null) return live.getName().getString();
        if (info.contains("name")) return info.getString("name");
        if (!entry.individual()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.typeId());
            if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return BuiltInRegistries.ENTITY_TYPE.get(id).getDescription().getString();
        }
        return entry.name();
    }
    private void drawCard(GuiGraphics g, FriendshipEntry entry, int x, int y, int mouseX, int mouseY) {
        boolean dead = ClientFriendships.dead(entry.key());
        int color = dead ? 0x92979D : entry.individual() ? FriendshipList.INDIVIDUAL_COLOR : FriendshipList.TYPE_COLOR;
        float textScale = capacity == 2 ? 0.85f : capacity == 4 ? 0.75f : 0.65f;
        g.fill(x, y, x + cardWidth, y + cardHeight, dead ? 0xFF293039 : 0xFF263545);
        g.fill(x, y, x + 2, y + cardHeight, 0xFF000000 | color);
        Entity live = dead ? null : findLive(entry);
        Entity portrait = live == null ? preview(entry) : live;
        CompoundTag info = ClientFriendships.live(entry.key());
        String name = displayName(entry, live, info);
        boolean renameable = entry.individual() && !dead && info.getBoolean("renameable");
        drawText(g, Component.literal(name), x + 7, y + 7, color, textScale, cardWidth - 48);
        drawText(g, Component.literal(entry.favorite() ? "★" : "☆"), x + cardWidth - 30, y + 6, 0xFFFFD77A, 0.8f, 12);
        drawText(g, Component.literal("×"), x + cardWidth - 14, y + 6, 0xFFE9A7A7, 0.8f, 10);
        drawText(g, Component.literal(entry.typeId()), x + 7, y + 20, 0xFF96A9B9, textScale * 0.8f, cardWidth - 14);
        if (renameable && mouseX >= x + 6 && mouseX < x + cardWidth - 40 && mouseY >= y + 4 && mouseY < y + 16) {
            g.fill(x + 7, y + 15, x + Math.min(cardWidth - 40, 7 + (int) (font.width(name) * textScale)), y + 16, 0xFF000000 | color);
        }
        int portraitHeight = cardHeight - (entry.individual() ? 64 : 39);
        if (portrait != null && !failedPreviews.contains(previewKey(entry))) {
            try { EntityPortrait.render(g, portrait, x + 5, y + 32, cardWidth - 10, portraitHeight, mouseX, mouseY); }
            catch (RuntimeException error) { failedPreviews.add(previewKey(entry)); DontHurtThem.LOGGER.warn("Cannot render friendship portrait for {}", entry.key(), error); }
        } else drawText(g, text("no_preview"), x + 7, y + 44, 0xFF96A9B9, textScale, cardWidth - 14);
        if (entry.individual()) {
            if (dead) drawText(g, text("dead"), x + 7, y + cardHeight - 20, color, textScale, cardWidth - 14);
            else {
                String health = ClientFriendships.health(entry.key());
                drawText(g, text("health").append(health), x + 7, y + cardHeight - 24, 0xFFE2D0CD, textScale, cardWidth - 14);
                if (live != null || info.getBoolean("loaded")) {
                    double px = live == null ? info.getDouble("x") : live.getX(), py = live == null ? info.getDouble("y") : live.getY(), pz = live == null ? info.getDouble("z") : live.getZ();
                    boolean same = live != null || minecraft.level.dimension().location().toString().equals(info.getString("dimension"));
                    String distance = same ? format(minecraft.player.position().distanceTo(new Vec3(px, py, pz))) + "m" : text("other_dimension").getString();
                    drawText(g, Component.literal(String.format(Locale.ROOT, "%.0f %.0f %.0f · %s", px, py, pz, distance)), x + 7, y + cardHeight - 12, 0xFFABC2D2, textScale * 0.9f, cardWidth - 14);
                } else drawText(g, text("unloaded_short"), x + 7, y + cardHeight - 12, 0xFF96A9B9, textScale * 0.9f, cardWidth - 14);
            }
        }
        if (mouseX >= x + 4 && mouseX < x + cardWidth - 38 && mouseY >= y + 3 && mouseY < y + cardHeight) {
            double days = entry.friendshipDays(ClientFriendships.clock());
            Component duration = days < 0 ? text("duration_unknown") : text("duration").append(format(days)).append(text("days"));
            hoveredTooltip = entry.individual() ? List.of(duration, Component.literal(entry.entityId().toString())) : List.of(duration);
        }
    }
    private void renderCardTooltip(GuiGraphics g, int mouseX, int mouseY) {
        float scale = 0.8f; int w = (int) (hoveredTooltip.stream().mapToInt(font::width).max().orElse(0) * scale) + 12;
        int h = hoveredTooltip.size() * 11 + 8;
        int x = Math.max(2, Math.min(mouseX + 10, (int) canvasWidth - w - 2)), y = Math.max(2, Math.min(mouseY + 10, (int) canvasHeight - h - 2));
        g.pose().pushPose(); g.pose().translate(0, 0, 500); g.fill(x, y, x + w, y + h, 0xF5121720);
        for (int i = 0; i < hoveredTooltip.size(); i++) drawText(g, hoveredTooltip.get(i), x + 6, y + 5 + i * 11, 0xFFE3E8EF, scale, w - 12);
        g.pose().popPose();
    }
    private void refreshTracked() {
        tracked.clear();
        if (previewLevel != minecraft.level) {
            previewLevel = minecraft.level; previews.clear(); failedPreviews.clear(); entityKinds.clear();
        }
        if (minecraft.level != null) for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!entity.isRemoved()) tracked.put(entity.getUUID(), entity);
        }
    }
    private Entity findLive(FriendshipEntry entry) {
        Entity entity = entry.individual() ? tracked.get(entry.entityId()) : null;
        return entity == null || entity.isRemoved() ? null : entity;
    }
    private AlbumQuery.EntityKind entityKind(FriendshipEntry entry) {
        Entity live = findLive(entry);
        if (live != null) return live instanceof LivingEntity ? AlbumQuery.EntityKind.LIVING : AlbumQuery.EntityKind.NON_LIVING;
        CompoundTag info = ClientFriendships.live(entry.key());
        if (info.contains("living")) return info.getBoolean("living") ? AlbumQuery.EntityKind.LIVING : AlbumQuery.EntityKind.NON_LIVING;
        return entityKinds.computeIfAbsent(previewKey(entry), key -> {
            ResourceLocation id = ResourceLocation.tryParse(entry.typeId());
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return AlbumQuery.EntityKind.UNKNOWN;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type == EntityType.PLAYER || net.minecraft.world.entity.ai.attributes.DefaultAttributes.hasSupplier(type)) return AlbumQuery.EntityKind.LIVING;
            // Classification requires a real runtime class; a missing factory is not evidence of a nonliving entity.
            Entity entity = preview(entry);
            return entity == null ? AlbumQuery.EntityKind.UNKNOWN : entity instanceof LivingEntity ? AlbumQuery.EntityKind.LIVING : AlbumQuery.EntityKind.NON_LIVING;
        });
    }
    private static String previewKey(FriendshipEntry entry) {
        // Player skin lookup and cached previews must keep each recorded UUID distinct.
        return entry.individual() && BuiltInRegistries.ENTITY_TYPE.getKey(net.minecraft.world.entity.EntityType.PLAYER).toString().equals(entry.typeId())
                ? entry.key() : entry.typeId();
    }
    private Entity preview(FriendshipEntry entry) {
        if (failedPreviews.contains(previewKey(entry)) || minecraft.level == null) return null;
        Entity result = previews.computeIfAbsent(previewKey(entry), key -> {
            try {
                ResourceLocation id = ResourceLocation.tryParse(entry.typeId());
                if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
                if (type == EntityType.PLAYER) return entry.individual() ? new RemotePlayer(minecraft.level, new GameProfile(entry.entityId(), entry.name())) : null;
                return type.create(minecraft.level);
            } catch (RuntimeException error) { failedPreviews.add(key); DontHurtThem.LOGGER.warn("Cannot create friendship portrait for {}", key, error); return null; }
        });
        if (result == null) failedPreviews.add(previewKey(entry));
        return result;
    }
    private boolean suggestionsVisible() { return registry.isFocused() && completion.visible(); }
    private int suggestionRows() { return Math.min(5, completion.matches().size()); }
    private int suggestionTop() { return registry.getY() - 4 - suggestionRows() * 12; }
    private int suggestionWidth() { return Math.min(panelWidth - 24, Math.max(registry.getWidth(), completion.matches().stream().mapToInt(font::width).max().orElse(0) + 12)); }
    private boolean overSuggestions(double x, double y) {
        return suggestionsVisible() && x >= registry.getX() && x < registry.getX() + suggestionWidth() && y >= suggestionTop() && y < registry.getY() - 4;
    }
    private boolean overSuggestionPopup(double x, double y) {
        return suggestionsVisible() && x >= registry.getX() - 1 && x < registry.getX() + suggestionWidth() + 1 && y >= suggestionTop() - 13 && y < registry.getY() - 3;
    }
    private void renderSuggestions(GuiGraphics g, int mouseX, int mouseY) {
        if (!suggestionsVisible()) return;
        int count = suggestionRows(), w = suggestionWidth(), x = registry.getX(), y = suggestionTop();
        if (completion.selection() < suggestionOffset) suggestionOffset = completion.selection();
        if (completion.selection() >= suggestionOffset + count) suggestionOffset = completion.selection() - count + 1;
        g.pose().pushPose(); g.pose().translate(0, 0, 400);
        g.fill(x - 1, y - 13, x + w + 1, registry.getY() - 3, 0xFF15212D);
        drawText(g, text("completion_hint"), x + 3, y - 10, 0xFFA9C2D3, 0.8f, w - 6);
        for (int row = 0; row < count; row++) {
            int index = suggestionOffset + row;
            boolean chosen = index == completion.selection(), hovered = mouseX >= x && mouseX < x + w && mouseY >= y + row * 12 && mouseY < y + (row + 1) * 12;
            if (chosen || hovered) g.fill(x, y + row * 12, x + w, y + (row + 1) * 12, chosen ? 0xFF365775 : 0xFF283D51);
            drawText(g, Component.literal(completion.matches().get(index)), x + 4, y + row * 12 + 2, chosen ? 0xFFFFFF80 : 0xFFE5F4EC, 0.85f, w - 8);
        }
        g.pose().popPose();
    }
    @Override
    public boolean mouseClicked(double rawX, double rawY, int button) {
        double mouseX = rawX / uiScale, mouseY = rawY / uiScale;
        if (overSuggestionPopup(mouseX, mouseY)) {
            if (button == 0 && overSuggestions(mouseX, mouseY)) { completion.select(suggestionOffset + (int) ((mouseY - suggestionTop()) / 12)); applyCompletion(); completion.dismiss(); }
            return true;
        }
        boolean inputClicked = registry.isMouseOver(mouseX, mouseY);
        boolean searchClicked = search.isMouseOver(mouseX, mouseY);
        if (!inputClicked) completion.dismiss();
        if (!inputClicked && !searchClicked && (getFocused() == registry || getFocused() == search)) setFocused(null);
        if (inputClicked && button == 0) { updateCompletion(registry.getValue()); completion.show(); }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        List<FriendshipEntry> shown = entries();
        for (int i = 0; i < capacity && page * capacity + i < shown.size(); i++) {
            int x = cardLeft + i % columns * (cardWidth + 8), y = top + 46 + i / columns * (cardHeight + 8);
            if (mouseY < y + 3 || mouseY >= y + 18 || mouseX < x || mouseX >= x + cardWidth) continue;
            FriendshipEntry entry = shown.get(page * capacity + i);
            if (mouseX >= x + cardWidth - 34 && mouseX < x + cardWidth - 18) { send("favorite", entry.key()); return true; }
            if (mouseX >= x + cardWidth - 18) { send("remove", entry.key()); return true; }
            CompoundTag info = ClientFriendships.live(entry.key()); Entity live = findLive(entry);
            if (entry.individual() && !ClientFriendships.dead(entry.key()) && info.getBoolean("renameable")) {
                minecraft.setScreen(new RenameFriendScreen(this, entry.entityId(), displayName(entry, live, info))); return true;
            }
        }
        return false;
    }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(x / uiScale, y / uiScale, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return super.mouseDragged(x / uiScale, y / uiScale, button, dx / uiScale, dy / uiScale); }
    @Override
    public boolean mouseScrolled(double x, double y, double sy) {
        if (sy != 0 && overSuggestions(x / uiScale, y / uiScale)) { completion.move(sy > 0 ? -1 : 1); tabCompleted = false; return true; }
        return super.mouseScrolled(x / uiScale, y / uiScale, sy);
    }
    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (registry.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE && completion.visible()) { completion.dismiss(); return true; }
            if ((key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) && !completion.matches().isEmpty()) { completion.move(key == GLFW.GLFW_KEY_UP ? -1 : 1); tabCompleted = false; return true; }
            if (key == GLFW.GLFW_KEY_TAB && !completion.matches().isEmpty()) { if (tabCompleted || hasShiftDown()) completion.move(hasShiftDown() ? -1 : 1); completion.show(); applyCompletion(); return true; }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (completion.visible() && !registry.getValue().equals(completion.selected())) { applyCompletion(); completion.dismiss(); } else addType(); return true;
            }
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
    private static String format(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private static net.minecraft.network.chat.MutableComponent text(String key) { return Component.translatable("screen.dont_hurt_them." + key); }
    private static void send(String action, String key) { FriendshipNetwork.sendToServer(new FriendshipNetwork.Action(action, key)); }
}
