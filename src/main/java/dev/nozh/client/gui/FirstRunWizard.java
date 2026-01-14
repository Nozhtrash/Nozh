package dev.nozh.client.gui;

import dev.nozh.client.ConfigPresets;
import dev.nozh.core.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * First Run Wizard - A guided setup for new users.
 * 
 * Steps:
 * 1. Welcome
 * 2. Hardware Analysis (Mocked for now)
 * 3. Profile Selection
 * 4. Finish
 */
public class FirstRunWizard extends Screen {
    private final Screen parent;
    private int step = 0;

    public FirstRunWizard(Screen parent) {
        super(Text.literal("Nozh Setup"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (step == 0) {
            // Step 1: Welcome
            addDrawableChild(ButtonWidget.builder(Text.literal("Begin Setup"), btn -> {
                step++;
                this.clearAndInit();
            }).dimensions(centerX - 100, centerY + 20, 200, 20).build());
        } else if (step == 1) {
            // Step 2: Choose Profile
            addDrawableChild(ButtonWidget.builder(Text.literal("Optimize for FPS (Potato)"), btn -> {
                ConfigPresets.applyLowEnd();
                nextStep();
            }).dimensions(centerX - 100, centerY - 10, 200, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Balanced"), btn -> {
                ConfigPresets.applyMidRange();
                nextStep();
            }).dimensions(centerX - 100, centerY + 15, 200, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("High Fidelity"), btn -> {
                ConfigPresets.applyHighEnd();
                nextStep();
            }).dimensions(centerX - 100, centerY + 40, 200, 20).build());
        } else {
            // Step 3: Finish
            addDrawableChild(ButtonWidget.builder(Text.literal("Start Playing"), btn -> {
                ConfigManager.getConfig().tutorialStep = 3; // Mark wizard complete
                ConfigManager.saveAndNotify();
                this.client.setScreen(parent);
            }).dimensions(centerX - 100, centerY + 20, 200, 20).build());
        }
    }

    private void nextStep() {
        step++;
        this.clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, "NOZH OPTIMIZER SETUP", centerX, centerY - 60, 0x55FF55);

        if (step == 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, "Welcome! Let's get your game optimized.", centerX,
                    centerY - 30, 0xFFFFFF);
        } else if (step == 1) {
            context.drawCenteredTextWithShadow(this.textRenderer, "Choose your target experience:", centerX,
                    centerY - 40, 0xFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, "You are all set!", centerX, centerY - 30, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
