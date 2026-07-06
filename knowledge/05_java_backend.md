# Backend Engineering Notes

## Spring Boot

Spring Boot используется для построения backend-сервисов.

---

## Kafka

Kafka применяется для обмена событиями между сервисами.

Dead Letter Topic используется для сообщений, которые невозможно обработать.

---

## Redis

Redis применяется для кэширования.

Redisson предоставляет распределённые блокировки.

---

## PostgreSQL

Используется как основная реляционная база данных.

---

## Архитектура

Рекомендуется разделять:

Controller

Service

Repository

Также полезно выделять отдельные сервисы:

MemoryService

PromptBuilder

Retriever

EmbeddingService

ToolRegistry