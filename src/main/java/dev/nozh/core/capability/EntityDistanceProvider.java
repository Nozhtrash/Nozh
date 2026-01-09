package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class EntityDistanceProvider implements CapabilityProvider {
    
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 5.0;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing entity distance scale");
        }
        
        try {
            double targetScale = parseScale(params[0]);
            
            if (targetScale < MIN_SCALE || targetScale > MAX_SCALE) {
                return ActionResult.invalid(
                    String.format("Entity distance scale must be %.1f-%.1f, got %.2f", 
                                  MIN_SCALE, MAX_SCALE, targetScale)
                );
            }
            
            GameOptions options = client.options;
            double oldScale = options.getEntityDistanceScaling().getValue();
            
            if (Math.abs(oldScale - targetScale) < 0.01) {
                return ActionResult.noChange("Already at target scale");
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("entity_distance", oldScale);
            
            options.getEntityDistanceScaling().setValue(targetScale);
            options.write();
            
            NozhConstants.LOGGER.info("Changed entity distance scale: {:.2f} -> {:.2f}", oldScale, targetScale);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set entity distance", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("entity_distance")) {
            throw new IllegalArgumentException("Invalid snapshot for entity distance");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        double oldScale = (double) snapshot.get("entity_distance");
        
        client.options.getEntityDistanceScaling().setValue(oldScale);
        client.options.write();
        
        NozhConstants.LOGGER.info("Rolled back entity distance to: {:.2f}", oldScale);
    }
    
    private double parseScale(Object param) {
        if (param instanceof Double) return (Double) param;
        if (param instanceof Number) return ((Number) param).doubleValue();
        return Double.parseDouble(param.toString());
    }
}
