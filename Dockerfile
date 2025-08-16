# Используем официальный образ OpenJDK 11
FROM openjdk:11-jre-slim

# Устанавливаем Chromium браузер и сетевые утилиты
RUN apt-get update && \
    apt-get install -y chromium iputils-ping net-tools curl wget telnet dnsutils && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Создаем рабочую директорию
WORKDIR /app

# Устанавливаем переменные окружения
ENV SPRING_PROFILES_ACTIVE=docker

# Копируем JAR файл приложения
COPY target/bonds-0.0.1-SNAPSHOT.jar app.jar

# Копируем chromedriver
COPY drivers/linux/chromedriver drivers/linux/chromedriver

# Создаем директории для кэша и документации
RUN mkdir -p cache/raexpert docs

# Открываем порт приложения
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]