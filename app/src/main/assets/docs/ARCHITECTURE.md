## 📄 docs/ARCHITECTURE.md

```markdown
# Архитектура RAG-системы

## Обзор

Документ описывает архитектуру RAG (Retrieval-Augmented Generation) системы в приложении AI Challenge.

## Диаграмма компонентов
┌─────────────────────────────────────────────────────────────┐
│ MainActivity (UI) │
├─────────────────────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────┐ ┌─────────────────┐ │
│ │ SimpleAgent │ │ RagAgent │ │ BenchmarkRunner │ │
│ └──────┬───────┘ └──────┬───────┘ └─────────────────┘ │
│ │ │ │
│ ┌──────▼─────────────────▼───────┐ │
│ │ LLM Providers │ │
│ │ ┌───────────┐ ┌───────────┐ │ │
│ │ │OpenRouter │ │ Ollama │ │ │
│ │ └───────────┘ └───────────┘ │ │
│ └───────────────────────────────┘ │
│ │
│ ┌──────────────────────────────────────────────────┐ │
│ │ RAG Pipeline │ │
│ │ ┌──────────┐ ┌─────────┐ ┌──────────────┐ │ │
│ │ │ Document │→│Chunking │→│ Embedding │ │ │
│ │ │ Loader │ │Strategy │ │ Service │ │ │
│ │ └──────────┘ └─────────┘ └──────┬───────┘ │ │
│ │ ↓ │ │
│ │ ┌──────────┐ ┌─────────┐ ┌──────────────┐ │ │
│ │ │ Search │←│ Index │←│ Storage │ │ │
│ │ │ Results │ │Searcher│ │ │ │ │
│ │ └──────────┘ └─────────┘ └──────────────┘ │ │
│ └──────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

text

## Компоненты

### 1. UI Layer (MainActivity)

**Ответственность:**
- Отображение интерфейса пользователя
- Обработка пользовательского ввода
- Управление состоянием приложения
- Настройка параметров (провайдер, режим, чанкинг)

**Ключевые функции:**
```kotlin
// Отправка запроса с выбранным провайдером
fun sendRequest(provider: LlmProvider)

// Запуск бенчмарка
fun runBenchmark()

// Переиндексация документов
fun rebuildIndex()
2. Agents
SimpleAgent
Ответственность:

Прямое взаимодействие с LLM

Управление историей диалога

Переключение между провайдерами

Поток данных:

text
User Request → SimpleAgent → OpenRouter/Ollama → Response
RagAgent
Ответственность:

RAG-пайплайн

Переписывание запросов

Поиск по индексу

Формирование контекстных промптов

Поток данных:

text
User Request → Query Rewriting → Embedding → Search → 
Prompt Building → LLM → Response
3. Indexing System
DocumentLoader
Загрузка документов из assets и файловой системы.

kotlin
fun loadDocuments(): List<Document>
Chunking Strategies
ParagraphChunker:

Разбивка по абзацам

Сохранение структуры заголовков

Идеально для документов с четкой структурой

FixedSizeChunker:

Фиксированный размер чанка (по умолчанию 500 символов)

Гарантированное количество чанков

Подходит для любых документов

EmbeddingService
Создание векторных представлений с использованием OpenRouter API.

kotlin
suspend fun createEmbedding(text: String): List<Float>
IndexBuilder
Построение индекса с эмбеддингами для всех чанков.

kotlin
suspend fun buildIndex(): List<Chunk>
4. Search System
IndexSearcher
Векторный поиск по индексу с использованием косинусного сходства.

kotlin
suspend fun search(question: String, topK: Int): List<SearchResult>
SimilarityFilter
Фильтрация результатов по порогу сходства.

Параметр	Значение	Описание
threshold	0.3	Минимальное сходство для включения
topKAfter	5	Максимум результатов после фильтрации
5. LLM Providers
OpenRouterClient
Особенности:

Облачный API

Модель: GPT-4o-mini

Требуется API ключ

Высокая скорость

OllamaClient
Особенности:

Локальное выполнение

Модель: Mistral 7B

Не требует интернета

Приватность данных

6. Benchmark System
Компоненты:

BenchmarkRunner: Выполнение тестов

BenchmarkStorage: Сохранение результатов

BenchmarkResult: Структура данных

Метрики:

Время выполнения (Simple vs RAG)

Качество ответов

Сравнение провайдеров

Потоки данных
Индексация документов
text
1. DocumentLoader → List<Document>
2. ChunkingStrategy → List<Chunk>
3. EmbeddingService → Chunk с эмбеддингами
4. IndexStorage → index.json
RAG-запрос
text
1. User Query → RagAgent
2. QueryRewriter → Оптимизированный запрос
3. IndexSearcher → List<SearchResult>
4. SimilarityFilter → Фильтрованные результаты
5. PromptBuilder → Контекстный промпт
6. SimpleAgent → Ответ LLM
Управление состоянием
ChatMemory (RagAgent)
kotlin
class ChatMemory(
    private val maxMessages: Int = 10
) {
    fun addUserMessage(text: String)
    fun addAssistantMessage(text: String)
    fun getMessages(): List<ChatMessage>
}
ConversationHistory (SimpleAgent)
kotlin
class ConversationHistory(
    private val maxMessages: Int = 20
) {
    fun addUser(text: String)
    fun addAssistant(text: String)
    fun getMessages(): List<ChatMessage>
}
Конфигурация
Локальные настройки
properties
# secrets.properties
api_key=YOUR_OPENROUTER_API_KEY
Параметры модели
Провайдер	Параметр	Значение
OpenRouter	model	openai/gpt-4o-mini
Ollama	model	mistral:7b-instruct-v0.3-q4_K_M
Ollama	temperature	0.5
Ollama	max_tokens	512
Ollama	num_ctx	4096
Оптимизация производительности
Кэширование
Индекс сохраняется в index.json

Повторное использование при перезапуске

Асинхронность
Все IO операции в корутинах

UI не блокируется

Пакетная обработка
Чанки обрабатываются последовательно

Возможность прерывания

Безопасность
Хранение ключей
API ключ в secrets.properties

Не включен в систему контроля версий

Локальное выполнение
Ollama работает полностью локально

Данные не покидают устройство

Тестирование
Компоненты для тестирования
Unit тесты: Алгоритмы (CosineSimilarity, чанкинг)

Интеграционные тесты: Индексация, поиск

UI тесты: Navigation, состояния

Пример теста
kotlin
@Test
fun testCosineSimilarity() {
    val v1 = listOf(1.0f, 0.0f)
    val v2 = listOf(0.0f, 1.0f)
    val result = CosineSimilarity.calculate(v1, v2)
    assertEquals(0.0, result, 0.01)
}
Расширение системы
Добавление нового провайдера
Создать класс клиента

Добавить в SimpleAgent

Обновить UI для выбора

Добавление стратегии чанкинга
Реализовать ChunkingStrategy

Добавить в ChunkingType

Обновить UI

Новые метрики для Benchmark
Добавить поля в BenchmarkResult

Обновить BenchmarkRunner

Отобразить в UI