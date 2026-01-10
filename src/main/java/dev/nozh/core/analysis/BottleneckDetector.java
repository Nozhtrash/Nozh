package dev.nozh.core.analysis;

import dev.nozh.NozhConstants;
import dev.nozh.core.util.RollingAverage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

/**
 * Detecta cuellos de botella de rendimiento (CPU vs GPU).
 * 
 * <p>Analiza tick time (CPU) vs render time (GPU) para identificar
 * qué componente está limitando el rendimiento.
 * 
 * @author Nozh Team
 * @since 0.5.0
 */
public final class BottleneckDetector {
    private final RollingAverage tickTime = new RollingAverage(100);
    private final RollingAverage renderTime = new RollingAverage(100);
    
    /**
     * Tipos de cuellos de botella detectables.
     */
    public enum Bottleneck {
        CPU_BOUND,      // Tick time alto, render ok
        GPU_BOUND,      // Render time alto, tick ok
        BALANCED,       // Ambos similares
        NEITHER         // Ambos bajos (sin cuello de botella)
    }
    
    /**
     * Muestrea métricas de rendimiento actuales.
     * 
     * @param client instancia de MinecraftClient
     */
    public void sample(MinecraftClient client) {
        if (client == null) {
            return;
        }
        
        try {
            // Estimar tiempo de tick (lógica CPU)
            double tickMs = estimateTickTime(client);
            tickTime.add(tickMs);
            
            // Obtener tiempo total de frame
            double frameMs = client.getLastFrameDuration();
            if (frameMs < 1.0) {
                frameMs *= 1000.0; // Convertir a ms si es necesario
            }
            
            // Tiempo de render = total - tick (aproximación)
            double renderMs = Math.max(0, frameMs - tickMs);
            renderTime.add(renderMs);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Error al muestrear datos de bottleneck", e);
        }
    }
    
    /**
     * Detecta el tipo de cuello de botella actual.
     * 
     * @return tipo de bottleneck detectado
     */
    public Bottleneck detect() {
        if (!tickTime.hasEnoughSamples() || !renderTime.hasEnoughSamples()) {
            return Bottleneck.NEITHER;
        }
        
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        double total = avgTick + avgRender;
        
        // Sin cuello de botella si corre a 60+ FPS
        if (total < 16.67) {
            return Bottleneck.NEITHER;
        }
        
        double tickRatio = avgTick / total;
        
        if (tickRatio > 0.65) {
            return Bottleneck.CPU_BOUND; // Tick es 65%+ del tiempo de frame
        } else if (tickRatio < 0.35) {
            return Bottleneck.GPU_BOUND; // Render es 65%+ del tiempo de frame
        } else {
            return Bottleneck.BALANCED;
        }
    }
    
    /**
     * Obtiene análisis detallado con recomendaciones.
     * 
     * @return string con análisis completo
     */
    public String getDetailedAnalysis() {
        Bottleneck type = detect();
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        double total = avgTick + avgRender;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Análisis de Bottleneck:\n");
        sb.append(String.format("  Tipo: %s\n", type));
        sb.append(String.format("  Tick time: %.2f ms\n", avgTick));
        sb.append(String.format("  Render time: %.2f ms\n", avgRender));
        sb.append(String.format("  Total: %.2f ms (%.1f FPS)\n", total, 1000.0 / total));
        
        sb.append("\nRecomendaciones:\n");
        switch (type) {
            case CPU_BOUND:
                sb.append("  - Reducir simulation distance\n");
                sb.append("  - Bajar cantidad de entidades\n");
                sb.append("  - Simplificar redstone complejo\n");
                sb.append("  - Desactivar mods innecesarios\n");
                break;
            case GPU_BOUND:
                sb.append("  - Reducir render distance\n");
                sb.append("  - Bajar calidad gráfica\n");
                sb.append("  - Desactivar shaders\n");
                sb.append("  - Reducir partículas\n");
                break;
            case BALANCED:
                sb.append("  - CPU y GPU están limitando por igual\n");
                sb.append("  - Aplicar optimizaciones balanceadas\n");
                break;
            case NEITHER:
                sb.append("  - ¡Rendimiento óptimo!\n");
                sb.append("  - No se necesitan optimizaciones\n");
                break;
        }
        
        return sb.toString();
    }
    
    /**
     * Estima el tiempo de tick desde fuentes disponibles.
     * 
     * @param client cliente de Minecraft
     * @return tiempo de tick estimado en milisegundos
     */
    private double estimateTickTime(MinecraftClient client) {
        try {
            // Intentar servidor integrado primero (singleplayer)
            MinecraftServer server = client.getServer();
            if (server != null) {
                // Convertir nanosegundos a milisegundos
                return server.getTickTime() / 1_000_000.0;
            }
            
            // Fallback: estimar basado en complejidad del mundo
            if (client.world != null && client.player != null) {
                // Estimación básica: ~10ms + overhead por entidades cercanas
                int nearbyEntities = client.world.getEntitiesByClass(
                    net.minecraft.entity.Entity.class,
                    client.player.getBoundingBox().expand(32),
                    entity -> true
                ).size();
                
                double baseTickTime = 10.0;
                double entityOverhead = Math.min(nearbyEntities * 0.02, 30.0);
                
                return baseTickTime + entityOverhead;
            }
            
            return 10.0; // Valor por defecto seguro
            
        } catch (Exception e) {
            return 10.0;
        }
    }
    
    /**
     * Obtiene el promedio de tick time.
     * 
     * @return tiempo promedio de tick en ms
     */
    public double getAverageTickTime() {
        return tickTime.getAverage();
    }
    
    /**
     * Obtiene el promedio de render time.
     * 
     * @return tiempo promedio de render en ms
     */
    public double getAverageRenderTime() {
        return renderTime.getAverage();
    }
    
    /**
     * Resetea todos los datos recolectados.
     */
    public void reset() {
        tickTime.clear();
        renderTime.clear();
    }
}