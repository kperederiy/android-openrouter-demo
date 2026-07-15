# AI Challenge - Android RAG Assistant

Приложение для Android с интеграцией RAG (Retrieval-Augmented Generation) системы, позволяющей использовать локальные документы для получения контекстных ответов от AI-ассистента.

## 🎯 Основные возможности

- **Два режима работы**: Simple (без RAG) и RAG (с поиском по документам)
- **Поддержка двух LLM провайдеров**:
    - ☁️ OpenRouter (облачный) - GPT-4o-mini
    - 💻 Ollama (локальный) - Mistral 7B
- **Векторный поиск** по документам с использованием эмбеддингов
- **Интерактивный Benchmark** для сравнения производительности
- **Гибкая настройка чанкинга** (по абзацам или фиксированного размера)

## 🏗️ Архитектура проекта
app/src/main/java/com/example/aichallenge/
├── agents/
│ ├── SimpleAgent.kt # Базовый AI-агент
│ └── RagAgent.kt # RAG-агент с поиском по документам
├── chunking/
│ ├── ChunkingStrategy.kt # Интерфейс стратегии чанкинга
│ ├── ParagraphChunker.kt # Разбивка по абзацам
│ └── FixedSizeChunker.kt # Разбивка по фиксированному размеру
├── embedding/
│ ├── EmbeddingService.kt # Сервис для создания эмбеддингов
│ └── CosineSimilarity.kt # Вычисление косинусного сходства
├── index/
│ ├── IndexBuilder.kt # Построение векторного индекса
│ ├── IndexSearcher.kt # Поиск по индексу
│ └── IndexStorage.kt # Хранение индекса
├── llm/
│ ├── OpenRouterClient.kt # Клиент для OpenRouter API
│ └── OllamaClient.kt # Клиент для Ollama API
├── benchmark/
│ ├── BenchmarkRunner.kt # Запуск бенчмарков
│ └── BenchmarkStorage.kt # Хранение результатов
└── ui/
└── MainActivity.kt # Основной UI с Compose

text

## 🚀 Настройка и запуск

### 1. API Ключи

Создайте файл `app/src/main/assets/secrets.properties`:

```properties
api_key=YOUR_OPENROUTER_API_KEY
2. Настройка Ollama (опционально)
Для локального запуска требуется Ollama:

bash
# Установка Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Загрузка модели
ollama pull mistral:7b-instruct-v0.3-q4_K_M

# Запуск сервера (с ngrok для доступа с Android)
ngrok http 11434
Обновите OllamaConfig.kt с вашим ngrok URL.

3. Подготовка документов
Поместите .md файлы в app/src/main/assets/ для индексации.

📱 Использование
Режимы работы
Режим	Описание	Когда использовать
Simple	Прямой запрос к LLM без контекста	Общие вопросы
RAG	Поиск по документам + генерация ответа	Вопросы по документации
Провайдеры
Провайдер	Модель	Особенности
OpenRouter	GPT-4o-mini	Облачный, быстрый, точный
Ollama	Mistral 7B	Локальный, приватный, оффлайн
Команды
/help - Получить справку по проекту

/help [вопрос] - Задать вопрос о проекте

Обычное сообщение - Общение с AI-ассистентом

🔬 Бенчмаркинг
Приложение включает систему бенчмарков для сравнения:

SimpleAgent vs RagAgent

OpenRouter vs Ollama

Различные стратегии чанкинга

Результаты сохраняются в benchmark.json и отображаются в UI.

⚙️ Технические детали
Поток данных RAG
text
1. Вопрос пользователя
   ↓
2. Query Rewriting (опционально)
   ↓
3. Создание эмбеддинга вопроса
   ↓
4. Поиск по векторному индексу
   ↓
5. Фильтрация по порогу сходства (0.3)
   ↓
6. Формирование промпта с контекстом
   ↓
7. Генерация ответа LLM
Настройки чанкинга
Paragraph Chunker: Разбивка по двойным переносам строк

Fixed Size Chunker: Разбивка на чанки фиксированного размера (по умолчанию 500 символов)

Параметры эмбеддингов
Модель: openai/text-embedding-3-small

Размерность: 1536

Провайдер: OpenRouter

🛠️ Зависимости
gradle
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'androidx.compose.ui:ui:1.5.4'
    // ... другие зависимости
}
🧪 Тестирование
Unit тесты
bash
./gradlew test
Интеграционные тесты
bash
./gradlew connectedAndroidTest
📊 Метрики производительности
Компонент	Среднее время	Примечание
Индексация документа	2-5 сек	Зависит от размера
Поиск (10 чанков)	200-500 мс	С эмбеддингом вопроса
Генерация ответа	1-3 сек	OpenRouter быстрее
🤝 Вклад в проект
Форкните репозиторий

Создайте ветку для фичи (git checkout -b feature/AmazingFeature)

Зафиксируйте изменения (git commit -m 'Add some AmazingFeature')

Запушьте ветку (git push origin feature/AmazingFeature)

Откройте Pull Request