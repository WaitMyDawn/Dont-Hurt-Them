package yagen.waitmydawn.dont_hurt_them.registries;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.item.ProofOfFriendshipItem;

public final class ModRegistries {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, DontHurtThem.MODID);
    public static final Supplier<Item> PROOF_OF_FRIENDSHIP = ITEMS.register("proof_of_friendship",
            () -> new ProofOfFriendshipItem(new Item.Properties().stacksTo(1)));
    public static final Capability<FriendshipList> FRIENDSHIPS = CapabilityManager.get(new CapabilityToken<>() {});

    public static void capabilities(RegisterCapabilitiesEvent event) { event.register(FriendshipList.class); }

    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(PROOF_OF_FRIENDSHIP.get());
    }

    private ModRegistries() {}
}
