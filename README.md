# Частина 2: основна технічна частина

> Мод **CrimsonMoon** (Fabric, Minecraft 1.20.x, GeckoLib для моделей, Trinkets для слотів).
> Усі посилання нижче ведуть на файли в репозиторії:
> `https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/...`

## Зміст

- [1. Основні модулі проєкту](#1-основні-модулі-проєкту)
  - [1.1 Ядро мода](#11-ядро-мода)
  - [1.2 Боси (папка entity)](#12-боси-папка-entity)
  - [1.3 Менеджери спавну та світу (папка world)](#13-менеджери-спавну-та-світу-папка-world)
  - [1.4 Стан світу](#14-стан-світу)
  - [1.5 Журнал босів](#15-журнал-босів)
  - [1.6 Портал і ключ](#16-портал-і-ключ)
  - [1.7 Предмети](#17-предмети)
  - [1.8 Додаткові системи (страх, параноя, спостерігач)](#18-додаткові-системи-страх-параноя-спостерігач)
- [2. Основні механіки](#2-основні-механіки)
  - [2.1 Система прокляття](#21-система-прокляття)
  - [2.2 Чотири боси](#22-чотири-боси)
  - [2.3 Портал до фінального боса](#23-портал-до-фінального-боса)
  - [2.4 Журнал босів](#24-журнал-босів)
  - [2.5 Особлива зброя босів](#25-особлива-зброя-босів)
  - [2.6 Фінал і зняття прокляття](#26-фінал-і-зняття-прокляття)

---

## 1. Основні модулі проєкту

Весь код лежить у пакеті `com.crimsonmoon`. Він розділений на: ядро (ініціалізація, реєстри), `entity` (боси та їхні цілі ШІ), `world` (спавн босів і стан світу), `item` (предмети), `client` (GUI, рендер, звук), `mixin`.

### 1.1 Ядро мода

| Файл | Клас | За що відповідає |
|------|------|------------------|
| [CrimsonMoon.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/CrimsonMoon.java) | `CrimsonMoon` | Точка входу (`ModInitializer`). Реєструє все: предмети, блоки, сутностей, менеджери. Тут же живе **система прокляття** (`applyCurse`, `removeCurse`), спавн Моноліту, спавн боса Nightmare у печері, фінальний відлік та «астральна проєкція» окулярів |
| [CrimsonMoonClient.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/CrimsonMoonClient.java) | `CrimsonMoonClient` | Клієнтська частина: клавіші (**J** — журнал, окрема клавіша для окулярів), реєстрація рендерерів босів, HUD-оверлеї |
| [ModEntities.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/ModEntities.java) | `ModEntities` | Реєстр усіх сутностей: боси (Nightmare, Boxer, Thorncaller, Monsterik), Bloodwalker, Watcher, снаряди |
| [ModItems.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/ModItems.java) | `ModItems` | Реєстр предметів (див. 1.7) |
| [ModBlocks.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/ModBlocks.java) | `ModBlocks` | Блоки: `MONOLIT` і кривава руда |
| [ModDimensions.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/ModDimensions.java) | `ModDimensions` | Ключ виміру `BOSS_REALM` — арена фінального боса |

### 1.2 Боси (папка entity)

Кожен бос — окремий клас, який наслідує `HostileEntity` (Thorncaller — `ZombieEntity`) і працює через GeckoLib (`GeoEntity`). Модель, текстура та анімації лежать у `resources/assets/crimsonmoon/geo` та `.../animations`.

| Файл | Клас | За що відповідає |
|------|------|------------------|
| [NightmareEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/NightmareEntity.java) | `NightmareEntity` | Бос №1 «Жах». Переслідування, поява за спиною, вигризання з блоків |
| [StalkPlayerGoal.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/StalkPlayerGoal.java) | `StalkPlayerGoal` | Ціль ШІ: іти до гравця навіть без прямої видимості |
| [NightmareMeleeAttackGoal.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/NightmareMeleeAttackGoal.java) | `NightmareMeleeAttackGoal` | Ближній бій Nightmare |
| [BoxerEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BoxerEntity.java) | `BoxerEntity` | Бос №2 «Боксер». Три фази, ривок, стрибок, ударна хвиля, телепорт у люті |
| [BoxerStalkGoal.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BoxerStalkGoal.java) | `BoxerStalkGoal` | Ціль ШІ Боксера: підкрадання до гравця |
| [ThorncallerEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/ThorncallerEntity.java) | `ThorncallerEntity` | Бос №3 «Терновик». Чотири здібності (призов, шипи, шторм, піка) |
| [ThorncallerTreeClimbGoal.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/ThorncallerTreeClimbGoal.java), [ThorncallerTreeHideGoal.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/ThorncallerTreeHideGoal.java) | `...TreeClimbGoal`, `...TreeHideGoal` | Взаємодія з джунглями: лазить по деревах і ховається за стовбурами |
| [BloodwalkerEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BloodwalkerEntity.java) | `BloodwalkerEntity` | Міньйон, якого призиває Терновик |
| [MonsterikEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/MonsterikEntity.java) | `MonsterikEntity` | Фінальний бос «Монстерік». Фази, ривок, кільця блискавок, хвилі мобів |
| [WatcherEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/WatcherEntity.java) | `WatcherEntity` | «Спостерігач» — моб, що лякає гравця (не бос) |
| [BloodBoltEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BloodBoltEntity.java), [BloodClotEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BloodClotEntity.java) | снаряди | Снаряди зброї мода |

### 1.3 Менеджери спавну та світу (папка world)

Менеджер = клас, який вішає обробник на `ServerTickEvents.END_SERVER_TICK` і сам вирішує, коли й де створити боса або структуру.

| Файл | Клас | За що відповідає |
|------|------|------------------|
| [ThorncallerSpawnManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/ThorncallerSpawnManager.java) | `ThorncallerSpawnManager` | Спавн Терновика: лише в джунглях, лише під прокляттям, лише поки він не вбитий. Лише один екземпляр одночасно |
| [BoxerColosseumManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/BoxerColosseumManager.java) | `BoxerColosseumManager` | Знаходить найближчу пустелю, вирівнює площадку, ставить колізей і спавнить Боксера |
| [MonsterikBossManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/MonsterikBossManager.java) | `MonsterikBossManager` | Спавн і єдиність Монстеріка на арені виміру `BOSS_REALM` |
| [PortalManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/PortalManager.java) | `PortalManager` | Ставить портал у світі після перемоги над трьома босами |
| [BossArenaManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/BossArenaManager.java) | `BossArenaManager` | Телепорт гравця на арену фінального боса (`enter`) і назад (`exit`), одноразове розміщення арени |
| [ArenaStructureLoader.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/ArenaStructureLoader.java) | `ArenaStructureLoader` | Потоковий завантажувач величезної структури арени (~4,4 млн блоків), щоб не тримати її цілком у пам'яті |
| [BossArenaState.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/BossArenaState.java) | `BossArenaState` | Зберігає, чи вже розміщена арена, і її розмір (переживає перезапуск сервера) |
| [WatcherManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/WatcherManager.java) | `WatcherManager` | Періодичний спавн Спостерігача за спиною гравця |
| [PortalKeyDrops.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/PortalKeyDrops.java) | `PortalKeyDrops` | Допоміжна логіка видачі ключа порталу |
| [ModWorldGen.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/ModWorldGen.java) | `ModWorldGen` | Генерація руди в світі |

### 1.4 Стан світу

Прогрес гри зберігається не в пам'яті, а в **`PersistentState`** — тому він переживає перезапуск сервера.

| Файл | Клас | Що зберігає |
|------|------|-------------|
| [CrimsonMoonWorldState.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/CrimsonMoonWorldState.java) | `CrimsonMoonWorldState` | Головний стан: `isCurseActive`, `nightmareSpawned/Defeated`, `boxerArenaPlaced/Spawned/Defeated` + координати колізею, `monolitPlaced` + координати, `monsterikDefeated`, `portalKeyDropped`, `portalPlaced` + координати, `finaleCountdownActive`, `finaleTicks`. Методи `writeNbt` / `createFromNbt` серіалізують усе в NBT. Метод `checkAndDropKey` видає ключ, коли вбито всіх трьох босів |
| [ThorncallerBossState.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/ThorncallerBossState.java) | `ThorncallerBossState` | Окремий стан Терновика: `defeated` (назавжди вбитий) і `awaitingLoot` (вбитий, але лут ще не забрали) |

### 1.5 Журнал босів

| Файл | Клас | За що відповідає |
|------|------|------------------|
| [BossJournalData.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/BossJournalData.java) | `BossJournalData` | Пакет даних: хто вбитий, чи активне прокляття, координати Моноліту і колізею. Має `write` / `read` для передачі по мережі |
| [JournalNetworking.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/JournalNetworking.java) | `JournalNetworking` | Сервер приймає запит `journal_request`, збирає `BossJournalData` зі стану світу й відповідає пакетом `journal_response` |
| [BossJournalScreen.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/client/gui/BossJournalScreen.java) | `BossJournalScreen` | Екран журналу: рядок на кожен етап (Моноліт, Жах, Боксер, Терновик, Монстерік) з іконкою, описом, місцем і статусом |

### 1.6 Портал і ключ

| Файл | Клас | За що відповідає |
|------|------|------------------|
| [PortalManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/PortalManager.java) | `PortalManager` | Ставить структуру `portal` за 1000–1600 блоків від гравця |
| [PortalKeyItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/PortalKeyItem.java) | `PortalKeyItem` | Ключ: ПКМ здалеку тягне до порталу, ПКМ біля порталу телепортує на арену |
| [BossArenaManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/BossArenaManager.java) | `BossArenaManager` | Сам перехід у вимір `BOSS_REALM` |

### 1.7 Предмети

| Файл | Клас | За що відповідає |
|------|------|------------------|
| [ModItems.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/ModItems.java) | `ModItems` | Реєстр: кривавий набір інструментів, `BLOOD_STAFF`, `BLOOD_MACE`, `BLOOD_BOW`, `BLOOD_INGOT`, `BLOOD_CLOT`, `BLOOD_MONOLIT`, броня, щит, `BLOOD_NECKLACE`, `XRAY_GLASSES`, `PORTAL_KEY`, яйця спавну |
| [BloodStaffItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodStaffItem.java), [BloodMaceItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodMaceItem.java), [BloodBowItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodBowItem.java) | зброя босів | Трофейна зброя: посох (Терновик), булава (Жах), лук (Боксер). Здібності витрачають **страх** |
| [BloodNecklaceItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodNecklaceItem.java) | `BloodNecklaceItem` | Намисто прокляття, яке видається автоматично в слот Trinkets |
| [ArmorSetEffectManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/ArmorSetEffectManager.java) | `ArmorSetEffectManager` | Бонус повного комплекту броні (+16 HP, тобто +8 сердець, та ефекти) |
| [NoDropRegistry.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/NoDropRegistry.java), [PendingItemStash.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/PendingItemStash.java) | службові | Захищені від викидання предмети й повернення їх гравцеві після смерті (`ServerPlayerEvents.COPY_FROM`) |

### 1.8 Додаткові системи (страх, параноя, спостерігач)

| Файл | Клас | За що відповідає |
|------|------|------------------|
| [FearManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/FearManager.java) | `FearManager` | Шкала страху 0–100 як ігровий ресурс. Росте біля страшних мобів, у темряві й на арені (на арені +2.0 за тик). Синхронізується на клієнт, малюється в [FearHudOverlay.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/client/gui/FearHudOverlay.java) |
| [ParanoiaSystem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/ParanoiaSystem.java) | `ParanoiaSystem` | Раз на 5 с перевіряє кількість «тригерних» блоків біля гравця. Рівні: 3–5 (ambient-звук), 6–9 (биття серця Вардена й повідомлення), 10+ (Darkness або Slowness) |
| [WatcherManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/WatcherManager.java) | `WatcherManager` | Раз на 3 хвилини для кожного гравця 45% шанс заспавнити Спостерігача за 24–28 блоків від нього, поза конусом огляду |

---

## 2. Основні механіки

### Загальний ланцюжок гри

```
Моноліт зламано → ПРОКЛЯТТЯ
      ├── Жах (печери)
      ├── Боксер (колізей у пустелі)
      └── Терновик (джунглі)
              ↓  усі троє вбиті
   Ключ порталу падає + портал з'являється в світі
              ↓
   Вимір BOSS_REALM → Монстерік (фінал)
              ↓
   Окуляри → відлік 10 с → прокляття знято
```

### 2.1 Система прокляття

**Файл:** [CrimsonMoon.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/CrimsonMoon.java) (методи `applyCurse`, `removeCurse`, обробники в `onInitialize`).

**Як влаштовано:**

1. **Моноліт.** Коли гравець уперше заходить у світ, обробник `ServerPlayConnectionEvents.JOIN` через `spawnMonolitNearPlayer` ставить структуру Моноліту у випадковій точці на відстані від 20 до 1000 блоків (кільцями по 60 блоків, шукає рівну суходільну ділянку). Прапорець зберігається в scoreboard-цілі `crimson_monolit` (`#monolit = 1`).
2. **Активація.** Коли гравець ламає блок `MONOLIT` (`PlayerBlockBreakEvents.AFTER`), значення стає `2`, і **всі** гравці на сервері проклинаються через `applyCurse`: отримують тег `cursed_player`, у `CrimsonMoonWorldState` стає `isCurseActive = true`, у слот Trinkets потрапляє `BLOOD_NECKLACE`, накладається сліпота, починається гроза.
3. **Поширення.** Кожен тик сервер перевіряє: якщо хоч один гравець проклятий, то прокляття отримує й той, хто щойно зайшов.
4. **Ефекти світу під прокляттям:**
   - випадкові страшні звуки (1 шанс із 400 за тик на гравця: Creeper, Ендермен, скло);
   - гасіння факелів у радіусі 4 блоків (звук + дим);
   - постійний дощ.
5. **Обмеження.** У вимірі `BOSS_REALM` ламати блоки заборонено (`PlayerBlockBreakEvents.BEFORE`).
6. **Зняття.** `removeCurse` прибирає тег, намисто, сліпоту й повертає ясну погоду (див. [2.6](#26-фінал-і-зняття-прокляття)).

**Що прокляття запускає:** спавн Жаху в печері, будівництво колізею, дозвіл на спавн Терновика.

![Прокляття](readme_media/curse.gif)

---

### 2.2 Чотири боси

#### Бос 1 — Жах (Nightmare)

**Файл:** [NightmareEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/NightmareEntity.java)

| Параметр | Значення |
|----------|----------|
| HP / урон / броня / швидкість | 150 / 12 / 5 / 0.35 |
| Де з'являється | Глибоко під землею |

**Спавн.** Раз на 5 секунд (`server.getTicks() % 100`) `CrimsonMoon.java` перевіряє: прокляття активне, Жах ще не заспавнений і не вбитий, а проклятий гравець нижче Y = 55. Тоді `findSpaciousCave` шукає просторий простір печери в радіусі 20 блоків, і там створюється Жах.

**Поведінка:**
- постійно переслідує проклятого гравця, навіть без прямої видимості (`StalkPlayerGoal`);
- **поява за спиною:** якщо не переслідує гравця впритул, кожні 12–24 с телепортується на 5–9 блоків за спину гравця зі звуком; у 1 випадку з 4 додається Darkness на 2 с;
- **не задихається в блоках:** якщо ноги чи голова замуровані, розчищає область 3×3×3 (перевірка кожні 2 тики);
- не зникає, коли гравець вмирає.

**Смерть:** `nightmareDefeated = true`, падає `BLOOD_MACE`, викликається `checkAndDropKey`.

![Жах](readme_media/boss_nightmare.gif)

---

#### Бос 2 — Боксер (Boxer)

**Файл:** [BoxerEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BoxerEntity.java)

| Параметр | Значення |
|----------|----------|
| HP / урон / броня | 400 / 16 / 8 |
| Швидкість | 0.32 у фазі 1, +0.07 за кожну наступну фазу |
| Опір відкиданню | 0.8 |
| Де | Колізей у пустелі |

**Фази** (`updatePhase`): понад 66% HP — фаза 1, 33–66% — фаза 2, нижче 33% — фаза 3. При зміні фази Боксер реве (звук Ravager) і стає швидшим.

**Атаки** (кожна має власний кулдаун, а нижчі HP його скорочують):

| Атака | Метод | Умови | Що робить |
|-------|-------|-------|-----------|
| Стрибок | `doLeap` | відстань 6–22, на землі | Стрибає на гравця, при приземленні — ударна хвиля ×2 |
| Ривок | `doCharge` | відстань 4–16 | Різкий розгін у бік гравця |
| Ударна хвиля | `doShockwave` | фаза ≥ 2, відстань < 5 | Урон `0.75 × attack` у радіусі 4 і відкидання вгору |
| Телепорт у люті | `doRageTeleport` | 100 тиків не може дістатися гравця (не бачить його) | Телепорт за спину, удар, ударна хвиля ×1.2 |
| Апперкот | `tryAttack` | фаза ≥ 2, спец-кулдаун | Підкидає гравця вгору |
| Захоплення | `tryAttack` | фаза 3 | Притягує гравця до себе й завдає додатковий урон |

Також Боксер сам прориває стіни, якщо його замурували (`freeSpaceIfBuried`).

**Смерть:** `boxerDefeated = true`, падає `BLOOD_BOW`.

![Боксер](readme_media/boss_boxer.gif)

---

#### Бос 3 — Терновик (Thorncaller)

**Файли:** [ThorncallerEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/ThorncallerEntity.java), [ThorncallerSpawnManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/ThorncallerSpawnManager.java), [ThorncallerBossState.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/ThorncallerBossState.java)

| Параметр | Значення |
|----------|----------|
| HP | 250 |
| Де | Тільки в біомах джунглів (`BiomeTags.IS_JUNGLE`) |

**Спавн.** Раз на секунду `ThorncallerSpawnManager` перевіряє, чи є гравець у джунглях. Умови: прокляття активне, Терновик не вбитий і не чекає підбору луту. Він з'являється на відстані 14–20 блоків від гравця **поза його полем зору** (виключений конус 100° попереду). Якщо всі гравці вийшли з джунглів, а бос не вбитий, він тихо зникає й заспавниться знову при наступному заході. Одночасно існує лише один.

**Чотири здібності.** Між будь-якими двома атаками є загальна пауза 4 секунди (`GLOBAL_ATTACK_DELAY_TICKS`), щоб їх не кастували підряд.

| № | Здібність | Кулдаун | Що робить |
|---|-----------|---------|-----------|
| 1 | Призов (`summonMinions`) | 25 с | Викликає по 2 Bloodwalker, максимум 6 живих поруч |
| 2 | Шипи з землі (`startSpikeAttack`) | 20 с | 3 кільця по 8 шипів навколо (радіуси ≈ 2–9 блоків), урон 10, якщо гравець стоїть біля шипа, коли той виріс |
| 3 | Голковий шторм (`startStormAttack`) | 28 с | Розширювальне кільце на 360° (24 снаряди, радіус до 12), стіна заввишки ~1.7 блока, тож його не перестрибнути. Урон 5 + Slowness IV на 5 с |
| 4 | Шипова піка (`startLanceAttack`) | 22 с | 5 швидких шипів по прямій на гравця (потрібно ухилятися вбік). Урон 5 + Poison II на 4 с |

**ШІ в джунглях:** `ThorncallerTreeClimbGoal` (лазить по деревах) і `ThorncallerTreeHideGoal` (ховається за стовбурами).

**Смерть:** `ThorncallerBossState` ставить `defeated = true` (більше ніколи не заспавниться), падає `BLOOD_STAFF` і 1–3 `BLOOD_INGOT`.

![Терновик](readme_media/boss_thorncaller.gif)

---

#### Бос 4 — Монстерік (Monsterik, фінал)

**Файли:** [MonsterikEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/MonsterikEntity.java), [MonsterikBossManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/MonsterikBossManager.java)

| Параметр | Фаза 1 | Фаза 2 (≤ 66% HP) | Фаза 3 (≤ 33% HP) |
|----------|--------|-------------------|-------------------|
| HP | 400 | 400 | 400 |
| Швидкість | 0.34 | 0.40 | 0.47 |
| Урон | 7 | 10 | 14 |
| Кулдаун ривка | немає | 16 с | 9 с |
| Кулдаун блискавок | 18 с | 14 с | 10 с |

Броня 6. Імунітет до блискавок (`damage` повертає `false` для `LIGHTNING_BOLT`).

**Спавн (`MonsterikBossManager`).** Бос завжди з'являється у фіксованій точці арени `(39, 6, 0)` у вимірі `BOSS_REALM`. Менеджер гарантує **єдиність**: якщо завантажується другий екземпляр (наприклад, через `/summon`), зайвий тихо видаляється. Автоспавн працює лише до першої смерті, далі тільки командою `/monsterikboss spawn`.

**Атаки:**
- **Ривок (`startDash`)**: з фази 2, відстань 4–12, урон + бонус 4, відкидання;
- **Кільця блискавок (`startLightningRingAttack`)**: 3 кільця по 8 ударів, спочатку показується «телеграф» (бірюзові частинки, 18 тиків), потім удар. Урон 12;
- **Подія призову (`startSummonEvent`)** у фазі 3 (один раз): бос телепортується до центру, стає **невразливим**, AI вимикається, і вперед виходять 4 хвилі: 4 зомбі → 2 скелети + 2 хаски → 2 розбійники + 1 віндикатор → 3 віндикатори + 2 євокери. Після всіх хвиль бос знову стає вразливим.

**Смерть:** вибух, звук смерті дракона, падають `XRAY_GLASSES`, `monsterikDefeated = true`, залишкові мобі хвиль видаляються, автоспавн вимикається.

![Монстерік](readme_media/boss_monsterik.gif)

---

### 2.3 Портал до фінального боса

**Файли:** [CrimsonMoonWorldState.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/CrimsonMoonWorldState.java) (`checkAndDropKey`), [PortalManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/PortalManager.java), [PortalKeyItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/PortalKeyItem.java), [BossArenaManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/world/BossArenaManager.java)

**Послідовність:**

1. Після смерті **кожного** з трьох босів викликається `checkAndDropKey`. Коли умова `boxerDefeated && nightmareDefeated && thorncaller defeated` виконана, той бос, що помер останнім, випускає `PORTAL_KEY` (з ефектами частинок і звуком). Прапорець `portalKeyDropped` не дає видати ключ двічі.
2. `PortalManager` раз на 5 секунд перевіряє ту саму умову. Коли вона виконана, структура `portal` ставиться на відстані **1000–1600 блоків** від гравця у випадковому напрямку (до 40 спроб, шукає тверду землю без води й льоду). Координати зберігаються в стані світу.
3. **Ключ** у руках:
   - ПКМ **здалеку**: рух-«ривок» у бік порталу («Ключ тягне тебе до порталу...»), тобто це навігація;
   - ПКМ **ближче ніж 6 блоків**: `BossArenaManager.enter` телепортує гравця у вимір `BOSS_REALM`;
   - якщо портала ще немає: «Портал ще не з'явився. Здолай трьох стражів!»
4. **Арена.** Структура `boss_arena` величезна (~4,4 млн блоків), тому її розміщує `ArenaStructureLoader` потоково, лише при першому заході. Гравця висаджує в точку `(-100, 4, 1)`, там чекає Монстерік.

---

### 2.4 Журнал босів

**Файли:** [BossJournalScreen.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/client/gui/BossJournalScreen.java), [JournalNetworking.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/JournalNetworking.java), [BossJournalData.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/BossJournalData.java), [CrimsonMoonClient.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/CrimsonMoonClient.java)

**Як це працює (клієнт ↔ сервер):**

```
Гравець натискає J ──► клієнт відкриває BossJournalScreen
                         │ надсилає пакет journal_request
                         ▼
            Сервер (JournalNetworking.registerServer)
            читає CrimsonMoonWorldState + ThorncallerBossState
            збирає BossJournalData ──► пакет journal_response
                         ▼
            Клієнт оновлює latestData і перемальовує екран
```

Журнал нічого не рахує сам: **джерело правди** — стан світу на сервері. Тому всі гравці бачать однаковий прогрес.

**Що показує екран** (п'ять рядків-етапів):
- Етап 1: Моноліт ([ЦІЛИЙ] / [ЗРУЙНОВАНО]); поки прокляття не настало, можна побачити координати Моноліту;
- Жах: підказка «Печери»;
- Боксер: «Колізей у пустелі» + координати колізею, коли його вже побудовано;
- Терновик: «Джунглі»;
- Монстерік: «За порталом».

Для кожного боса показується статус, перемога підсвічується зеленим. Інтерфейс має дві мови (українську й англійську).

![Журнал](readme_media/journal.png)

---

### 2.5 Особлива зброя босів

Три боси після смерті залишають унікальну зброю. Усі три предмети лежать у папці [item](https://github.com/Pranichek/Mode-Minecraft/tree/main/src/main/java/com/crimsonmoon/item), а снаряди для них — у папці [entity](https://github.com/Pranichek/Mode-Minecraft/tree/main/src/main/java/com/crimsonmoon/entity).

| Зброя | Хто дає | Файл предмета | Снаряд |
|-------|---------|---------------|--------|
| Кривава булава (`BLOOD_MACE`) | Жах | [BloodMaceItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodMaceItem.java) | немає (ближній бій) |
| Кривавий посох (`BLOOD_STAFF`) | Терновик | [BloodStaffItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodStaffItem.java) | [BloodBoltEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BloodBoltEntity.java) |
| Кривавий лук (`BLOOD_BOW`) | Боксер | [BloodBowItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodBowItem.java) | [BloodClotEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BloodClotEntity.java) |

#### Ресурс «Страх»

Здібності булави й посоха **коштують страх** ([FearManager.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/FearManager.java)). Шкала страху (0–100) росте, коли гравець поруч зі страшними мобами, у темряві або на арені фінального боса, і малюється на екрані як смужка «Страх».

Перевірка влаштована у два кроки, щоб гра не «лагала» для гравця:
1. **Клієнт** (`hasEnoughFearClientSide`) дивиться на значення зі шкали HUD і вирішує, чи показувати анімацію.
2. **Сервер** (`FearManager.consume`) є справжнім суддею: сам списує страх і відхиляє дію, якщо його не вистачило («Not enough Fear...»).

Після перемоги над Монстеріком (`monsterikDefeated`) зброя стає безкоштовною: сервер більше не списує страх.

---

#### Кривава булава

**Файл:** [BloodMaceItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodMaceItem.java) (наслідує `SwordItem`)

| Дія | Що відбувається |
|-----|-----------------|
| Звичайний удар | Булава з уроном 7, швидкістю атаки −3.2 (повільний, важкий). У `postHit` на цілі з'являються криваві частинки та звук сильного удару |
| **Shift + ПКМ** | **Ударна хвиля** (`shockwave`) |

**Ударна хвиля:**
- вартість: **18 страху**, перезарядка **7 секунд** (`getItemCooldownManager`);
- радіус **4.5 блоків**, урон **8** усім живим істотам навколо (крім самого гравця);
- відкидання: горизонтальне 2.2 і вгору 0.6; напрямок рахується від гравця до цілі;
- візуал: три кільця червоних частинок, `SONIC_BOOM` і вибух, звуки ковадла та Ravager.

![Булава](readme_media/mace.gif)

---

#### Кривавий посох

**Файли:** [BloodStaffItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodStaffItem.java), [BloodBoltEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BloodBoltEntity.java)

Посох має **дві здібності з окремими перезарядками**. Час готовності зберігається в NBT самого предмета (`BoltReadyTick`, `SlamReadyTick`), тож кожен посох рахує свої таймери незалежно.

| Дія | Вартість страху | Перезарядка | Що відбувається |
|-----|-----------------|-------------|-----------------|
| **ПКМ** | 5 | 1 секунда | `shootBolt`: випускає **кривавий снаряд** (`BloodBoltEntity`) у напрямку погляду зі швидкістю 1.6. Урон **12** (магічний, `indirectMagic`), частинки сліду, зникає при влучанні в моба чи блок |
| **Shift + ПКМ** | 20 | 13 секунд | `groundSlam`: **удар по землі** в радіусі **10 блоків**, урон **24**, відкидання 2.2 |

![Посох](readme_media/staff.gif)

---

#### Кривавий лук

**Файли:** [BloodBowItem.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/item/BloodBowItem.java), [BloodClotEntity.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/entity/BloodClotEntity.java)

Лук працює як звичайний (тримати ПКМ, щоб натягнути, відпустити, щоб вистрілити), але має дві відмінності:

1. **Не потребує стріл.** Замість стріли він запускає **згусток крові** (`BloodClotEntity`), а ціною пострілу є міцність самого лука (`stack.damage(1, ...)`).
2. **Сила пострілу залежить від натягу.** Функція `getPullProgress` дає значення від 0 до 1 (повний натяг приблизно за 1 секунду). Менше 0.1 — пострілу немає, швидкість згустку = `натяг × 3`.

Влучання згустка: урон **5** і ефект **Слабкість** (`WEAKNESS`) на 3 секунди. Згусток живе максимум 100 тиків (5 секунд), потім зникає, а при влучанні залишає частинки крові. Страх лук **не витрачає**.

![Лук](readme_media/bow.gif)

---

#### Порівняння

| | Булава | Посох | Лук |
|---|--------|-------|-----|
| Тип | ближній бій + зона | далекобійний + зона | далекобійний |
| Здібності | ударна хвиля | снаряд + удар по землі | згусток крові |
| Витрата страху | 18 | 5 / 20 | 0 |
| Перезарядка | 7 с | 1 с / 13 с | залежить від натягу |
| Найбільший урон | 8 (хвиля) | 24 (удар по землі) | 5 + Слабкість |

---

### 2.6 Фінал і зняття прокляття

Реалізовано в `ServerTickEvents.END_SERVER_TICK` у [CrimsonMoon.java](https://github.com/Pranichek/Mode-Minecraft/blob/main/src/main/java/com/crimsonmoon/CrimsonMoon.java):

1. Монстерік мертвий (`monsterikDefeated`), а хтось підібрав `XRAY_GLASSES` → `finaleCountdownActive = true`, `finaleTicks = 200` (10 секунд), звук і повідомлення «Реліквія знайдена... Світ починає руйнуватися!».
2. Останні 10 секунд на екрані йде відлік «Повернення через: N».
3. По закінченні: `removeCurse`, і всіх гравців із виміру `BOSS_REALM` телепортує на спавн Overworld («Кошмар завершено... Ви повернулися додому.»).

**Окуляри** (`XRAY_GLASSES`) також дають «астральну проєкцію»: через клавішу гравець на 10 секунд переходить у режим спостерігача (`GameMode.SPECTATOR`), а потім повертається в тіло на вихідну точку.

---