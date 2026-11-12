package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

@Mixin(Explosion.class)
public abstract class ExplosionCauseMixin {
    @WrapMethod(method = "finalizeExplosion")
    private void dontHurtThem$explode(boolean particles, Operation<Void> original) {
        Explosion explosion = (Explosion) (Object) this;
        var owner = DamageProtection.owner(explosion.getDirectSourceEntity());
        CauseContext.push(owner != null ? owner : CauseContext.current());
        try { original.call(particles); }
        finally { CauseContext.pop(); }
    }
}
