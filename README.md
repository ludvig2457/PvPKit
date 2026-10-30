# PvPKit Suite

Набор независимых PvP-плагинов для Paper/Folia. Каждый модуль ставится отдельно и не требует остальных.

| Плагин | Репозиторий | Отвечает за |
|---|---|---|
| **PvPKit** | [ludvig2457/PvPKit](https://github.com/ludvig2457/PvPKit) | HUD, статистика, сайдбар, `/settings` |
| **Combat++** | [ludvig2457/CombatPlusPlus](https://github.com/ludvig2457/CombatPlusPlus) | Режимы 1.8.9 / 1.12 / 1.21.11, combat tag, комбо |
| **Spawn++** | [ludvig2457/SpawnPlusPlus](https://github.com/ludvig2457/SpawnPlusPlus) | `/spawn`, кулдаун, запрет в бою, `/spawn set` |
| **KitPvP** | [ludvig2457/KitPvP](https://github.com/ludvig2457/KitPvP) | 16 китов, GUI, кулдауны |
| **Report++** | [ludvig2457/ReportPlusPlus](https://github.com/ludvig2457/ReportPlusPlus) | Жалобы, GUI, уведомления стаффу |

Модули общаются через metadata, а не через классы:

- `pvpplugin_combat_until` — конец combat tag (Combat++).
- `combatplus_combo` / `combatplus_combo_until` — комбо (Combat++).
- `combatplus_mode` — текущий режим (Combat++).
- `kitpvp_last_kit` — последний выданный кит (KitPvP).

## Поддерживаемые версии

Один исходник собирается под каждую версию Paper API:

`1.16.5`, `1.17.1`, `1.18.2`, `1.19.4`, `1.20.6`, `1.21.1`, `1.21.4`, `1.21.8`, `1.21.11`

Платформы: **Paper** и **Folia** (scheduler-совместимость через `SchedulerCompat`).

> **Spigot:** текущие сборки используют Paper API (Adventure, Paper scheduler), поэтому на чистом Spigot не загрузятся. Для Spigot нужна отдельная legacy-сборка.
>
> **1.8.9:** это режим боя `/combat 1.8.9`, а не поддержка сервера 1.8.9. Сервер должен быть Paper/Folia 1.16.5+; клиенты 1.8.9 могут подключаться через ViaVersion.

## Сборка

```bash
# версия по умолчанию — 1.21.11
./gradlew build

# конкретная версия Paper API
./gradlew build -PmcVersion=1.20.6-R0.1-SNAPSHOT
```

Jar: `build/libs/PvPKit-1.3.0.jar`.

## PvPKit (этот репозиторий)

### Возможности
- HUD: CPS, комбо, броня, keystrokes, серия, combat tag, ping.
- Статистика: kills, deaths, KDR, серии, топ, kill feed.
- Сайдбар: кит, статы, combat, комбо, онлайн, пинг.
- `/settings`: HUD, сайдбар, переходы в Combat++, Spawn++, KitPvP и Report++.
- Работает без остальных модулей; отсутствующие модули просто скрываются.

### Команды
| Команда | Описание |
|---|---|
| `/hud [on\|off]` | Переключить HUD |
| `/stats [player]` | Статистика игрока |
| `/stats top [page]` | Топ игроков |
| `/stats reset <player>` | Сбросить статистику |
| `/scoreboard [on\|off]` | Переключить сайдбар |
| `/settings` | Меню настроек |
| `/pvpkit [reload]` | Информация и reload |

### Права
`pvpplugin.hud`, `pvpplugin.stats`, `pvpplugin.stats.admin`, `pvpplugin.scoreboard`, `pvpplugin.settings`, `pvpplugin.admin`.

### Файлы
```
plugins/PvPKit/
├── config.yml
└── stats.yml
```

## Установка

1. Скачайте jar каждого нужного плагина для своей версии сервера из Releases.
2. Положите их в `plugins/`.
3. Запустите сервер.
4. Настройте `plugins/<Plugin>/config.yml` и примените `/pvpkit reload`, `/combat reload`, `/spawn reload`, `/kit reload`.

## CI и релизы

GitHub Actions собирает матрицу всех 9 версий и загружает отдельный jar на каждый релизный тег.

## Лицензия

MIT
