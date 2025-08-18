#!/bin/bash

# Генерация самоподписанного SSL сертификата для разработки
echo "🔐 Генерация SSL сертификата для разработки..."

# Создаем keystore с самоподписанным сертификатом
keytool -genkeypair \
    -alias tomcat \
    -keyalg RSA \
    -keysize 2048 \
    -keystore src/main/resources/keystore.p12 \
    -storetype PKCS12 \
    -storepass changeit \
    -keypass changeit \
    -validity 365 \
    -dname "CN=localhost, OU=Development, O=Bonds, L=City, ST=State, C=RU" \
    -ext SAN=dns:localhost,ip:127.0.0.1

echo "✅ SSL сертификат создан: src/main/resources/keystore.p12"
echo ""
echo "🚀 Для запуска с HTTPS используйте:"
echo "export SSL_ENABLED=true"
echo "mvn spring-boot:run"
echo ""
echo "🌐 Приложение будет доступно по адресу: https://localhost:8080"
echo "⚠️  Браузер покажет предупреждение о самоподписанном сертификате - это нормально для разработки"