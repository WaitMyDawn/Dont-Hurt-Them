package yagen.waitmydawn.dont_hurt_them.registries;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipSerialization;
import yagen.waitmydawn.dont_hurt_them.item.ProofOfFriendshipItem;

public final class ModRegistries {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DontHurtThem.MODID);
    public static final Supplier<Item> PROOF_OF_FRIENDSHIP = ITEMS.register("proof_of_friendship",
            () -> new ProofOfFriendshipItem(new Item.Properties().stacksTo(1)));
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DontHurtThem.MODID);
    public static final Supplier<AttachmentType<FriendshipList>> FRIENDSHIPS = ATTACHMENTS.register("friendships",
            () -> AttachmentType.builder(FriendshipList::new)
                    .serialize(CompoundTag.CODEC.xmap(FriendshipSerialization::read, FriendshipSerialization::write))
                    .copyOnDeath().build());

    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(PROOF_OF_FRIENDSHIP.get());
    }

    private ModRegistries() {}
}
