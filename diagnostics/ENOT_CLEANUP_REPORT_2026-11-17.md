# СчётЕнот — отчёт об очистке, 2026-11-17

Очистка выполнена без Gradle-сборки, тестов и изменения Git. Работоспособность приложения на устройстве не подтверждалась.

## Метод и границы доказательства

Проверены все source sets app/src, Kotlin/Java основного кода и тестов, Manifest, Navigation, Gradle, ProGuard и все XML ресурсов. Граф объединяет конфигурации по паре тип/имя; корни — ссылки из исходников и Manifest, импортированные generated Binding classes, Navigation, конфигурационные XML, звуки и mipmap. Учтены XML include, ссылки внутри DataBinding, tools/Preview, selectors, родители стилей (явные и неявные). Обход транзитивный. Любые ссылки из существующего Kotlin сохранены, даже из потенциально неиспользуемых функций. В исходниках не найдены getIdentifier, Class.forName или механизмы построения имён ресурсов. Отсутствие текстового упоминания имени само по себе не использовалось как основание удаления.

Внешние Android/AndroidX/Material ресурсы не удалялись; разрешение библиотечных родителей стилей не проверялось сборкой. Классификация SAFE_DELETE относится к текущему исходному проекту; внешние потребители ресурсных имён вне репозитория статически не наблюдаемы.

## SAFE_DELETE — файлы

Удалено 14 файлов; суммарный исходный размер 3,064,641 байт (2.923 МиБ). Категории не пересекаются: XML: 4, PNG: 8, Vector: 1, Fonts: 1, Kotlin: 0, Other: 0.

- `app/src/main/res/color/ncs_v2_comparison_answer_text.xml` — 255 байт; SHA-256 `14845f9b7cdfc85f12471e4941bf078a0366d7f120e320f59354913255f8d6b1`
- `app/src/main/res/drawable/brain.png` — 354,072 байт; SHA-256 `4f880e993e405cb38ab4a73d0db016403e3af6439d8cbdaae1f975cad7562d3a`
- `app/src/main/res/drawable/cuub_1.png` — 224,900 байт; SHA-256 `99da9a6a0f57c54cfba94f0653e4acbf72f7a956fbd1c50f994745effee4a8b2`
- `app/src/main/res/drawable/girl_2.png` — 618,003 байт; SHA-256 `d42ca248c292c7cd7ecd606693da2a3e69618537b2a7e9c968c33f8777bf0fe3`
- `app/src/main/res/drawable/girl_qes.png` — 422,399 байт; SHA-256 `585aa051a57c1325ec11fb32951fe6c1efa1db036d72dff61c7fb2034f1071e6`
- `app/src/main/res/drawable/ic_brain.xml` — 7,322 байт; SHA-256 `ca7d52df4bc1eaf11a6f1095c1e32ddc00ec7cf9fb35f5fe2c8df1778fc7208f`
- `app/src/main/res/drawable/kids.png` — 450,006 байт; SHA-256 `38fcf02bcbd530115d7382a990b76f7fece8905dbea3e0252e469d26e16057b7`
- `app/src/main/res/drawable/ncs_v2_answer_incorrect.xml` — 346 байт; SHA-256 `2167f9f13babe0b22578d3e573ff481da9a15e0a39a11317ca1daef0fb7f4ca6`
- `app/src/main/res/drawable/ncs_v2_notebook_background.xml` — 323 байт; SHA-256 `fa0b27390c679a205fea0a90937dab9db9024ea60058d7d314248a2dbc1671dd`
- `app/src/main/res/drawable/ncs_v2_notebook_cell.png` — 656 байт; SHA-256 `b06119d3aaf979dbb8f4a7be39aa3ce364dddc8d9504e6843097a6efb7c62fc6`
- `app/src/main/res/drawable/owl_1.png` — 271,695 байт; SHA-256 `09690b8043d6d9db4cab11cd95006045c17dc493c22da25e3d8e489d660b9032`
- `app/src/main/res/drawable/tux_1.png` — 368,314 байт; SHA-256 `cf1f099f54df59cd534ec712827ce80679239c320c02f6833872dc4629583883`
- `app/src/main/res/font/ncs_v2_inter_black.ttf` — 344,820 байт; SHA-256 `f04a43cde09da7a0ec01526347dac368980098410321ab8edc9e5c61b9d1c45f`
- `app/src/main/res/values/themes_v2.xml` — 1,530 байт; SHA-256 `63ac65f9ba2d9d1fd7acc2409210761354c42ec0e118d5721a372ba7e33b06e9`

Для каждого удалённого ключа отсутствует достижимость от корней графа. Зависимость notebook_background → notebook_cell удалена целиком. Theme.CompositionOfNumbers.V2 не применяется ни Manifest, ни кодом, ни layout; TextAppearance.NcsV2.Body использовался только этой темой. Действующая Theme.CompositionOfNumbers сохранена.

## SAFE_DELETE — объявления values

Удалено 95 объявлений (включая тему в удалённом файле); 46 уникальных ключей. Дополнительное сокращение оставшихся values-файлов: 10,084 байт.

- `app/src/main/res/values/colors.xml`: `color/holo_blue_light`, `color/holo_green_dark`, `color/holo_green_light`, `color/holo_orange_dark`, `color/holo_red_dark`, `color/purple_200`.
- `app/src/main/res/values/colors_v2.xml`: `color/ncs_v2_comparison_correct_border`, `color/ncs_v2_comparison_expression_border`, `color/ncs_v2_hub_arithmetic_border`, `color/ncs_v2_hub_comparison_border`, `color/ncs_v2_hub_comparison_ink`, `color/ncs_v2_hub_comparison_surface`, `color/ncs_v2_hub_future_border`.
- `app/src/main/res/values/dimens_v2.xml`: `dimen/ncs_v2_answer_width`, `dimen/ncs_v2_content_width`, `dimen/ncs_v2_lesson_available_height`, `dimen/ncs_v2_notebook_cell_height`, `dimen/ncs_v2_notebook_cell_width`, `dimen/ncs_v2_progress_width`, `dimen/ncs_v2_radius_screen`, `dimen/ncs_v2_text_25`.
- `app/src/main/res/values/strings.xml`: `string/imgBack`, `string/ncs_v2_comparison_game_correct_explanation`, `string/ncs_v2_comparison_game_correct_expression_accessibility`, `string/ncs_v2_comparison_result_accuracy_met`, `string/ncs_v2_comparison_result_accuracy_missing`, `string/ncs_v2_comparison_result_failure_accuracy`, `string/ncs_v2_comparison_result_failure_count`, `string/ncs_v2_comparison_result_failure_description`, `string/ncs_v2_comparison_result_minimum_met`, `string/ncs_v2_comparison_result_success_accuracy`, `string/ncs_v2_comparison_result_success_count`, `string/ncs_v2_hub_arithmetic_symbols`, `string/ncs_v2_hub_comparison_symbols`, `string/right_answers`, `string/score_percentage`.
- `app/src/main/res/values/strings_v2.xml`: `string/ncs_v2_addends_explanation`, `string/ncs_v2_brand_subtitle`, `string/ncs_v2_comparison_brand_subtitle`, `string/ncs_v2_comparison_rules_speech`, `string/ncs_v2_game_time_preview`, `string/ncs_v2_level_and_range`, `string/ncs_v2_other_lessons_soon`, `string/ncs_v2_other_lessons_soon_available`.
- `app/src/main/res/values/styles_v2.xml`: `style/TextAppearance.NcsV2.Body`.
- `app/src/main/res/values/themes_v2.xml`: `style/Theme.CompositionOfNumbers.V2`.
- `app/src/main/res/values-en/strings.xml`: `string/imgBack`, `string/ncs_v2_addends_explanation`, `string/ncs_v2_brand_subtitle`, `string/ncs_v2_comparison_brand_subtitle`, `string/ncs_v2_comparison_game_correct_explanation`, `string/ncs_v2_comparison_game_correct_expression_accessibility`, `string/ncs_v2_comparison_result_accuracy_met`, `string/ncs_v2_comparison_result_accuracy_missing`, `string/ncs_v2_comparison_result_failure_description`, `string/ncs_v2_comparison_result_minimum_met`, `string/ncs_v2_comparison_rules_speech`, `string/ncs_v2_game_time_preview`, `string/ncs_v2_level_and_range`, `string/ncs_v2_other_lessons_soon`, `string/ncs_v2_other_lessons_soon_available`, `string/right_answers`, `string/score_percentage`.
- `app/src/main/res/values-kk/strings.xml`: `string/ncs_v2_addends_explanation`, `string/ncs_v2_brand_subtitle`, `string/ncs_v2_comparison_brand_subtitle`, `string/ncs_v2_comparison_game_correct_explanation`, `string/ncs_v2_comparison_game_correct_expression_accessibility`, `string/ncs_v2_comparison_result_accuracy_met`, `string/ncs_v2_comparison_result_accuracy_missing`, `string/ncs_v2_comparison_result_failure_description`, `string/ncs_v2_comparison_result_minimum_met`, `string/ncs_v2_comparison_rules_speech`, `string/ncs_v2_game_time_preview`, `string/ncs_v2_level_and_range`, `string/ncs_v2_other_lessons_soon`, `string/ncs_v2_other_lessons_soon_available`, `string/right_answers`, `string/score_percentage`.
- `app/src/main/res/values-uk/strings.xml`: `string/ncs_v2_addends_explanation`, `string/ncs_v2_brand_subtitle`, `string/ncs_v2_comparison_brand_subtitle`, `string/ncs_v2_comparison_game_correct_explanation`, `string/ncs_v2_comparison_game_correct_expression_accessibility`, `string/ncs_v2_comparison_result_accuracy_met`, `string/ncs_v2_comparison_result_accuracy_missing`, `string/ncs_v2_comparison_result_failure_description`, `string/ncs_v2_comparison_result_minimum_met`, `string/ncs_v2_comparison_rules_speech`, `string/ncs_v2_game_time_preview`, `string/ncs_v2_level_and_range`, `string/ncs_v2_other_lessons_soon`, `string/ncs_v2_other_lessons_soon_available`, `string/right_answers`, `string/score_percentage`.

Строки удалены одновременно во всех имеющихся локализациях. Для каждой локализации подтверждено: набор строк после = набор до минус только SAFE_DELETE; действующие переводы не менялись. Все варианты используемых размеров, цветов, шрифтов и стилей сохранены.

## KEEP

- Все 27 layout, включая 14 действующих макетов, состояния и include-компоненты: сохранены побайтно; Binding-импорты ведут к существующим layout.
- Все Nunito и Inter, кроме Inter Black: достижимы из действующих стилей/разметки.
- Все пять звуков, XML backup/data extraction и Navigation сохранены побайтно.
- Все UI-классы сохранены: Fragment/Activity связаны с Manifest/Navigation; вспомогательные функции вызываются кодом/Binding. WelcomeFragment — действующие правила сложения, несмотря на прежнее имя.
- Все UI-константы имеют обращения в своём файле; удалений не потребовалось. Domain, Data, ViewModel, генераторы, пороги, таймеры и результаты не менялись.
- Manifest по-прежнему использует ic_launcher/ic_launcher_round. Adaptive XML ссылаются на ic_launcher_foreground и ic_launcher_background. Все плотности Main и Release сохранены. Debug использует Main: собственных res overrides у Debug нет. logo_schetenot остаётся в UI; PNG иконок не переименовывались и не перекодировались. Визуальное соответствие всех растров логотипу не проверялось.

## UNCERTAIN — сохранено

- `app/src/main/res/drawable/ic_smile.png`, `ic_sad.png`: входящие ссылки из getSmileResId в BindingAdapter.kt. Атрибут resultEmoji сейчас не найден в layout, но удаление отдельных функций существующего файла выходит за разрешённое удаление полностью недостижимых UI-классов.
- `string/score_answers`, `string/required_percentage` во всех конфигурациях: ссылки из BindingAdapter.kt, хотя соответствующие binding-атрибуты сейчас не применены. Сохранены вместе с адаптерами. required_score используется действующим requiredAnswers.
- `app/src/{main,debug,release}/ic_launcher-playstore.png`: публикационные изображения вне res; отсутствие runtime-ссылок не доказывает ненужность для магазина.
- `app/build/`, `app/release/` и их артефакты: результаты сборки/публикации и сопутствующие файлы не удалялись; пользовательская потребность в них неизвестна.

## Точные дубликаты SHA-256

Ни одна группа не удалена автоматически. Совпадения исходных файлов app (без build):

- `app/src/debug/ic_launcher-playstore.png`; `app/src/main/ic_launcher-playstore.png`; `app/src/release/ic_launcher-playstore.png`.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`; `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`.
- `app/src/main/res/values/ic_launcher_background.xml`; `app/src/release/res/values/ic_launcher_background.xml`.
- `app/src/release/res/mipmap-anydpi-v26/ic_launcher.xml`; `app/src/release/res/mipmap-anydpi-v26/ic_launcher_round.xml`.

Play Store изображения сохранены как публикационные артефакты; adaptive round/обычные XML нужны под разными именами; цвет фона сохраняется в Main/Release overrides.

## Статические проверки

- До: 596 ресурсных ключей, 86 XML ресурсов. После: 537 ключей, 81 XML ресурсов. Все XML разобраны парсером.
- Не найдено новых или оставшихся неразрешённых ссылок на локальные ресурсы в графе; входящие ссылки на удалённые ключи в оставшихся ресурсах отсутствуют.
- Все layout побайтно неизменны: ID, DataBinding expressions/variables и include сохранены. Все импортированные layout Binding classes сопоставлены с существующими файлами.
- Kotlin/Java, тесты, Manifest, Navigation, Gradle и ProGuard побайтно неизменны; маршруты и lifecycle-классы сохранены.
- Все не затронутые ресурсы побайтно неизменны; все исходные Handoff, figma_assets, ENOT_IMPLEMENTATION_REPORT и остальные корневые файлы сохранены. PROJECT_LOG обновлён только добавлением записи после проверки.
- Наборы локализованных строк сопоставлены до/после для values, values-en, values-kk, values-uk. Полнота действующих переводов сохранена; исправление ранее отсутствовавших переводов не входило в очистку.
- Остаточных недостижимых ключей по консервативной модели графа нет. Ресурсы Kotlin-функций, чья достижимость не доказана, сохраняются (см. UNCERTAIN).
- Удалённые пустые каталоги: `app/src/debug/res/mipmap-anydpi-v26`, `app/src/debug/res/mipmap-hdpi`, `app/src/debug/res/mipmap-mdpi`, `app/src/debug/res/mipmap-xhdpi`, `app/src/debug/res/mipmap-xxhdpi`, `app/src/debug/res/mipmap-xxxhdpi`, `app/src/debug/res/values`, `app/src/main/res/color`, `app/src/main/res/drawable-v24`, `app/src/debug/res`.

## Ограничения и следующий шаг

Сборка и тесты намеренно не запускались по заданию. Статическая проверка не заменяет AAPT, генерацию DataBinding, lint, R8 и проверку интерфейса на устройстве. Проект подготовлен к сборке владельцем; успешная сборка и работоспособность не заявляются. Удаление ограничено app/src; Git-очистка, commit, push, reset, checkout и смена ветки не выполнялись.
