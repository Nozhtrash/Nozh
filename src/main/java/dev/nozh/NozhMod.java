package dev.nozh;

import dev.nozh.commands.NozhCommand;
import dev.nozh.core.governor.IntegratedGovernor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;

/**
 * Main mod class for Nozh FPS Optimizer.
 * 
 * Initializes the full autonomous governor system.
 */
public class NozhMod implements ClientModInitializer {

    // Use NozhConstants as single source of truth for MOD_ID and LOGGER
    public static final String MOD_ID = NozhConstants.MOD_ID;
    public static final org.slf4j.Logger LOGGER = NozhConstants.LOGGER;
    
    private static IntegratedGovernor governor;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Nozh FPS Optimizer initializing...");
        
        // Get Minecraft client
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Set up log path
        Path logPath = client.runDirectory.toPath().resolve("logs").resolve("nozh-performance.log");
        
        // Initialize governor
        governor = new IntegratedGovernor(client, logPath);
        
        // Register commands
        NozhCommand.setGovernor(governor);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            NozhCommand.register(dispatcher);
        });
        
        LOGGER.info("Nozh FPS Optimizer initialized successfully!");
        LOGGER.info("Use /nozh to access commands");
    }

    /**
     * Get the governor instance.
     */
    public static IntegratedGovernor getGovernor() {
        return governor;
    }

    /**
     * Shutdown hook.
     */
    public static void shutdown() {
        if (governor != null) {
            governor.shutdown();
        }
    }
}
