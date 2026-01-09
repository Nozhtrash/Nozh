# NOZH API para modders

Esta guía describe cómo integrarse con NOZH desde un mod externo usando los contratos
de capabilities, declaraciones de stewardship y métricas internas.

## Objetivos de la API

- Declarar contratos de capabilities (tipo, rango, valores permitidos).
- Informar acuerdos de stewardship (quién administra cada capability).
- Exponer métricas internas para observabilidad.
- Registrar acuerdos en `CompatibilityMatrix` para reportes y HUD.

## Contratos de capabilities

Los contratos viven en `dev.nozh.api.capability` y se registran mediante `NozhApi`.

```java
import dev.nozh.api.NozhApi;
import dev.nozh.api.capability.CapabilityContract;
import dev.nozh.api.capability.CapabilityContractDeclaration;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

CapabilityContract contract = CapabilityContract.builder(
        CapabilityId.RENDER_DISTANCE,
        CapabilityContract.ValueType.INT)
    .description("Distancia de renderizado controlada por el mod")
    .range(4, 32)
    .unit("chunks")
    .defaultValue(new CapabilityValue.IntValue(12))
    .build();

CapabilityContractDeclaration declaration = CapabilityContractDeclaration
    .builder("mi-mod", "Mi Mod")
    .contract(contract)
    .notes("Contrato validado en 1.20.4")
    .build();

NozhApi.registerCapabilityContracts(declaration);
```

## Stewardship (acuerdos de control)

Usa `StewardshipDeclaration` para indicar si una capability es exclusiva o compartida.

```java
import dev.nozh.api.NozhApi;
import dev.nozh.api.compat.StewardshipDeclaration;
import dev.nozh.core.bus.CapabilityId;

StewardshipDeclaration stewardship = StewardshipDeclaration
    .builder("mi-mod", "Mi Mod")
    .exclusive(CapabilityId.DYNAMIC_LIGHTING)
    .shared(CapabilityId.CLOUDS)
    .reason("El mod maneja estas opciones en runtime")
    .build();

NozhApi.registerStewardship(stewardship);
```

## Métricas internas

Las métricas internas ayudan a reportar señales específicas del mod.

```java
import dev.nozh.api.NozhApi;
import dev.nozh.api.metrics.ModMetricsDeclaration;
import dev.nozh.core.bus.CapabilityId;

ModMetricsDeclaration metrics = ModMetricsDeclaration
    .builder("mi-mod", "Mi Mod")
    .capabilities(CapabilityId.DYNAMIC_LIGHTING)
    .metric("dynamic_lighting.apply.count",
        ModMetricsDeclaration.MetricType.COUNTER,
        "Cantidad de aplicaciones exitosas",
        "calls")
    .metric("dynamic_lighting.apply.latency",
        ModMetricsDeclaration.MetricType.TIMER,
        "Latencia promedio de aplicación",
        "ms")
    .notes("Métricas internas expuestas por el adaptador")
    .build();

NozhApi.registerModMetrics(metrics);
```

## Registrar acuerdos en CompatibilityMatrix

`CompatibilityMatrix` ofrece acuerdos agregados para UI, reportes y diagnóstico:

```java
import dev.nozh.core.compatibility.CompatibilityMatrix;

CompatibilityMatrix matrix = new CompatibilityMatrix();
matrix.getAgreements().forEach(agreement -> {
    // agreement.modId(), agreement.contracts(), agreement.metrics(), etc.
});
```

## Recomendaciones

- Registra contratos y métricas durante la inicialización del mod.
- Evita declarar una capability como exclusiva si vas a permitir control compartido.
- Mantén las descripciones claras para que el HUD muestre información útil.
