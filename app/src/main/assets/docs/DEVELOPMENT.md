## 📄 docs/DEVELOPMENT.md

```markdown
# Руководство разработчика

## 🏗️ Структура проекта
app/
├── src/
│ ├── main/
│ │ ├── java/com/example/aichallenge/
│ │ │ ├── agents/
│ │ │ ├── chunking/
│ │ │ ├── embedding/
│ │ │ ├── index/
│ │ │ ├── llm/
│ │ │ ├── benchmark/
│ │ │ ├── ui/
│ │ │ ├── models/
│ │ │ └── utils/
│ │ ├── assets/
│ │ │ ├── secrets.properties
│ │ │ └── *.md
│ │ └── res/
│ └── test/
└── build.gradle.kts

text

## 📦 Основные зависимости

```kotlin
// build.gradle.kts (app)

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // UI
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
🔧 Конфигурация
secrets.properties
properties
api_key=sk-or-v1-xxx  # OpenRouter API key
gradle.properties
properties
# Оптимизация сборки
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=true
kotlin.code.style=official
🧪 Тестирование
Unit тесты (JUnit)
kotlin
// test/java/com/example/aichallenge/CosineSimilarityTest.kt
class CosineSimilarityTest {
    
    @Test
    fun `cosine similarity returns 0 for orthogonal vectors`() {
        val v1 = listOf(1.0f, 0.0f)
        val v2 = listOf(0.0f, 1.0f)
        val result = CosineSimilarity.calculate(v1, v2)
        assertEquals(0.0, result, 0.01)
    }
    
    @Test
    fun `cosine similarity returns 1 for identical vectors`() {
        val v1 = listOf(1.0f, 0.0f)
        val v2 = listOf(1.0f, 0.0f)
        val result = CosineSimilarity.calculate(v1, v2)
        assertEquals(1.0, result, 0.01)
    }
}
Интеграционные тесты
kotlin
// androidTest/java/com/example/aichallenge/IndexTest.kt
@RunWith(AndroidJUnit4::class)
class IndexTest {
    
    @Test
    fun `index building and searching works`() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        val loader = DocumentLoader(context)
        val service = EmbeddingService("test-key")
        
        val chunks = loader.loadDocuments()
            .flatMap { ParagraphChunker().createChunks(it) }
            .map { service.createEmbedding(it) }
        
        val index = Index(chunks)
        val searcher = IndexSearcher(context, service)
        
        val results = searcher.search("test query", topK = 5)
        
        assertTrue(results.isNotEmpty())
    }
}
📊 Профилирование
CPU Profiling
kotlin
class PerformanceTracker {
    private val times = mutableMapOf<String, MutableList<Long>>()
    
    fun measure(operation: String, block: () -> Unit) {
        val start = System.nanoTime()
        block()
        val duration = System.nanoTime() - start
        times.getOrPut(operation) { mutableListOf() }.add(duration)
    }
    
    fun report(): String {
        return times.map { (op, durations) ->
            val avg = durations.average() / 1_000_000 // ms
            "$op: avg=${"%.2f".format(avg)}ms, n=${durations.size}"
        }.joinToString("\n")
    }
}
Memory Profiling
kotlin
class MemoryMonitor {
    private val runtime = Runtime.getRuntime()
    
    fun getMemoryUsage(): String {
        val used = runtime.totalMemory() - runtime.freeMemory()
        val max = runtime.maxMemory()
        return "Memory: ${used / 1024 / 1024}MB / ${max / 1024 / 1024}MB"
    }
}
🔍 Отладка
Логирование
kotlin
object Logger {
    private const val TAG = "AIChallenge"
    
    fun d(message: String) {
        Log.d(TAG, message)
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
    
    fun i(message: String) {
        Log.i(TAG, message)
    }
}
Инструменты отладки
Logcat: Фильтр по тегам

Profiler: CPU, Memory, Network

Layout Inspector: Просмотр UI иерархии

Database Inspector: Просмотр SQLite

API Debug
kotlin
class NetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Logger.d("Request: ${request.method} ${request.url}")
        
        val response = chain.proceed(request)
        Logger.d("Response: ${response.code}")
        
        return response
    }
}
🚀 Производительность
Оптимизация индексации
kotlin
// Использование корутин для параллельной обработки
suspend fun buildIndexOptimized(documents: List<Document>): List<Chunk> {
    return documents
        .flatMap { chunkingStrategy.createChunks(it) }
        .map { chunk ->
            async { embeddingService.createEmbedding(chunk) }
        }
        .awaitAll()
}
Кэширование
kotlin
class CacheManager {
    private val cache = LruCache<String, Any>(100)
    
    fun <T> getOrCompute(key: String, compute: () -> T): T {
        return cache.get(key) as? T ?: compute().also {
            cache.put(key, it)
        }
    }
}
📝 Стиль кода
Kotlin Style Guide
Использовать val вместо var где возможно

Именование: camelCase для переменных и функций

PascalCase для классов

UPPER_SNAKE_CASE для констант

Пример
kotlin
class ExampleClass(
    private val dependency: Dependency
) {
    companion object {
        private const val MAX_RETRIES = 3
    }
    
    fun processData(data: String): Result {
        // implementation
    }
}
🔄 CI/CD
GitHub Actions
yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Run tests
        run: ./gradlew test
Fastlane
ruby
# fastlane/Fastfile
lane :beta do
  gradle(task: "assembleDebug")
  upload_to_play_store(
    track: 'internal',
    release_status: 'draft'
  )
end
🐛 Отладка распространенных проблем
1. Индекс не загружается
Проблема: IndexSearcher возвращает пустой список

Решение:

kotlin
// Проверить существование файла
val file = File(context.filesDir, "index.json")
if (!file.exists()) {
    // Запустить индексацию
    indexInitializer.initialize(forceRebuild = true)
}

// Проверить содержимое
val text = file.readText()
Log.d("DEBUG", "Index content: ${text.take(100)}")
2. Медленные ответы
Проблема: Долгое время генерации

Решение:

Уменьшить topK в поиске

Уменьшить размер контекста

Использовать OpenRouter вместо Ollama

Оптимизировать промпты

3. OOM при индексации
Проблема: OutOfMemoryError при обработке больших документов

Решение:

kotlin
// Обработка по частям
suspend fun buildIndexInBatches(
    documents: List<Document>,
    batchSize: Int = 10
): List<Chunk> {
    val result = mutableListOf<Chunk>()
    documents.chunked(batchSize).forEach { batch ->
        val chunks = batch.flatMap { chunkingStrategy.createChunks(it) }
        val embedded = chunks.map { embeddingService.createEmbedding(it) }
        result.addAll(embedded)
    }
    return result
}
📈 Мониторинг и аналитика
Метрики для сбора
Время индексации: IndexBuilder

Время поиска: IndexSearcher

Качество ответов: Рейтинг пользователя

Использование памяти: MemoryMonitor

Ошибки сети: NetworkInterceptor

📚 Полезные ссылки
Kotlin Documentation

Android Development Guide

Coroutines Guide

OkHttp Documentation

Compose UI Guide

🤝 Вклад
Процесс внесения изменений
Создать issue

Обсудить изменения

Создать branch

Внести изменения

Написать тесты

Создать PR

Код-ревью

Мерж

Правила коммитов
text
feat: Добавление новой функции
fix: Исправление ошибки
docs: Обновление документации
test: Добавление тестов
refactor: Рефакторинг кода
chore: Обновление зависимостей