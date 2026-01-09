#!/bin/bash
echo "🔨 Verificando compilación..."
./gradlew compileJava --no-daemon --console=plain

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ ¡COMPILACIÓN EXITOSA!"
    echo "Puedes hacer push con confianza 🚀"
else
    echo ""
    echo "❌ ERRORES DE COMPILACIÓN"
    echo "Arregla los errores antes de hacer push"
    exit 1
fi
