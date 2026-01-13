package dev.nozh.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nozh.core.telemetry.VitalsRecorder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.drawable.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * VitalsGraphWidget - Renders a real-time graph of frame times.
 * 
 * Visual Style:
 * - Gradient fill below the line.
 * - Color-coded performance (Green = 60fps+, Yellow = 30fps, Red = <30fps).
 * - Smooth lines.
 */
public class VitalsGraphWidget implements Drawable, Element, Selectable {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final VitalsRecorder recorder;
    
    private static final int COLOR_GOOD = 0xFF55FF55;   // Green
    private static final int COLOR_OK = 0xFFFFFF55;     // Yellow
    private static final int COLOR_BAD = 0xFFFF5555;    // Red
    private static final int BACKGROUND = 0x80000000;   // Semi-transparent black

    public VitalsGraphWidget(int x, int y, int width, int height, VitalsRecorder recorder) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.recorder = recorder;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Draw Background
        context.fill(x, y, x + width, y + height, BACKGROUND);
        
        // 2. Draw Grid Lines (Target FPS)
        int targetMs = 16; // 60 FPS
        int targetY = y + height - (int)((targetMs / 50.0) * height); // Scale 0-50ms
        if (targetY > y && targetY < y + height) {
            context.fill(x, targetY, x + width, targetY + 1, 0x40FFFFFF); // Faint white line
        }

        // 3. Fetch Data
        // Ideally VitalsRecorder exposes a snapshot. Assuming float[] or List<Float>
        // Note: You might need to update VitalsRecorder to expose `getFrameTimeHistory()`
        // For now, assuming a hypothetical API or we fix it in the next step.
        // let's grab the history array.
        float[] history = recorder.getFrameTimeHistory(); 
        if (history == null || history.length < 2) return;

        // 4. Render Graph
        // We use the Tessellator directly for lines because DrawContext is limited for arbitrary shapes
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        int sampleCount = history.length;
        // We only render as many points as pixels we have, or sample down
        int step = Math.max(1, sampleCount / width); 
        
        for (int i = 0; i < sampleCount; i += step) {
            // X position: map index to width
            float px = x + ((float)i / sampleCount) * width;
            
            // Y position: map frameTime (0-50ms) to height
            // value is ms. 16ms = good. 33ms = 30fps.
            float ms = history[i];
            float py = y + height - MathHelper.clamp((ms / 50.0f) * height, 0, height);
            
            // Color logic
            int color = ms < 18 ? COLOR_GOOD : (ms < 34 ? COLOR_OK : COLOR_BAD);
            
            buffer.vertex(px, py, 0).color(color).next();
        }
        
        tessellator.draw();
        RenderSystem.disableBlend();
        
        // 5. Draw Stats text
        String fpsText = "Avg: " + String.format("%.1f", 1000.0 / Math.max(1, recorder.getAverageFrameTime())) + " FPS";
        context.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer, fpsText, x + 4, y + 4, 0xFFFFFF, true);
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() { return false; }

    @Override
    public SelectionType getType() { return SelectionType.NONE; }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {}
}
