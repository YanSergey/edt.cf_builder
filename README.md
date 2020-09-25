# *EDT Cf-builder*

[![Download](https://img.shields.io/github/release/ILazutin/edt.cf_builder?label=download&style=flat)](https://github.com/Ilazutin/edt.cf_builder/releases/latest)
[![GitHub Releases](https://img.shields.io/github/downloads/Ilazutin/edt.cf_builder/latest/total?style=flat-square)](https://github.com/Ilazutin/edt.cf_builder/releases)
[![GitHub All Releases](https://img.shields.io/github/downloads/Ilazutin/edt.cf_builder/total?style=flat-square)](https://github.com/Ilazutin/edt.cf_builder/releases)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=alert_status)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=security_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)


Плагин для [1C:Enterprise Development Tools](https://edt.1c.ru/) для работы с CF-файлами.

## Добавляет в 1C:EDT возможности:
        - Импорт проекта из cf-файла
        - Экспорт проекта в cf-файл (сборка cf-файла)
        - Сборка файла поставки

### Импорт проекта из cf-файла

```В контекстное меню рабочего пространства (клик по пустому месту в навигаторе проектов) и в главное меню "Файл" добавлены пункты:```

        ru: Импортировать проект из cf-файла
        en: Import project from a cf-file
![Menu](/img/import.png "Меню с пунктом")

### Экспорт проекта в cf-файл (сборка cf-файла)

```В контекстное меню проекта добавлен пункт:```

        ru: Собрать cf-файл
        en: Build cf-file

![Menu](/img/export.png "Меню с пунктом")

### Сборка файла поставки

```В контекстное меню проекта добавлен пункт:```

        ru: Собрать файл поставки
        en: Build distr cf

![Menu](/img/export.png "Меню с пунктом")

Плагин совместим с 1C:EDT версий 1.16 и выше.

**Плагин совместим с версиями 1C:EDT, установленными через 1C:EDT Starter**

Тестировался на версиях:

        1.16
        2020.1
        2020.2
        2020.3
        2020.4

### Сборка из исходников:
1. Склонировать проект
2. Запустить консоль в папке подкаталоге **ru.yanygin.dt.cfbuilder.plugin.ui**
3. Выполнить команду **mvn clean verify -P,find-bugs -Dtycho.localArtifacts=ignore**

### Установка:
1. Скачать zip файл со страницы релизов [![Download](https://img.shields.io/github/release/Ilazutin/edt.cf_builder?label=download&style=flat)](https://github.com/ILazutin/edt.cf_builder/releases/latest)

2. В EDT выбрать пункт меню **Справка - Установить новое ПО**.

3. В открывшемся окне нажать кнопку **Добавить**, затем **Архив** и выбрать zip-файл из п. 1. Нажать кнопку **Добавить**

4. В таблице первого окна п. 2 выбрать плагины для установки и проследовать далее по шагам помощника.

При выполнении операций импорта/экспорта, в каталоге для временных файлов EDT **java.io.tmpdir**, заданном в **1cedt.ini**. создаются:

* временная информационная база 1С (необходимо, что бы для файловых баз была доступна лицензия 1С)
* временный каталог для конвертации исходников в формат платформенного xml

Необходимо учитывать наличие достаточного количества свободного пространства. После выполнения операций временные каталоги очищаются автоматически.
