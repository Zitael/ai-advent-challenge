# Model Context Protocol

## Что такое MCP

Model Context Protocol позволяет публиковать инструменты, доступные агенту.

Инструмент содержит:

- название;
- описание;
- параметры;
- результат.

---

## MCP не является REST

REST описывает HTTP API.

MCP описывает возможности для AI.

Один MCP Tool может внутри:

- вызвать REST API;
- выполнить SQL;
- обратиться к Git;
- выполнить локальную функцию;
- прочитать файл.

---

## Примеры инструментов

search_tasks

create_ticket

git_diff

save_file

summarize

send_email