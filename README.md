# Проект: Image Classification and Camera Integration

## Описание
Этот проект представляет собой мобильное приложение для Android, разработанное на языке Kotlin. Основная цель приложения — классификация изображений с использованием модели машинного обучения TensorFlow Lite.

## В данном проекте было реализовано:

### 1. **Классификация изображений**
- Использование модели TensorFlow Lite для классификации изображений.
- Поддержка работы с камерой устройства для захвата изображений в реальном времени.
- Отображение результатов классификации с указанием вероятности для каждого класса.
- Настройка порога вероятности для фильтрации результатов.

### 2. **Анимации и переходы**
- Плавное появление элементов интерфейса с использованием анимаций.
- Отображение GIF-анимаций с последующей заменой на статичные изображения.
- Переходы между экранами с использованием кнопок.

### 3. **Интеграция с камерой**
- Захват изображений с помощью камеры устройства.
- Предварительная обработка изображений для передачи в модель TensorFlow Lite.
- Отображение захваченного изображения на экране.

## Технологии и инструменты
- **Язык программирования**: Kotlin.
- **Машинное обучение**: TensorFlow Lite.
- **Библиотеки**:
  - `Glide` — для загрузки и отображения GIF-анимаций.
  - `CameraX` — для работы с камерой устройства.
- **Анимации**:
  - `ObjectAnimator`
  - `AnimatorSet`

## Структура проекта

### Основные классы:
1. **`MainActivity`**:
   - Отвечает за анимации и переходы между экранами.
   - Загружает и отображает GIF-анимации.

2. **`ImageClassification`**:
   - Реализует классификацию изображений с использованием модели TensorFlow Lite.
   - Поддерживает захват изображений с камеры и их обработку.

3. **`MainActivity2`**:
   - Реализует классификацию изображений в реальном времени с использованием камеры устройства.
   - Отображает результаты классификации на экране.
