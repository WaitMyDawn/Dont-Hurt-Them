package yagen.waitmydawn.dont_hurt_them.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import yagen.waitmydawn.dont_hurt_them.core.PortraitFit;

public final class EntityPortrait {
    private static int renderingDepth;
    public static boolean rendering() { return renderingDepth > 0; }
    @SuppressWarnings("unchecked")
    public static void render(GuiGraphicsExtractor graphics, Entity entity, int x, int y, int width, int height, int mouseX, int mouseY) {
        float yaw = entity.getYRot(), pitch = entity.getXRot();
        LivingEntity living = entity instanceof LivingEntity value ? value : null;
        float body = living == null ? 0 : living.yBodyRot, head = living == null ? 0 : living.yHeadRot, oldHead = living == null ? 0 : living.yHeadRotO;
        float turn = (float) Math.atan((x + width / 2f - mouseX) / 90f);
        float tilt = (float) Math.atan((y + height / 2f - mouseY) / 90f);
        renderingDepth++;
        try {
            entity.setYRot(150 + turn * 20); entity.setXRot(-tilt * 8);
            if (living != null) { living.yBodyRot = 150 + turn * 12; living.yHeadRot = entity.getYRot(); living.yHeadRotO = living.yHeadRot; }
            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            var renderer = (EntityRenderer<Entity, EntityRenderState>) (EntityRenderer<?, ?>) dispatcher.getRenderer(entity);
            EntityRenderState state = renderer.createRenderState(entity, 1);
            state.lightCoords = 15728880; state.nameTag = null;
            // Match inventory previews: world outlines and ground shadows must not enter the GUI render queue.
            state.outlineColor = 0;
            state.shadowPieces.clear();
            Vector2f min = graphics.pose().transformPosition(new Vector2f(x, y));
            Vector2f max = graphics.pose().transformPosition(new Vector2f(x + width, y + height));
            float scale = PortraitFit.scale(width, height, entity.getBbWidth(), entity.getBbHeight()) * graphics.pose().m00();
            graphics.entity(state, scale, new Vector3f(0, state.boundingBoxHeight / 2f, 0),
                    new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(),
                    (int) Math.floor(min.x), (int) Math.floor(min.y), (int) Math.ceil(max.x), (int) Math.ceil(max.y));
        } finally {
            renderingDepth--;
            entity.setYRot(yaw); entity.setXRot(pitch);
            if (living != null) { living.yBodyRot = body; living.yHeadRot = head; living.yHeadRotO = oldHead; }
        }
    }
    private EntityPortrait() {}
}
