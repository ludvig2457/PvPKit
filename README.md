# PvPKit

Плагин для **Paper 1.21.11** с полноценным KitPvP-ядром: HUD, киты, выбор боевой системы, статистика, сайдбар, настройки и репорт-система.

Собран и проверен на Paper `1.21.11-132`, Java 21.

## Возможности

### HUD
- CPS отдельно для ЛКМ и ПКМ (окно 1 секунда).
- Прочность каждого элемента брони в процентах.
- Keystrokes: `W`, `A`, `S`, `D`, `SPACE`, `SHIFT` с подсветкой активных клавиш.
- Ping, серия убийств, комбо и статус боя.
- Гибкое включение каждого блока через `config.yml`.
- Обновление 10 раз в секунду, отправка только при изменении строки.
- Состояние HUD хранится в PDC игрока.

### Боевые системы
- `1.8.9` — без attack cooldown, без sweep-атак, щиты отключены.
- `1.12` — attack cooldown включён, sweep отключён, щиты работают.
- `1.21.11` — ванильная современная боевая система.
- Переключение на лету: `/combat <mode>`.
- Combat tag после удара по игроку блокирует кит и `/spawn`.
- Счётчик комбо и таймер боя.

### KitPvP
- 16 готовых китов: Warrior, Archer, Tank, Assassin, Berserker, Paladin, Pyromancer, Frost, Ninja, Druid, Sniper, Sumo, Crystal, Trident, Healer, Miner.
- GUI-меню китов с иконками, кулдаунами и предпросмотром.
- Киты полностью настраиваются в `config.yml`: предметы, количество, зачарования, броня, offhand, эффекты.
- Кулдауны сохраняются в `cooldowns.yml` и переживают рестарт.
- Запрет на взятие кита в бою.
- Админ-выдача, сброс кулдаунов и reload.

### Статистика
- Kills, deaths, KDR, текущая и лучшая серия.
- Kill feed и объявления серий (5, 10, 15, ...).
- Топ игроков по убийствам с постраничным выводом.
- Статистика сохраняется в `stats.yml`.
- Сброс статистики администратором.

### Сайдбар
- Отдельный scoreboard для каждого игрока.
- Кит, kills/deaths, KDR, серия, combat tag, комбо, онлайн и ping.
- Включение и отключение через `/scoreboard`.
- Период обновления настраивается.

### Reports
- `/report <player> <reason>` — жалоба на онлайн- или ранее заходившего игрока.
- Жалобы сохраняются в `reports.yml`.
- Уведомление стаффу с кликабельными `[TP]` и `[Close]`.
- GUI-меню жалоб: ЛКМ — телепорт, ПКМ — детали, Shift+ЛКМ — закрыть.
- Список, детали, телепорт к цели или на сохранённую локацию, закрытие.
- Кулдаун на отправку и лимит длины причины.

### Прочее
- `/settings` — GUI с переключателями HUD и сайдбара, быстрый доступ к китам, статам и репортам.
- `/spawn` с кулдауном и запретом во время боя.
- Все данные сохраняются в YAML-файлах плагина.
- Русскоязычная документация и MIT-лицензия.

## Команды

| Команда | Описание |
|---|---|
| `/hud [on\|off]` | Переключить HUD |
| `/kit` / `/kits` | Открыть GUI китов |
| `/kit list` | Список китов в чате |
| `/kit <kit>` | Получить кит |
| `/kit preview <kit>` | Предпросмотр кита |
| `/kit give <player> <kit>` | Выдать кит (админ) |
| `/kit reset <player> [kit]` | Сбросить кулдаун (админ) |
| `/kit reload` | Перезагрузить конфиг и киты (админ) |
| `/combat [mode\|info]` | Показать или сменить боевую систему |
| `/stats [player]` | Статистика игрока |
| `/stats top [page]` | Топ по убийствам |
| `/stats reset <player>` | Сбросить статистику (админ) |
| `/scoreboard [on\|off]` | Переключить сайдбар |
| `/settings` | Открыть меню настроек |
| `/spawn` | Телепорт на спавн |
| `/report <player> <reason>` | Отправить жалобу |
| `/reports` | Список открытых жалоб |
| `/reports gui` | GUI жалоб |
| `/reports tp <id>` | Телепорт к цели жалобы |
| `/reports close <id>` | Закрыть жалобу |
| `/reports info <id>` | Детали жалобы |
| `/pvpkit` | Информация о плагине |
| `/pvpkit reload` | Полная перезагрузка |

## Права

| Право | По умолчанию | Описание |
|---|---|---|
| `pvpplugin.hud` | все | HUD |
| `pvpplugin.kit` | все | Киты |
| `pvpplugin.kit.bypass` | оператор | Обход кулдауна и combat-проверки |
| `pvpplugin.kit.admin` | оператор | Выдача, сброс, reload |
| `pvpplugin.combat` | все | Просмотр боевой системы |
| `pvpplugin.combat.admin` | оператор | Смена боевой системы |
| `pvpplugin.stats` | все | Просмотр статистики |
| `pvpplugin.stats.admin` | оператор | Сброс статистики |
| `pvpplugin.scoreboard` | все | Сайдбар |
| `pvpplugin.settings` | все | Меню настроек |
| `pvpplugin.spawn` | все | `/spawn` |
| `pvpplugin.spawn.bypass` | оператор | Обход кулдауна `/spawn` |
| `pvpplugin.report.use` | все | Отправка жалоб |
| `pvpplugin.report.notify` | оператор | Уведомления о жалобах |
| `pvpplugin.report.staff` | оператор | Просмотр и обработка жалоб |
| `pvpplugin.report.bypass` | оператор | Обход кулдауна жалоб |
| `pvpplugin.admin` | оператор | Полный доступ |

## Сборка

Нужен **JDK 21**.

```bash
./gradlew build
```

Готовый jar: `build/libs/PvPKit-1.1.0.jar`.

## Установка

1. Положите jar в `plugins/` сервера Paper 1.21.11.
2. Запустите сервер — создастся `plugins/PvPKit/config.yml`.
3. Настройте киты, HUD и боевую систему.
4. Примените изменения командой `/pvpkit reload`.

## Боевые системы

```yaml
settings:
  combat:
    mode: "1.21.11" # 1.8.9 | 1.12 | 1.21.11
    tag-seconds: 15
    combo-seconds: 3
```

- **1.8.9**: attack strength удерживается на максимуме, кулдаун удара отключён, sweep и щиты отключены, блокировка мечом (50% урона и 20% скорости), кнокбек 1.8 со спринт-бонусом и сбросом спринта после удара.

Параметры 1.8.9 можно тонко настроить:

```yaml
settings:
  combat:
    legacy:
      blocking-damage-multiplier: 0.5
      blocking-damage-flat: 0.5
      blocking-speed-multiplier: 0.2
      knockback-horizontal: 0.4
      knockback-sprint-horizontal: 0.9
      knockback-vertical: 0.4
```

Клиентский индикатор атаки остаётся элементом интерфейса клиента и не влияет на серверный урон: сервер принимает каждый удар с полной силой.
- **1.12**: ванильный attack cooldown, sweep отключён, щиты включены.
- **1.21.11**: полная ванильная механика Paper 1.21.11.

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

## Файлы данных

```
plugins/PvPKit/
├── config.yml
├── cooldowns.yml
├── reports.yml
└── stats.yml
```

## Структура проекта

```
src/main/java/com/ludvig/pvpkit/
├── PvPKitPlugin.java
├── PvPKitCommand.java
├── combat/     # режимы 1.8.9 / 1.12 / 1.21.11, combat tag, combo
├── hud/        # action bar HUD, CPS, keystrokes
├── kit/        # киты, кулдауны, GUI и предпросмотр
├── report/     # жалобы, хранение, чат и GUI
├── settings/   # меню настроек
├── sidebar/    # сайдбар на scoreboard
├── spawn/      # /spawn
├── stats/      # kills, deaths, KDR, серии, топ
└── util/       # форматирование
```

## Лицензия

MIT
