# Private AI smoke scenarios

## SMOKE-01 — application and health
Open `/`; verify heading `Private AI`; wait until status changes from `Проверка...`; capture status.

## SMOKE-02 — save settings
Enter API key and session `smoke-settings`; click `Сохранить`; reload; verify session remains.

## SMOKE-03 — required API key
Leave API key empty; enter a message; click `Отправить`; verify `Сначала укажи API key.` appears.

## SMOKE-04 — unauthorized request
Enter an invalid API key and session; send `hello`; verify a visible `Ошибка:` message appears.

## SMOKE-05 — clear validation
Clear API key and session; click `Очистить сессию`; verify required-settings message appears.
