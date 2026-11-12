package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

@Mixin(Explosion.class)
public abstract class ExplosionTargetsMixin {
    @ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> dontHurtThem$targets(List<Entity> entities) {
        var owner = DamageProtection.owner(((Explosion) (Object) this).getDirectSourceEntity());
        if (owner == null) owner = CauseContext.current();
        var cause = owner;
        entities.removeIf(entity -> DamageProtection.protects(entity, cause));
        return entities;
    }
}
