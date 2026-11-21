# Лаб 8: Mock объектууд — Mockito-гоор хамаарлыг орлуулж тестлэх

**F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)**

| | |
|---|---|
| **Оюутан** | Д.Алтанбагана |
| **Оюутны код** | B222270827 |

## Орчин

```
$ java -version
openjdk version "26.0.1" 2026-04-21
OpenJDK Runtime Environment (build 26.0.1+8-34)
OpenJDK 64-Bit Server VM (build 26.0.1+8-34, mixed mode, sharing)

$ mvn -version
Apache Maven 3.10.1 (3afd8084b5629f80b97750906a719879ed4cd283)
Maven home: /opt/homebrew/Cellar/maven/3.10.1/libexec
Java version: 27.0.1.1, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk/27.0.1.1/libexec/openjdk.jdk/Contents/Home
Default locale: en_MN, platform encoding: UTF-8, time zone: Asia/Ulaanbaatar
OS name: "mac os x", version: "26.6.2", arch: "aarch64", family: "mac"
```

Starter төслийг (Teams → Lab08) өөрчлөлтгүй ашигласан: JUnit 5.10.2, Mockito 5.11.0 (`mockito-junit-jupiter`), `maven.compiler.release=17`. JDK 27 дээр гардаг «A Java agent has been loaded dynamically (byte-buddy-agent)» анхааруулга нь Mockito-гийн inline mock maker-ийн хэвийн мессеж.

## Хэсэг A: Найман тест

`src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java` — `@ExtendWith(MockitoExtension.class)`, `@Mock StudentRepository`, `@Mock EmailSender`.

| № | Тест | Хэрэгсэл |
|---|---|---|
| A1 | `register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail` | `when` + `verify` |
| A2 | `register_unknownStudent_returnsErrorNoStudentAndNothingSaved` | `verify(..., never())` |
| A3 | `register_inactiveStudent_returnsErrorInactiveAndNoEmailInteraction` | `verifyNoInteractions` |
| A4 | `register_missingPrerequisite_returnsErrorPrerequisitesAndNotSaved` | `when` + `never` |
| A5 | `register_emailSenderThrows_stillReturnsOkAndSaves` | `doThrow(...).when(...)` |
| A6 | `register_nullStudentId_throwsIllegalArgumentAndNoInteractions` | `assertThrows` |
| A7 | `register_success_emailBodyContainsCourseCode` | `ArgumentCaptor` |
| A8 | `register_errorClasses_returnExpectedErrorAndNothingSavedOrSent` (3 мөр: MISSING / INACTIVE / ACTIVE+хичээлгүй) | `@ParameterizedTest` + `@CsvSource` |

Үр дүн (`results/mvn-test.txt`): **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS** (7 энгийн тест + A8-ийн 3 мөр).

Matcher-ийн дүрэм: нэг дуудалтад matcher хэрэглэсэн бол бүх аргументад хэрэглэсэн, жишээ нь `verify(emailSender).send(eq(EMAIL), anyString(), contains(COURSE_ID))`.

**A8-д `lenient()` яагаад хэрэгтэй вэ:** мөр бүр `findStudent` ба `courseExists` хоёуланг нь stub хийдэг ч MISSING ба INACTIVE мөрүүдэд сервис оюутны шалгалт дээр буцаж `courseExists`-ийг огт дууддаггүй. MockitoExtension-ий strict stubs горим ашиглагдаагүй stub-ийг `UnnecessaryStubbingException` гэж унагаадаг тул (`lenient()`-гүйгээр ажиллуулахад [1], [2] мөрүүд Errors: 2 өгсөн) нийтлэг stub-уудыг `lenient().when(...)`-ээр тэмдэглэсэн.
