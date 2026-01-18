package dev.nozh.client.gui.toast;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class PotatoModeSuggestionToast implements Toast {
    private static final Text TITLE = Text.translatable("nozh.toast.potato_mode.title");
    private static final Text DESCRIPTION = Text.translatable("nozh.toast.potato_mode.desc");
    private static final long DURATION = 5000L;

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        context.drawTexture(TEXTURE, 0, 0, 0, 32, this.getWidth(), this.getHeight());

        context.drawText(manager.getClient().textRenderer, TITLE, 30, 7, 0xFF500050, false);
        context.drawText(manager.getClient().textRenderer, DESCRIPTION, 30, 18, 0xFF000000, false);

        // Draw Potato Icon
        context.drawItem(new ItemStack(Items.POTATO), 8, 8);

        return startTime >= DURATION ? Visibility.HIDE : Visibility.SHOW;
    }
}
