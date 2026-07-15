## 📄 docs/SETUP.md

```markdown
# Руководство по установке и настройке

## 📋 Требования

### Системные требования
- Android Studio Arctic Fox или новее
- JDK 11+
- Android SDK API 24+
- Kotlin 1.9+

### Для локального запуска (Ollama)
- Ollama установленный и запущенный
- Ngrok (для доступа с Android устройства)

## 🔧 Установка

### 1. Клонирование репозитория

```bash
git clone https://github.com/your-username/ai-challenge.git
cd ai-challenge
2. Настройка API ключей
Создайте файл app/src/main/assets/secrets.properties:

bash
mkdir -p app/src/main/assets
cat > app/src/main/assets/secrets.properties << EOF
api_key=your_openrouter_api_key_here
EOF
Получить API ключ OpenRouter:

Перейдите на OpenRouter

Зарегистрируйтесь и получите API ключ

Пополните баланс (минимальный депозит $5)

3. Настройка Ollama (опционально)
Установка Ollama
bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh

# Windows
# Скачайте установщик с ollama.com
Загрузка модели
bash
ollama pull mistral:7b-instruct-v0.3-q4_K_M
Настройка ngrok
bash
# Установка ngrok
brew install ngrok/ngrok/ngrok  # macOS
# или скачайте с ngrok.com

# Авторизация
ngrok config add-authtoken YOUR_NGROK_AUTH_TOKEN

# Запуск туннеля
ngrok http 11434
Обновление конфигурации
В OllamaConfig.kt обновите URL:

kotlin
const val URL = "https://your-ngrok-url.ngrok-free.app/api/chat"
4. Сборка проекта
bash
./gradlew build
5. Запуск на устройстве
bash
./gradlew installDebug
📱 Настройка приложения
Первый запуск
При первом запуске происходит индексация документов

Прогресс отображается в UI

После завершения можно использовать приложение

Настройка чанкинга
В интерфейсе доступны два режима:

Paragraph Chunker:

Рекомендуется для документации

Сохраняет структуру абзацев

Лучше для понимания контекста

Fixed Size Chunker:

Для любых документов

Предсказуемый размер чанков

Настройка размера (по умолчанию 1000)

Выбор провайдера
OpenRouter (Cloud):

✅ Высокая скорость

✅ Качественные ответы

❌ Требует интернет

❌ Платная

Ollama (Local):

✅ Бесплатный

✅ Приватный

✅ Работает оффлайн

❌ Медленнее

❌ Требует настройки

🧪 Проверка работы
Тестовые запросы
Для SimpleAgent:
text
"Что такое Kotlin?"
"Расскажи про data class"
"Какие типы данных есть в Kotlin?"
Для RagAgent:
text
"Что такое индексация в RAG?"
"Как работает поиск по документам?"
"Какие стратегии чанкинга доступны?"
Запуск бенчмарка
Выберите провайдера (OpenRouter/Ollama)

Нажмите "Запустить Benchmark"

Дождитесь выполнения (10 вопросов)

Просмотрите результаты

🐛 Устранение проблем
Проблема: Индекс не создается
Решение:

bash
# Очистить кэш приложения
adb shell pm clear com.example.aichallenge

# Перезапустить приложение
Проблема: Ollama не отвечает
Проверка:

bash
# Проверить запущен ли Ollama
curl http://localhost:11434/api/chat

# Проверить доступность через ngrok
curl https://your-ngrok-url.ngrok-free.app/api/chat

# Перезапустить
ollama stop mistral:7b-instruct-v0.3-q4_K_M
ollama run mistral:7b-instruct-v0.3-q4_K_M
Проблема: OpenRouter ошибка 401
Проверка:

API ключ в secrets.properties

Баланс на аккаунте OpenRouter

Правильный формат ключа (sk-or-v1-...)

Проблема: Медленная индексация
Оптимизация:

Используйте Fixed Size Chunker с меньшим размером

Уменьшите количество документов

Используйте OpenRouter для эмбеддингов

📊 Мониторинг
Логирование
Логи доступны через Logcat:

kotlin
// Теги для фильтрации
TAG_INDEX = "INDEX"        // Индексация
TAG_RAG = "RagAgent"       // RAG запросы
TAG_SEARCH = "IndexSearcher" // Поиск
Профилирование
Включить профилирование в MainActivity:

kotlin
// Для измерения времени выполнения
val time = measureTimeMillis {
    // ваш код
}
Log.d(TAG, "Execution time: $time ms")
🔄 Обновление
Через Git
bash
git pull origin main
./gradlew clean build
./gradlew installDebug
Обновление документов
Добавьте новые .md файлы в app/src/main/assets/

Нажмите "Переиндексировать" в приложении

Дождитесь завершения

📚 Дополнительные ресурсы
Документация Kotlin

Android Developers

OpenRouter API

Ollama API

RAG Paper

💬 Поддержка
Если у вас возникли проблемы:

Проверьте Issues

Создайте новый Issue с описанием проблемы

Включите логи и шаги воспроизведения