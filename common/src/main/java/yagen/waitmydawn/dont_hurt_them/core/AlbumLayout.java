package yagen.waitmydawn.dont_hurt_them.core;

/** Virtual canvas keeps dense pages legible and portrait-oriented independently of Minecraft GUI scale. */
public record AlbumLayout(float scale, float canvasWidth, float canvasHeight, int left, int top, int panelWidth,
                          int panelHeight, int columns, int rows, int cardWidth, int cardHeight, int cardLeft) {
    public static AlbumLayout create(int screenWidth, int screenHeight, int capacity) {
        if (capacity != 2 && capacity != 4 && capacity != 8) throw new IllegalArgumentException("Unsupported page size");
        int columns = capacity == 2 ? 2 : 4, rows = capacity == 8 ? 2 : 1;
        float scale = Math.min(1f, Math.min(screenWidth / (capacity == 8 ? 780f : 640f), screenHeight / (capacity == 8 ? 520f : 360f)));
        float canvasWidth = screenWidth / scale, canvasHeight = screenHeight / scale;
        int panelWidth = Math.min((int) canvasWidth - 16, 800), panelHeight = Math.min((int) canvasHeight - 16, 560);
        int left = ((int) canvasWidth - panelWidth) / 2, top = ((int) canvasHeight - panelHeight) / 2;
        int cardWidth = (panelWidth - 24 - (columns - 1) * 8) / columns;
        if (capacity == 2) cardWidth = Math.min(220, cardWidth);
        int cardHeight = (panelHeight - 106 - (rows - 1) * 8) / rows;
        int cardLeft = left + (panelWidth - (columns * cardWidth + (columns - 1) * 8)) / 2;
        return new AlbumLayout(scale, canvasWidth, canvasHeight, left, top, panelWidth, panelHeight, columns, rows, cardWidth, cardHeight, cardLeft);
    }
    public int cardX(int index) { return cardLeft + index % columns * (cardWidth + 8); }
    public int cardY(int index) { return top + 46 + index / columns * (cardHeight + 8); }
}
