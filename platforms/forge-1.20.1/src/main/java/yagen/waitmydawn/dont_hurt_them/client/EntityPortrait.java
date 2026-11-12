package yagen.waitmydawn.dont_hurt_them.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yagen.waitmydawn.dont_hurt_them.core.PortraitFit;

public final class EntityPortrait {
    private static int renderingDepth;
    public static boolean rendering() { return renderingDepth > 0; }
    public static void render(GuiGraphics graphics, Entity entity, int x, int y, int width, int height, int mouseX, int mouseY) {
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        Quaternionf camera = new Quaternionf(dispatcher.cameraOrientation());
        float yaw = entity.getYRot(), pitch = entity.getXRot();
        LivingEntity living = entity instanceof LivingEntity value ? value : null;
        float body = living == null ? 0 : living.yBodyRot;
        float head = living == null ? 0 : living.yHeadRot;
        float oldHead = living == null ? 0 : living.yHeadRotO;
        float turn = (float) Math.atan((x + width / 2f - mouseX) / 90f);
        float tilt = (float) Math.atan((y + height / 2f - mouseY) / 90f);
        float scale = PortraitFit.scale(width, height, entity.getBbWidth(), entity.getBbHeight());
        Vector3f min = graphics.pose().last().pose().transformPosition(new Vector3f(x, y, 0));
        Vector3f max = graphics.pose().last().pose().transformPosition(new Vector3f(x + width, y + height, 0));
        graphics.enableScissor((int) Math.floor(min.x), (int) Math.floor(min.y), (int) Math.ceil(max.x), (int) Math.ceil(max.y));
        graphics.pose().pushPose();
        renderingDepth++;
        try {
            entity.setYRot(150 + turn * 20); entity.setXRot(-tilt * 8);
            if (living != null) { living.yBodyRot = 150 + turn * 12; living.yHeadRot = entity.getYRot(); living.yHeadRotO = living.yHeadRot; }
            graphics.pose().translate(x + width / 2f, y + height / 2f + entity.getBbHeight() * scale / 2f, 100);
            graphics.pose().scale(scale, scale, -scale);
            graphics.pose().mulPose(new Quaternionf().rotateZ((float) Math.PI));
            Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(new Quaternionf().rotateY((float) Math.PI));
            dispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0, 1,
                    graphics.pose(), graphics.bufferSource(), 15728880));
            graphics.flush();
        } finally {
            renderingDepth--;
            entity.setYRot(yaw); entity.setXRot(pitch);
            if (living != null) { living.yBodyRot = body; living.yHeadRot = head; living.yHeadRotO = oldHead; }
            dispatcher.overrideCameraOrientation(camera);
            dispatcher.setRenderShadow(true);
            graphics.pose().popPose();
            graphics.disableScissor();
            Lighting.setupFor3DItems();
        }
    }
    private EntityPortrait() {}
}
