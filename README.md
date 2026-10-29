# PvPKit Suite

Набор из трёх независимых плагинов для **Paper 1.21.11**: PvP-ядро, киты и репорты.

| Плагин | Jar | Отвечает за |
|---|---|---|
| **PvPKit** | `PvPKit-1.2.0.jar` | Боевые системы, HUD, статистика, сайдбар, настройки, spawn |
| **KitPvP** | `KitPvP-1.2.0.jar` | Киты, GUI, кулдауны, выдача |
| **ReportPlusPlus** | `ReportPlusPlus-1.2.0.jar` | Жалобы, GUI, уведомления стаффу |

Все три плагина можно ставить вместе или по отдельности. `KitPvP` мягко зависит от `PvPKit` и читает combat tag через metadata, поэтому модули не связаны классами.

Собрано и проверено на Paper `1.21.11-132`, Java 21.

## Сборка

```bash
./gradlew build
```

Готовые jars:

```
build/libs/PvPKit-1.2.0.jar
kitpvp/build/libs/KitPvP-1.2.0.jar
reportplus/build/libs/ReportPlusPlus-1.2.0.jar
```

## Установка

1. Положите нужные jar в `plugins/`.
2. Запустите сервер.
3. Настройте `plugins/PvPKit/config.yml`, `plugins/KitPvP/config.yml` и `plugins/ReportPlusPlus/config.yml`.
4. Примените изменения: `/pvpkit reload`, `/kit reload`.

---

## PvPKit

### Возможности
- Боевые системы `1.8.9`, `1.12`, `1.21.11`.
- Полноценный legacy-режим 1.8.9: без attack cooldown, без sweep, без щитов, блокировка мечом, кнокбек 1.8, W-tap.
- HUD: CPS, броня, keystrokes, ping, серия, комбо, combat tag.
- Статистика: kills, deaths, KDR, серии, топ, kill feed.
- Настраиваемый сайдбар.
- Меню `/settings` с быстрым доступом к китам и репортам, если модули установлены.
- `/spawn` с кулдауном и запретом в бою.
- Combat tag публикуется в metadata `pvpplugin_combat_until` для других плагинов.

### Команды

| Команда | Описание |
|---|---|
| `/hud [on\|off]` | Переключить HUD |
| `/combat [mode\|info]` | Показать или сменить боевую систему |
| `/stats [player]` | Статистика игрока |
| `/stats top [page]` | Топ по убийствам |
| `/stats reset <player>` | Сбросить статистику |
| `/scoreboard [on\|off]` | Переключить сайдбар |
| `/settings` | Меню настроек |
| `/spawn` | Телепорт на спавн |
| `/pvpkit [reload]` | Информация и перезагрузка |

### Права
`pvpplugin.hud`, `pvpplugin.combat`, `pvpplugin.combat.admin`, `pvpplugin.stats`, `pvpplugin.stats.admin`, `pvpplugin.scoreboard`, `pvpplugin.settings`, `pvpplugin.spawn`, `pvpplugin.spawn.bypass`, `pvpplugin.admin`.

### Файлы
```
plugins/PvPKit/
├── config.yml
└── stats.yml
```

### 1.8.9
- Attack strength удерживается на максимуме — кулдаун удара отключён.
- Клиентский индикатор атаки остаётся элементом интерфейса клиента и не влияет на серверный урон.
- Sweep и щиты отключены.
- Блокировка мечом: 50% урона и 20% скорости.
- Кнокбек: `0.4`, спринт-удар `0.9`, вверх `0.4`.
- Спринт сбрасывается после удара.
- Параметры настраиваются:

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

---

## KitPvP

### Возможности
- 16 готовых китов: Warrior, Archer, Tank, Assassin, Berserker, Paladin, Pyromancer, Frost, Ninja, Druid, Sniper, Sumo, Crystal, Trident, Healer, Miner.
- GUI китов с кулдаунами и предпросмотром.
- Настройка предметов, брони, offhand, зачарований и эффектов.
- Кулдауны сохраняются в `cooldowns.yml`.
- Запрет взятия кита в бою, если установлен PvPKit.
- Выдача, сброс кулдаунов и reload.

### Команды

| Команда | Описание |
|---|---|
| `/kit` / `/kits` | Открыть GUI китов |
| `/kit list` | Список китов |
| `/kit <kit>` | Получить кит |
| `/kit preview <kit>` | Предпросмотр |
| `/kit give <player> <kit>` | Выдать кит |
| `/kit reset <player> [kit]` | Сбросить кулдаун |
| `/kit reload` | Перезагрузить конфиг |

### Права
`kitpvp.kit`, `kitpvp.kit.bypass`, `kitpvp.kit.admin`.

### Файлы
```
plugins/KitPvP/
├── config.yml
└── cooldowns.yml
```

Последний кит публикуется в metadata `kitpvp_last_kit`, поэтому сайдбар PvPKit показывает его без прямой зависимости.

---

## ReportPlusPlus

### Возможности
- `/report <player> <reason>` для онлайн- и офлайн-игроков.
- Жалобы сохраняются в `reports.yml`.
- Уведомления стаффу с `[TP]` и `[Close]`.
- GUI жалоб: ЛКМ — телепорт, ПКМ — детали, Shift+ЛКМ — закрыть.
- Список, детали, телепорт к цели или сохранённой локации.
- Кулдаун и лимит длины причины.

### Команды

| Команда | Описание |
|---|---|
| `/report <player> <reason>` | Отправить жалобу |
| `/reports` | Список открытых жалоб |
| `/reports gui` | GUI жалоб |
| `/reports tp <id>` | Телепорт к цели жалобы |
| `/reports close <id>` | Закрыть жалобу |
| `/reports info <id>` | Детали жалобы |

### Права
`reportplus.report.use`, `reportplus.report.notify`, `reportplus.report.staff`, `reportplus.report.bypass`.

### Файлы
```
plugins/ReportPlusPlus/
├── config.yml
└── reports.yml
```

---

## Структура репозитория

```
PvPKit/
├── src/                    # PvPKit core
├── kitpvp/                 # KitPvP
├── reportplus/             # ReportPlusPlus
├── build.gradle
└── settings.gradle
```

## Лицензия

MIT
