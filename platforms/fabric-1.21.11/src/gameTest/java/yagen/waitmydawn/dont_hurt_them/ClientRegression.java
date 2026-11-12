package yagen.waitmydawn.dont_hurt_them;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import yagen.waitmydawn.dont_hurt_them.core.AlbumLayout;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import java.nio.file.Files;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import yagen.waitmydawn.dont_hurt_them.client.ClientFriendships;
import yagen.waitmydawn.dont_hurt_them.client.EntityPortrait;
import yagen.waitmydawn.dont_hurt_them.client.FriendshipScreen;
import yagen.waitmydawn.dont_hurt_them.client.RenameFriendScreen;
import yagen.waitmydawn.dont_hurt_them.config.ClientConfigs;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipEntry;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class ClientRegression implements ClientModInitializer {
    private record Fixtures(UUID individual, UUID named, UUID type, UUID control) {}
    private static final Set<UUID> observedNames = new HashSet<>();
    private static int frames, portraits;
    private static boolean active;
    private static CompletableFuture<Void> cardPixels;
    private final CompletableFuture<Fixtures> fixtures = new CompletableFuture<>();
    private long deadline;
    private boolean preparing;
    private int phase, originalCapacity;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("dont_hurt_them.clientRegression")) return;
        active = true;
        deadline = System.nanoTime() + 180_000_000_000L;
        originalCapacity = ClientConfigs.CARDS_PER_PAGE.get();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public static void frame(Screen screen) {
        if (active && (screen instanceof FriendshipScreen || screen instanceof RenameFriendScreen)) frames++;
    }
    public static void portrait() { if (active) portraits++; }
    public static void name(Entity entity) {
        if (!active) return;
        check(!EntityPortrait.rendering(), "Album portraits must not submit world name tags");
        observedNames.add(entity.getUUID());
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("DHT_CLIENT_REGRESSION: " + message);
    }
    private static void mutatePlayer(Minecraft mc, Consumer<ServerPlayer> mutation) {
        var server = mc.getSingleplayerServer();
        UUID uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            mutation.accept(player);
            player.inventoryMenu.broadcastChanges();
        });
    }
    private static Entity entity(Minecraft mc, UUID uuid) {
        for (Entity entity : mc.level.entitiesForRendering()) if (entity.getUUID().equals(uuid)) return entity;
        return null;
    }
    private static Mob spawn(ServerLevel level, EntityType<? extends Mob> type, int x) {
        Mob mob = type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        mob.setPos(x, 66, 6);
        mob.setNoAi(true); mob.setNoGravity(true); mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return mob;
    }
    private static void befriend(ServerPlayer player, Entity entity, boolean type) {
        FriendshipService.list(player).put(new FriendshipEntry(type ? null : entity.getUUID(),
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), entity.getName().getString(), false));
    }
    private void prepare(Minecraft mc) {
        preparing = true;
        mc.options.pauseOnLostFocus = false;
        mutatePlayer(mc, player -> {
            ServerLevel level = (ServerLevel) player.level();
            player.setGameMode(GameType.CREATIVE);
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 12; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 64, z), Blocks.STONE.defaultBlockState());
            }
            player.teleportTo(0.5, 66, 0.5);
            player.setNoGravity(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistries.PROOF_OF_FRIENDSHIP.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            Mob individual = spawn(level, EntityType.COW, -3);
            Mob named = spawn(level, EntityType.COW, -1);
            named.setCustomName(Component.literal("Regression friend"));
            Mob type = spawn(level, EntityType.HORSE, 2);
            Mob control = spawn(level, EntityType.PIG, 4);
            befriend(player, individual, false); befriend(player, named, false); befriend(player, type, true);
            Mob deceased = spawn(level, EntityType.COW, 6);
            deceased.setCustomName(Component.literal("Deceased friend"));
            befriend(player, deceased, false);
            FriendshipService.changed(player);
            deceased.hurtServer(level, deceased.damageSources().genericKill(), 10000);
            FriendshipService.changed(player);
            fixtures.complete(new Fixtures(individual.getUUID(), named.getUUID(), type.getUUID(), control.getUUID()));
        });
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean hasName(Minecraft mc, Entity entity) {
        mc.getEntityRenderDispatcher().crosshairPickEntity = null;
        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        return ((EntityRenderState) renderer.createRenderState(entity, 1)).nameTag != null;
    }
    private static void assertNames(Minecraft mc, Fixtures f, boolean visible) {
        check(hasName(mc, entity(mc, f.individual())) == visible, "Unnamed individual name visibility=" + visible);
        check(hasName(mc, entity(mc, f.named())) == visible, "Named individual name visibility=" + visible);
        check(hasName(mc, entity(mc, f.type())) == visible, "Type friend name visibility=" + visible);
        check(!hasName(mc, entity(mc, f.control())), "Unrelated entity must keep vanilla name visibility");
    }
    private static boolean cardNamesVisible(Minecraft mc) {
        if (cardPixels == null) {
            var capture = new CompletableFuture<Void>();
            cardPixels = capture;
            Screen screen = mc.screen;
            int capacity = ClientConfigs.CARDS_PER_PAGE.get();
            AlbumLayout layout = AlbumLayout.create(screen.width, screen.height, capacity);
            var entries = ClientFriendships.list().sorted();
            Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
                try (image) {
                    var folder = mc.gameDirectory.toPath().resolve("screenshots");
                    Files.createDirectories(folder);
                    image.writeToFile(folder.resolve("album-" + capacity + ".png"));
                    double sx = layout.scale() * image.getWidth() / screen.width;
                    double sy = layout.scale() * image.getHeight() / screen.height;
                    for (int i = 0; i < Math.min(capacity, entries.size()); i++) {
                        var entry = entries.get(i);
                        int expected = ClientFriendships.dead(entry.key()) ? 0x92979D
                                : entry.individual() ? FriendshipList.INDIVIDUAL_COLOR : FriendshipList.TYPE_COLOR;
                        int visible = 0;
                        int x1 = (int) Math.floor((layout.cardX(i) + 7) * sx);
                        int x2 = (int) Math.ceil((layout.cardX(i) + layout.cardWidth() - 41) * sx);
                        int y1 = (int) Math.floor((layout.cardY(i) + 7) * sy);
                        int y2 = (int) Math.floor((layout.cardY(i) + 15) * sy);
                        for (int y = y1; y < y2; y++) for (int x = x1; x < x2; x++) {
                            int pixel = image.getPixel(x, y);
                            boolean match = true;
                            for (int shift : new int[]{0, 8, 16}) {
                                if (Math.abs((pixel >> shift & 255) - (expected >> shift & 255)) > 35) match = false;
                            }
                            if (match) visible++;
                        }
                        check(visible >= 4, "Card name has no visible glyph pixels: " + entry.name() + ", capacity=" + capacity);
                    }
                    DontHurtThem.LOGGER.info("DHT_CLIENT_REGRESSION: card-name pixels passed at capacity {}", capacity);
                    capture.complete(null);
                } catch (Throwable failure) { capture.completeExceptionally(failure); }
            });
            return false;
        }
        if (!cardPixels.isDone()) return false;
        cardPixels.join();
        return true;
    }
    private static void openAlbum(Minecraft mc, int capacity, boolean suggestions) {
        ClientConfigs.setCardsPerPage(capacity);
        cardPixels = null;
        frames = 0; portraits = 0;
        mc.setScreen(new FriendshipScreen());
        if (suggestions) {
            EditBox input = (EditBox) mc.screen.children().stream().filter(child -> child instanceof EditBox).findFirst().orElseThrow();
            input.setValue("minecraft:co"); mc.screen.setFocused(input); input.setFocused(true);
        }
    }
    private void tick(Minecraft mc) {
        if (!active) return;
        check(System.nanoTime() < deadline, "Timed out in phase " + phase + " screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getName()));
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (!preparing) { prepare(mc); return; }
        if (!fixtures.isDone()) return;
        Fixtures f = fixtures.join();
        if (entity(mc, f.individual()) == null || entity(mc, f.named()) == null || entity(mc, f.type()) == null || entity(mc, f.control()) == null
                || mc.getEntityRenderDispatcher().camera == null || !ClientFriendships.list().contains("entity/" + f.individual())) return;
        if (!ClientFriendships.list().entries().stream().anyMatch(entry -> ClientFriendships.dead(entry.key()))) return;
        if (phase == 0 && mc.getEntityRenderDispatcher().distanceToSqr(entity(mc, f.individual())) >= 4096) return;
        switch (phase) {
            case 0 -> {
                if (!mc.player.getMainHandItem().is(ModRegistries.PROOF_OF_FRIENDSHIP.get())) return;
                assertNames(mc, f, true);
                DontHurtThem.LOGGER.info("DHT_CLIENT_REGRESSION: main-hand names passed");
                mutatePlayer(mc, player -> {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModRegistries.PROOF_OF_FRIENDSHIP.get()));
                }); phase++;
            }
            case 1 -> {
                if (!mc.player.getMainHandItem().isEmpty() || !mc.player.getOffhandItem().is(ModRegistries.PROOF_OF_FRIENDSHIP.get())) return;
                assertNames(mc, f, true);
                mutatePlayer(mc, player -> player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)); phase++;
            }
            case 2 -> {
                if (!mc.player.getMainHandItem().isEmpty() || !mc.player.getOffhandItem().isEmpty()) return;
                assertNames(mc, f, false);
                DontHurtThem.LOGGER.info("DHT_CLIENT_REGRESSION: off-hand and unequipped names passed");
                ClientConfigs.setCardsPerPage(4);
                cardPixels = null;
                frames = 0; portraits = 0;
                mutatePlayer(mc, player -> {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistries.PROOF_OF_FRIENDSHIP.get()));
                    FriendshipService.sync(player, true);
                }); phase++;
            }
            case 3, 4, 5 -> {
                if (!(mc.screen instanceof FriendshipScreen) || frames < 8) return;
                if (!cardNamesVisible(mc)) return;
                check(portraits > 0, "Album must render entity portraits");
                DontHurtThem.LOGGER.info("DHT_CLIENT_REGRESSION: album capacity {} passed after {} frames", ClientConfigs.CARDS_PER_PAGE.get(), frames);
                if (phase == 3) openAlbum(mc, 2, false);
                else if (phase == 4) openAlbum(mc, 8, true);
                else {
                    frames = 0;
                    mc.setScreen(new RenameFriendScreen(mc.screen, f.named(), "Regression friend"));
                }
                phase++;
            }
            case 6 -> {
                if (!(mc.screen instanceof RenameFriendScreen) || frames < 8) return;
                cardPixels = null;
                frames = 0; portraits = 0; mc.screen.onClose(); phase++;
            }
            case 7 -> {
                if (!(mc.screen instanceof FriendshipScreen) || frames < 8) return;
                if (!cardNamesVisible(mc)) return;
                check(portraits > 0, "Returning from rename must restore album portraits");
                ClientConfigs.setCardsPerPage(originalCapacity);
                active = false;
                DontHurtThem.LOGGER.info("DHT_CLIENT_REGRESSION PASSED: names, item gating, album frames, completion, rename and return");
                mc.stop();
            }
            default -> throw new AssertionError("Unexpected regression phase " + phase);
        }
    }
}
