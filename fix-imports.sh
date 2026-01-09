#!/bin/bash
echo "🔧 Corrigiendo imports de CapabilityValue..."

find src -name "*.java" -type f -exec sed -i 's/dev\.nozh\.core\.bus\.CapabilityValue/dev.nozh.core.capability.CapabilityValue/g' {} +

echo "✅ Imports corregidos"
echo "🔨 Verificando compilación..."

./gradlew compileJava --no-daemon

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ ¡TODO ARREGLADO! Ahora puedes hacer commit"
else
    echo ""
    echo "⚠️ Aún hay errores, revisa el log arriba"
fi
