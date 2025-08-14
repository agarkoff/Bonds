# Используем официальный образ OpenJDK 11
FROM openjdk:11-jre-slim

# Создаем рабочую директорию
WORKDIR /app

# Устанавливаем переменные окружения
ENV SPRING_PROFILES_ACTIVE=docker

# Копируем JAR файл приложения
COPY target/bonds-0.0.1-SNAPSHOT.jar app.jar

# Создаем директории для кэша и документации
RUN mkdir -p cache/raexpert docs

# Копируем статические файлы (если есть)
COPY docs/ docs/
COPY cache/ cache/

# Открываем порт приложения
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]