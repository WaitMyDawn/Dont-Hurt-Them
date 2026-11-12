package yagen.waitmydawn.dont_hurt_them.registries;

import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipSerialization;
import yagen.waitmydawn.dont_hurt_them.item.ProofOfFriendshipItem;

public final class ModRegistries {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(DontHurtThem.MODID, path); }
    private static final Item PROOF = Registry.register(BuiltInRegistries.ITEM, id("proof_of_friendship"),
            new ProofOfFriendshipItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> PROOF_OF_FRIENDSHIP = () -> PROOF;
    public static final AttachmentType<FriendshipList> FRIENDSHIPS = AttachmentRegistry.<FriendshipList>create(id("friendships"), builder -> builder
            .initializer(FriendshipList::new).persistent(CompoundTag.CODEC.xmap(FriendshipSerialization::read, FriendshipSerialization::write)).copyOnDeath());
    private static final AttachmentType<CompoundTag> PROVENANCE = AttachmentRegistry.<CompoundTag>create(id("provenance"), builder -> builder
            .initializer(CompoundTag::new).persistent(CompoundTag.CODEC));

    public static CompoundTag data(Entity entity) { return ((AttachmentTarget) entity).getAttachedOrCreate(PROVENANCE); }
    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(PROOF));
    }
    private ModRegistries() {}
}
