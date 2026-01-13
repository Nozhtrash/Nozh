package dev.nozh.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.nozh.client.gui.NozhConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * ModMenu integration for NOZH with professional UI and tooltips.
 */
@Environment(EnvType.CLIENT)
public class NozhModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return NozhConfigScreen::new;
    }
}
