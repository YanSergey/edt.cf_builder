# EDT Cf-builder

[![Download](https://img.shields.io/github/release/YanSergey/edt.cf_builder?label=download&style=flat)](https://github.com/YanSergey/edt.cf_builder/releases/latest)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=alert_status)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=security_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=YanSergey_EDT_CF_Builder&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=YanSergey_EDT_CF_Builder)


Плагин для [1C:Enterprise Development Tools](https://edt.1c.ru/) для сборки CF файла

```Добавляет в контекстное меню проекта пункт "Собрать cf-файл":```

![Menu](/img/Menu.png "Меню с пунктом")

Плагин совместим с EDT версий: 1.16, 2020.1, 2020.2, 2020.3

### Установка:
1. Скачать jar файл со страницы релизов [![Download](https://img.shields.io/github/release/YanSergey/edt.cf_builder?label=download&style=flat)](https://github.com/YanSergey/edt.cf_builder/releases/latest)

2. Поместить файл в каталог с установленным EDT в подкаталог "***plugins***"

Для сборки cf-файла используется создание временных ИБ и workspace в каталоге для временных файлов EDT "***java.io.tmpdir***", заданном в "***1cedt.ini***". Необходимо учитывать наличие свободного пространства.