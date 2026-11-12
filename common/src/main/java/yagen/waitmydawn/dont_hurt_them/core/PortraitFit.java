package yagen.waitmydawn.dont_hurt_them.core;

public final class PortraitFit {
    public static float scale(float viewportWidth, float viewportHeight, float entityWidth, float entityHeight) {
        double width = Math.max(0.4, entityWidth), height = Math.max(0.4, entityHeight);
        // A padded sphere remains inside the viewport when a long body or head rotates toward the camera.
        double diameter = Math.sqrt(height * height + 2 * width * width) * 1.4;
        return (float) (Math.max(1, Math.min(viewportWidth - 12, viewportHeight - 12)) / diameter);
    }
    private PortraitFit() {}
}
