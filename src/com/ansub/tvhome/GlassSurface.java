package com.ansub.tvhome;

import android.graphics.drawable.GradientDrawable;

/** Shared translucent chrome: one native gradient, with no blur or off-screen bitmap. */
final class GlassSurface {
    private GlassSurface() {}

    static GradientDrawable panel(float scale, boolean focused) {
        // The wallpaper supplies the colour behind both surfaces. A faint light-to-dark
        // sheen and a hairline edge give depth without sampling or blurring that wallpaper.
        GradientDrawable surface = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                focused ? new int[] {0xD04A5664, 0xCA303C4A}
                        : new int[] {0xA6323B46, 0xA62A333E});
        surface.setStroke(Math.max(1, Math.round(scale)), focused ? 0x858BC7FF : 0x24FFFFFF);
        return surface;
    }
}
