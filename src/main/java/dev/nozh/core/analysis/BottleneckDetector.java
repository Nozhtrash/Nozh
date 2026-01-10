package dev.nozh.core.analysis;

import dev.nozh.NozhConstants;
import dev.nozh.core.util.RollingAverage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;

/**
 * Detector avanzado de cuellos de botella CPU vs GPU.
 * 
 * <p>Analiza tiempos de tick (CPU) y render (GPU) para identificar
 * qué componente está limitando el rendimiento.
 * 
 * <p>Usa métricas precisas del servidor integrado cuando está disponible,
 * con fallbacks inteligentes para modo multijugador.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 2 Sprint 4)
 */
public final class BottleneckDetector {
    private static final int SAMPLE_WINDOW = 100;
    private static final double TARGET_FRAME_TIME_MS = 16.67; // 60 FPS
    
    private final RollingAverage tickTime = new RollingAverage(SAMPLE_WINDOW);
    private final RollingAverage renderTime = new RollingAverage(SAMPLE_WINDOW);
    private final RollingAverage totalFrameTime = new RollingAverage(SAMPLE_WINDOW);
    
    /**
     * Tipos de cuello de botella detectables.
     */
    public enum Bottleneck {
        /** Tick time alto, render aceptable (CPU limitante) */
        CPU_BOUND,
        
        /** Render time alto, tick aceptable (GPU limitante) */
        GPU_BOUND,
        
        /** Ambos contribuyen similarmente */
        BALANCED,
        
        /** Rendimiento bueno, sin limitaciones */
        NEITHER,
        
        /** No hay suficientes datos para determinar */
        UNKNOWN
    }
    
    /**
     * Captura métricas de rendimiento del frame actual.
     * 
     * @param client instancia del cliente de Minecraft
     */
    public void sample(MinecraftClient client) {
        if (client == null) {
            return;
        }
        
        try {
            // Obtener tiempo total del frame
            double frameMs = getFrameTimeMs(client);
            totalFrameTime.add(frameMs);
            
            // Estimar tiempo de tick (CPU)
            double tickMs = estimateTickTimeMs(client);
            tickTime.add(tickMs);
            
            // Calcular tiempo de render (resto del frame)
            double renderMs = Math.max(0.0, frameMs - tickMs);
            renderTime.add(renderMs);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Error capturando métricas de bottleneck", e);
        }
    }
    
    /**
     * Obtiene el tiempo del frame más reciente en milisegundos.
     */
    private double getFrameTimeMs(MinecraftClient client) {
        try {
            // Obtener duración del último frame
            float frameDuration = client.getLastFrameDuration();
            
            // Si está en segundos, convertir a ms
            if (frameDuration < 1.0f) {
                return frameDuration * 1000.0;
            }
            return frameDuration;
            
        } catch (Exception e) {
            // Fallback: calcular desde FPS
            int fps = client.getCurrentFps();
            return fps > 0 ? 1000.0 / fps : 16.67;
        }
    }
    
    /**
     * Estima el tiempo de tick usando múltiples fuentes.
     * Prioridad: Server integrado > Profiler > Heurística
     */
    private double estimateTickTimeMs(MinecraftClient client) {
        // 1. Intento: Servidor integrado (singleplayer)
        IntegratedServer server = client.getServer();
        if (server != null) {
            try {
                // getTickTime() devuelve nanosegundos
                long tickNanos = server.getTickTime();
                if (tickNanos > 0) {
                    return tickNanos / 1_000_000.0; // convertir a ms
                }
            } catch (Exception e) {
                NozhConstants.LOGGER.debug("No se pudo obtener tickTime del servidor", e);
            }
        }
        
        // 2. Intento: Profiler del cliente (si está activo)
        try {
            var profiler = client.getProfiler();
            if (profiler != null) {
                // Nota: esto requeriría acceso a métricas internas del profiler
                // Por ahora, usamos heurística
            }
        } catch (Exception e) {
            // Ignorar
        }
        
        // 3. Fallback: Heurística basada en complejidad del mundo
        return estimateTickTimeHeuristic(client);
    }
    
    /**
     * Estimación heurística basada en complejidad del mundo.
     */
    private double estimateTickTimeHeuristic(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return 5.0; // Mínimo base
        }
        
        double baseTickTime = 5.0; // 5ms base para mundo vacío
        
        try {
            // Factor de entidades
            int entityCount = client.world.getEntityCount();
            double entityOverhead = Math.min(entityCount * 0.01, 15.0); // max 15ms
            
            // Factor de chunks cargados
            int renderDistance = client.options.getViewDistance().getValue();
            double chunkOverhead = renderDistance * 0.5; // ~0.5ms por chunk de distancia
            
            // Factor de redstone/tile entities (aproximado)
            double complexityOverhead = 2.0;
            
            return baseTickTime + entityOverhead + chunkOverhead + complexityOverhead;
            
        } catch (Exception e) {
            return 10.0; // Fallback conservador
        }
    }
    
    /**
     * Detecta el tipo de cuello de botella actual.
     * 
     * @return tipo de bottleneck detectado
     */
    public Bottleneck detect() {
        if (!tickTime.hasEnoughSamples() || !renderTime.hasEnoughSamples()) {
            return Bottleneck.UNKNOWN;
        }
        
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        double total = avgTick + avgRender;
        
        // Si el frame time total es bueno (60+ FPS), no hay bottleneck
        if (total < TARGET_FRAME_TIME_MS) {
            return Bottleneck.NEITHER;
        }
        
        // Calcular ratio de tick sobre total
        double tickRatio = avgTick / total;
        
        // CPU bound: tick time consume >65% del frame
        if (tickRatio > 0.65) {
            return Bottleneck.CPU_BOUND;
        }
        
        // GPU bound: render time consume >65% del frame (tick <35%)
        if (tickRatio < 0.35) {
            return Bottleneck.GPU_BOUND;
        }
        
        // Ambos contribuyen similarmente
        return Bottleneck.BALANCED;
    }
    
    /**
     * Genera un análisis detallado con recomendaciones.
     * 
     * @return string formateado con análisis y sugerencias
     */
    public String getDetailedAnalysis() {
        Bottleneck type = detect();
        double avgTick = tickTime.getAverage();
        double avgRender = renderTime.getAverage();
        double total = avgTick + avgRender;
        double fps = total > 0 ? 1000.0 / total : 0.0;
        
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════╗\n");
        sb.append("║    ANÁLISIS DE BOTTLENECK - NOZH     ║\n");
        sb.append("╚═══════════════════════════════════════╝\n\n");
        
        sb.append(String.format("Tipo detectado: %s\n", type));
        sb.append(String.format("Tick time (CPU): %.2f ms\n", avgTick));
        sb.append(String.format("Render time (GPU): %.2f ms\n", avgRender));
        sb.append(String.format("Frame time total: %.2f ms (%.1f FPS)\n\n", total, fps));
        
        // Agregar recomendaciones específicas
        sb.append("Recomendaciones:\n");
        switch (type) {
            case CPU_BOUND:
                sb.append("  🔴 CPU es el cuello de botella\n");
                sb.append("  • Reducir simulation distance\n");
                sb.append("  • Limitar entity count\n");
                sb.append("  • Desactivar redstone complejo\n");
                sb.append("  • Considerar mods de optimización CPU\n");
                break;
                
            case GPU_BOUND:
                sb.append("  🔴 GPU es el cuello de botella\n");
                sb.append("  • Reducir render distance\n");
                sb.append("  • Bajar calidad gráfica (FAST mode)\n");
                sb.append("  • Desactivar shaders\n");
                sb.append("  • Reducir partículas\n");
                break;
                
            case BALANCED:
                sb.append("  🟡 CPU y GPU contribuyen igualmente\n");
                sb.append("  • Aplicar optimizaciones balanceadas\n");
                sb.append("  • Reducir tanto render como simulation distance\n");
                break;
                
            case NEITHER:
                sb.append("  🟢 Rendimiento óptimo\n");
                sb.append("  • No se requieren optimizaciones\n");
                break;
                
            case UNKNOWN:
                sb.append("  ⚪ Insuficientes datos para análisis\n");
                sb.append("  • Esperar más muestras...\n");
                break;
        }
        
        return sb.toString();
    }
    
    /**
     * @return promedio de tick time en milisegundos
     */
    public double getAverageTickTime() {
        return tickTime.getAverage();
    }
    
    /**
     * @return promedio de render time en milisegundos
     */
    public double getAverageRenderTime() {
        return renderTime.getAverage();
    }
    
    /**
     * @return promedio de frame time total en milisegundos
     */
    public double getAverageTotalFrameTime() {
        return totalFrameTime.getAverage();
    }
    
    /**
     * Limpia todas las métricas recolectadas.
     */
    public void reset() {
        tickTime.clear();
        renderTime.clear();
        totalFrameTime.clear();
    }
    
    /**
     * @return información de debug sobre el estado del detector
     */
    public String getDebugInfo() {
        return String.format(
            "BottleneckDetector[samples=%d, tickAvg=%.2fms, renderAvg=%.2fms, type=%s]",
            tickTime.size(), tickTime.getAverage(), renderTime.getAverage(), detect()
        );
    }
}