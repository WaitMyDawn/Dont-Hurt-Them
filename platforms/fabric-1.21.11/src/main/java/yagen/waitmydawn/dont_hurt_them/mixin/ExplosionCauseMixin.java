package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

@Mixin(ServerExplosion.class)
public abstract class ExplosionCauseMixin {
    @WrapMethod(method = "explode")
    private int dontHurtThem$explode(Operation<Integer> original) {
        var owner = DamageProtection.owner(((ServerExplosion) (Object) this).getDirectSourceEntity());
        CauseContext.push(owner != null ? owner : CauseContext.current());
        try { return original.call(); }
        finally { CauseContext.pop(); }
    }
}
