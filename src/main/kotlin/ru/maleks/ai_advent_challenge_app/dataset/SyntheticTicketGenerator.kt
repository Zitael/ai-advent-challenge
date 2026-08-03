package ru.maleks.ai_advent_challenge_app.dataset

class SyntheticTicketGenerator {

    fun generate(targetCount: Int): List<FineTuningExample> {
        val templates = billingTemplates + accountTemplates + technicalTemplates + featureTemplates
        val pairs = templates.flatMap { template ->
            template.variants.map { variant -> template.category to variant }
        }

        require(pairs.isNotEmpty()) {
            "Synthetic templates must not be empty"
        }

        return (0 until targetCount).map { index ->
            val (category, message) = pairs[index % pairs.size]
            val userMessage = if (index < pairs.size) {
                message
            } else {
                "$message [case-${index + 1}]"
            }

            RealTicketSource.toExample(
                userMessage = userMessage,
                category = category,
                source = "synthetic",
                real = false
            )
        }
    }

    private data class Template(
        val category: TicketCategory,
        val variants: List<String>
    )

    private companion object {
        val billingTemplates = listOf(
            Template(
                TicketCategory.BILLING,
                listOf(
                    "Почему списали 990 рублей после отмены trial?",
                    "Нужен возврат за дублирующий платёж от 12 июля.",
                    "Не могу скачать счёт-фактуру за прошлый месяц.",
                    "Платёж прошёл, но тариф остался FREE.",
                    "Как изменить реквизиты для корпоративного счёта?",
                    "Автопродление сняло оплату, хотя подписку отключил вчера.",
                    "Нужен акт сверки за Q2 для бухгалтерии.",
                    "Промокод не применился при оплате годового тарифа.",
                    "В кабинете висит неоплаченный инвойс, хотя карта списана.",
                    "Как перейти с PRO на TEAM без двойного списания?"
                )
            )
        )

        val accountTemplates = listOf(
            Template(
                TicketCategory.ACCOUNT,
                listOf(
                    "Не работает вход через SSO после смены домена.",
                    "Как отключить двухфакторную аутентификацию?",
                    "Профиль показывает старый номер телефона.",
                    "Аккаунт заблокирован без объяснения причин.",
                    "Не могу пригласить коллегу в workspace.",
                    "После merge аккаунтов пропали права администратора.",
                    "Сессия сбрасывается каждые 10 минут на новом ноутбуке.",
                    "Не приходит SMS с кодом подтверждения на новый номер.",
                    "Как передать ownership workspace другому пользователю?",
                    "OAuth login через Google возвращает invalid_state."
                )
            )
        )

        val technicalTemplates = listOf(
            Template(
                TicketCategory.TECHNICAL,
                listOf(
                    "Интеграция с Ollama возвращает timeout через 30 секунд.",
                    "После деплоя API отвечает 502 на health check.",
                    "Экспорт отчёта падает с OutOfMemory на больших данных.",
                    "MCP сервер не стартует на порту 3000.",
                    "RAG индекс не обновляется после изменения docs.",
                    "Webhook delivery падает с SSL handshake error.",
                    "Batch job зависает на шаге embedding generation.",
                    "GraphQL endpoint отдаёт partial response без ошибок.",
                    "Логи показывают circuit breaker open для CRM connector.",
                    "Mobile SDK crash при открытии push notification deep link."
                )
            )
        )

        val featureTemplates = listOf(
            Template(
                TicketCategory.FEATURE_REQUEST,
                listOf(
                    "Нужен bulk import тикетов из Zendesk.",
                    "Добавьте dark mode в support dashboard.",
                    "Хотим SLA-алерты в Telegram.",
                    "Нужна роль auditor только для чтения логов.",
                    "Просим API endpoint для статистики по категориям.",
                    "Нужен экспорт audit trail в SIEM формате.",
                    "Хотим кастомные поля в карточке клиента CRM.",
                    "Добавьте шаблоны ответов для L1 support.",
                    "Нужна интеграция с Jira для эскалации багов.",
                    "Просим scheduled reports в Slack каждое утро."
                )
            )
        )
    }
}
