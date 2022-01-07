# ***EDT CF-builder***

[![Download](https://img.shields.io/github/release/YanSergey/edt.cf_builder?label=download&style=flat)](https://github.com/YanSergey/edt.cf_builder/releases/latest)
[![GitHub Releases](https://img.shields.io/github/downloads/YanSergey/edt.cf_builder/latest/total?style=flat-square)](https://github.com/YanSergey/edt.cf_builder/releases)
[![GitHub All Releases](https://img.shields.io/github/downloads/YanSergey/edt.cf_builder/total?style=flat-square)](https://github.com/YanSergey/edt.cf_builder/releases)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=alert_status)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=security_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)


Плагин для [1C:Enterprise Development Tools](https://edt.1c.ru/) для работы с CF/CFE-файлами.

## Добавляет в 1C:EDT возможности:
        - Импорт нового проекта из cf/cfe-файла
        - Импорт изменений в существующий проект из cf/cfe-файла
        - Экспорт проекта в cf/cfe-файл (сборка cf/cfe-файла)

### Импорт проекта из cf/cfe-файла

```В контекстные меню проекта и рабочего пространства (клик по пустому месту в навигаторе проектов) и в главное меню "Файл" добавлены пункты:```

        ru: Импортировать проект из CF/CFE-файла
        en: Import project from a CF/CFE-file
![Menu](/img/import.png "Меню с пунктом")

```Для того, что бы сравнить/объединить проект конфигурации/расширения с cf/cfe-файлом, нужно кликнуть ПКМ по проекту, в открывшемся диалоговом окне проект уже будет выбран (но можно поменять), нужно выбрать cf/cfe-файл, с которым нужно сравнить/объединить проект, выбрать одну из привязанных к проекту инфобаз. После нажатия ОК cf/cfe-файл будет загружен в выбранную инфобазу и запустится штатный механизм EDT импорта изменений из инфобазы.```

```Импорт изменений возможен только через одну из привязанных к проекту инфобаз.```

```ВНИМАНИЕ! При выполнении этой операции, конфигурация/расширение в инфобазе будет полностью затерта из выбранного файла.```

### Экспорт проекта в cf/cfe-файл (сборка cf/cfe-файла)

```В контекстное меню проекта добавлен пункт:```

        ru: Собрать из проекта CF/CFE-файл
        en: Build from the project a CF/CFE-file

![Menu](/img/export.png "Меню с пунктом")

### Сделано единое окно настройки импорта/экспорта
![Menu](/img/window.png "Меню с пунктом")

---
### Установка:
### 1. Через файл
1. Скачать zip файл со страницы релизов [![Download](https://img.shields.io/github/release/YanSergey/edt.cf_builder?label=download&style=flat)](https://github.com/YanSergey/edt.cf_builder/releases/latest)
2. В главном меню EDT выбрать пункт **Справка - Установить новое ПО**.
3. Если репозиторий еще не добавлен, нажать кнопку **Добавить**, затем **Архив** и выбрать zip-файл из п. 1. Нажать кнопку **Добавить**
4. Выбрать репозиторий, снять флаг **Группировать элементы по категории**, выбрать плагин, нажать **Далее** и пройти далее по шагам мастера установки.

### 2. Через сайт обновлений
1. В главном меню EDT выбрать пункт **Справка - Установить новое ПО**.
2. Если репозиторий еще не добавлен, нажать кнопку **Добавить**, в поле **Расположение** вставить путь к сайту обновлений https://yansergey.github.io/edt.cf_builder, в поле **Имя** вставить **EDT CF-builder**
3. Выбрать репозиторий, снять флаг **Группировать элементы по категории**, выбрать плагин, нажать **Далее** и пройти далее по шагам мастера установки.

---
**Плагин совместим с версиями 1C:EDT, установленными через 1C:EDT Starter**

Тестировался на версиях 1.16 - 2021.3.x

Таблица совместимости версий плагина с версиями EDT:
Версия EDT              |Версия плагина
-------------           |-------------
1.16 - 2020.5           |1.0.0 - 1.2.0
2020.6                  |1.3.0
2021.1.0 - 2021.1.7     |1.3.1
2021.2.0 - 2021.2.12    |1.3.2 (сайт обновлений)
2021.3.0 - ...          |1.3.3 (предварительная версия)

На сайте обновлений находится версия плагина для последней релизной версии EDT.

---
### Сборка из исходников:
1. Склонировать проект
2. Запустить консоль в папке подкаталоге **bundles**
3. Выполнить команду **mvn clean verify -P,find-bugs -Dtycho.localArtifacts=ignore**

---
Для операций импорта/экспорта возможно использовать как временную, так и существующую инфобазу.

При использовании существующей инфобазы для **экспорта (сборки)** можно ее привязать к проекту (если не привязана) установив флаг **Связать инфобазу с проектом (Associate an infobase with a project)**.

При использовании существующей инфобазы для **импорта**, если будет выбрана непривязанная к проекту инфобаза - она привяжется автоматически (заблокирована возможность снять флаг **Связать инфобазу с проектом (Associate an infobase with a project)**).

При выборе для импорта/экспорта проекта расширения (cfe), список инфобаз будет получен по родительскому проекту.

---
При выполнении операций импорта/экспорта, в каталоге для временных файлов EDT **java.io.tmpdir**, заданном в **1cedt.ini**. могут создаваться:

* временная информационная база 1С (необходимо, что бы для файловых баз была доступна лицензия 1С)
* временный каталог для конвертации исходников в формат платформенного xml

Необходимо учитывать наличие достаточного количества свободного пространства. После выполнения операций временные каталоги очищаются автоматически.

---

Еще плагины:

* https://github.com/YanSergey/edt.externalDesignerLauncher - Плагин для ЕДТ добавляющий возможность запускать альтернативный Конфигуратор (например, Снегопат) прямо из меню ЕДТ
