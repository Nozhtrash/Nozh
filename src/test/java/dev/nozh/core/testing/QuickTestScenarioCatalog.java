package dev.nozh.core.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuickTestScenarioCatalog {
    private QuickTestScenarioCatalog() {
    }

    public static Map<String, ScenarioDefinition> defaultScenarios() {
        Map<String, ScenarioDefinition> scenarios = new LinkedHashMap<>();

        scenarios.put("mobs", new ScenarioDefinition(
            "mobs",
            "Mobs (combate / entidades)",
            "Zona con alta densidad de entidades para evaluar spikes en combate e inventario.",
            180,
            List.of(
                ScenarioStep.teleport("/tp <x> <y> <z>", "Teleport a spawner/granja con alta densidad de entidades."),
                ScenarioStep.action("Combate constante con patrón fijo (arma + ruta).", 180),
                ScenarioStep.action("Abrir inventario durante combate para detectar spikes.", 30)
            )
        ));

        scenarios.put("mining", new ScenarioDefinition(
            "mining",
            "Minería (subterráneo / partículas)",
            "Cueva con agua, lava y partículas activas para validar chunk gen y estabilidad.",
            180,
            List.of(
                ScenarioStep.teleport("/tp <x> <y> <z>", "Teleport a cueva con agua/lava/partículas."),
                ScenarioStep.action("Minar y moverse por la cueva manteniendo partículas activas.", 180),
                ScenarioStep.action("Forzar generación de nuevos chunks al avanzar.", 60)
            )
        ));

        scenarios.put("nether", new ScenarioDefinition(
            "nether",
            "Nether (carga y efectos)",
            "Recorrido por biomas con efectos de partículas y teleports en Nether.",
            180,
            List.of(
                ScenarioStep.teleport("/tp <x> <y> <z>", "Entrar al Nether y posicionarse en el bioma objetivo."),
                ScenarioStep.action("Recorrer biomas con efectos activos para observar spikes.", 180),
                ScenarioStep.action("Realizar teleport corto entre biomas para validar cargas.", 30)
            )
        ));

        scenarios.put("rain", new ScenarioDefinition(
            "rain",
            "Lluvia (clima / shaders)",
            "Validar impacto de lluvia o shaders sobre el frametime sostenido.",
            180,
            List.of(
                ScenarioStep.action("Forzar lluvia si el modpack lo permite o esperar clima natural.", 60),
                ScenarioStep.action("Mantener posición fija con lluvia activa y observar p95.", 180),
                ScenarioStep.note("Revisar si se requiere rollback o sugerencias de config.")
            )
        ));

        return scenarios;
    }
}
