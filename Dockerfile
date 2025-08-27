# Этап 1: Сборка приложения
FROM maven:3.8.6-openjdk-11 AS build

# Создаем рабочую директорию для сборки
WORKDIR /build

# Копируем файлы Maven для кэширования зависимостей
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN mvn clean package -DskipTests

# Этап 2: Рабочий образ
FROM openjdk:11-jre-slim

# Устанавливаем Chromium браузер, ChromeDriver, сетевые утилиты и настраиваем часовой пояс
RUN apt-get update && \
    apt-get install -y chromium chromium-driver iputils-ping net-tools curl wget telnet dnsutils tzdata && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Создаем рабочую директорию
WORKDIR /app

# Создаем символическую ссылку для совместимости с путями в коде
RUN mkdir -p drivers/linux && \
    ln -s /usr/bin/chromedriver drivers/linux/chromedriver

# Устанавливаем переменные окружения
ENV TZ=Europe/Moscow
ENV DISPLAY=:99
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROME_DRIVER=/usr/bin/chromedriver

# Копируем JAR файл из этапа сборки
COPY --from=build /build/target/bonds-0.0.1-SNAPSHOT.jar app.jar

# ChromeDriver уже установлен из пакетного менеджера

# Создаем директории для кэша и документации
RUN mkdir -p cache/raexpert docs

# Открываем порт приложения
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]