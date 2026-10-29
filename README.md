# PvPKit

Лёгкий плагин для **Paper 1.21.11** с тремя модулями для KitPvP-сервера:

- **PvP HUD** — CPS, прочность брони и keystrokes (WASD, SPACE, SHIFT) в action bar.
- **KitPvP** — киты с кулдаунами, настраиваемые через `config.yml`.
- **Reports** — жалобы на игроков с сохранением, уведомлениями стаффу, телепортом и закрытием.

Проект собран и проверен на Paper `1.21.11-132`, Java 21.

## Возможности

### HUD
- Счётчик CPS отдельно для ЛКМ и ПКМ (окно 1 секунда).
- Процент прочности каждого элемента брони: шлем / нагрудник / поножи / ботинки.
- Keystrokes: `W`, `A`, `S`, `D`, `SPACE`, `SHIFT` — активные клавиши подсвечиваются.
- Обновление 10 раз в секунду (настраивается), отправка только при изменении строки.
- Состояние HUD сохраняется в PDC игрока, переключается через `/hud`.

### KitPvP
- Киты полностью описываются в `config.yml`.
- Поддержка предметов, количества, зачарований, неразрушимости, брони, offhand и эффектов.
- Кулдауны сохраняются в `cooldowns.yml` и переживают перезапуск сервера.
- Админ-сброс кулдаунов и reload без рестарта.

### Reports
- `/report <player> <reason>` — жалоба на онлайн- или ранее заходившего игрока.
- Жалобы сохраняются в `reports.yml` (ID, репортёр, цель, причина, время, статус, локация).
- Уведомление стаффу с кликабельными кнопками `[TP]` и `[Close]`.
- Список открытых жалоб, информация, телепорт к цели или на сохранённую локацию, закрытие.
- Кулдаун на отправку жалоб и лимит длины причины.

## Команды

| Команда | Описание |
|---|---|
| `/hud [on\|off]` | Включить, выключить или переключить HUD |
| `/kit` / `/kit list` | Список китов и статус кулдауна |
| `/kit <kit>` | Получить кит |
| `/kit reset <player> [kit]` | Сбросить кулдаун игрока |
| `/kit reload` | Перезагрузить конфиг и киты |
| `/report <player> <reason>` | Отправить жалобу |
| `/reports` | Список открытых жалоб |
| `/reports tp <id>` | Телепорт к цели жалобы |
| `/reports close <id>` | Закрыть жалобу |
| `/reports info <id>` | Подробности жалобы |
| `/pvpkit reload` | Полная перезагрузка плагина |

## Права

| Право | По умолчанию | Описание |
|---|---|---|
| `pvpplugin.hud` | все | Использование HUD |
| `pvpplugin.kit` | все | Получение китов |
| `pvpplugin.kit.bypass` | оператор | Игнорировать кулдаун китов |
| `pvpplugin.kit.admin` | оператор | Сброс кулдаунов, reload |
| `pvpplugin.report.use` | все | Отправка жалоб |
| `pvpplugin.report.notify` | оператор | Уведомления о новых жалобах |
| `pvpplugin.report.staff` | оператор | Просмотр и обработка жалоб |
| `pvpplugin.report.bypass` | оператор | Игнорировать кулдаун жалоб |
| `pvpplugin.admin` | оператор | Полный доступ к настройкам |

## Сборка

Нужен **JDK 21**.

```bash
./gradlew build
```

Готовый jar: `build/libs/PvPKit-1.0.0.jar`.

## Установка

1. Положите `PvPKit-1.0.0.jar` в папку `plugins/` сервера Paper 1.21.11.
2. Запустите сервер — создастся `plugins/PvPKit/config.yml`.
3. Настройте киты и параметры HUD/жалоб.
4. Примените изменения командой `/pvpkit reload`.

## Конфигурация китов

```yaml
kits:
  warrior:
    display-name: "&cWarrior"
    icon: NETHERITE_SWORD
    cooldown-seconds: 60
    armor:
      helmet: "DIAMOND_HELMET PROTECTION:2"
      chestplate: "DIAMOND_CHESTPLATE PROTECTION:2"
      leggings: "DIAMOND_LEGGINGS PROTECTION:2"
      boots: "DIAMOND_BOOTS PROTECTION:2"
    offhand: "SHIELD"
    items:
      - "NETHERITE_SWORD SHARPNESS:3"
      - "GOLDEN_APPLE:8"
    effects:
      - "SPEED:1:300" # <effect>:<amplifier>:<duration-seconds>
```

Формат предмета: `<MATERIAL>:<amount> <ENCHANTMENT>:<level> ...`

## Структура

```
src/main/java/com/ludvig/pvpkit/
├── PvPKitPlugin.java
├── PvPKitCommand.java
├── hud/       # HUD, keystrokes, CPS, listener, /hud
├── kit/       # модель кита, загрузка конфига, кулдауны, /kit
├── report/    # модель жалобы, хранение, /report, /reports
└── util/      # форматирование текста и времени
```

## Лицензия

MIT
