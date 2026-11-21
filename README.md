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
| A6 | `register_nullStudentIdOrCourseId_throwsIllegalArgumentAndNoInteractions` | `assertThrows` (×2) |
| A7 | `register_success_emailBodyContainsCourseCode` | `ArgumentCaptor` |
| A8 | `register_errorClasses_returnExpectedErrorAndNothingSavedOrSent` (3 мөр: MISSING / INACTIVE / ACTIVE+хичээлгүй) | `@ParameterizedTest` + `@CsvSource` |

Үр дүн (`results/mvn-test.txt`): **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS** (7 энгийн тест + A8-ийн 3 мөр).

Matcher-ийн дүрэм: нэг дуудалтад matcher хэрэглэсэн бол бүх аргументад хэрэглэсэн, жишээ нь `verify(emailSender).send(eq(EMAIL), anyString(), contains(COURSE_ID))`.

**A8-д `lenient()` яагаад хэрэгтэй вэ:** мөр бүр `findStudent` ба `courseExists` хоёуланг нь stub хийдэг ч MISSING ба INACTIVE мөрүүдэд сервис оюутны шалгалт дээр буцаж `courseExists`-ийг огт дууддаггүй. MockitoExtension-ий strict stubs горим ашиглагдаагүй stub-ийг `UnnecessaryStubbingException` гэж унагаадаг тул (`lenient()`-гүйгээр ажиллуулахад [1], [2] мөрүүд Errors: 2 өгсөн) нийтлэг stub-уудыг `lenient().when(...)`-ээр тэмдэглэсэн.

## Хэсэг B: Тестээ тестлэх — мини мутаци

Мутант бүрийн дараа `CourseRegistrationService.java`-г эх хувилбарт нь сэргээсэн. `src/main` репод өөрчлөгдөөгүй.

| Мутант | Эвдрэл | Унасан тест | Үр дүн | Файл |
|---|---|---|---|---|
| 1 | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";`-г комментолсон | A3 `register_inactiveStudent_…`, A8 [2] INACTIVE | Failures: 2, BUILD FAILURE | `results/mvn-test-mutant-1.txt` |
| 2 | `repository.saveRegistration(...)`-г комментолсон | A1 `register_activeStudentWithPrerequisites_…`, A5 `register_emailSenderThrows_…` | Failures: 2, BUILD FAILURE | `results/mvn-test-mutant-2.txt` |
| 3 | `if (studentId == null \|\| courseId == null)` → `if (studentId == null)` | **эхэндээ нэг ч тест унаагүй** → A6-г засаад: A6 `register_nullStudentIdOrCourseId_…` | Failures: 1, BUILD FAILURE | `results/mvn-test-mutant-3.txt` (засварын өмнөх амьд үлдсэн гаралт: `results/mvn-test-mutant-3-survived.txt`) |

**Мутант 3 ба A6-гийн засвар:** анхны A6 зөвхөн `register(null, "CS313")`-ийг шалгадаг байсан тул `courseId`-ийн null шалгалтыг устгахад Tests run: 10, Failures: 0, BUILD SUCCESS гарч мутант амьд үлдсэн — энэ бол тестийн цоорхой. A6-д `assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null))` гэсэн хоёр дахь шалгалт нэмж, нэрийг `register_nullStudentIdOrCourseId_…` болгосны дараа мутант баригдсан. Тестийн тоо 10 хэвээр.

**Ажиглалт:** мутант 1-ийн үед A3 нь `ERROR_INACTIVE_STUDENT` биш `ERROR_NO_COURSE` авсан (идэвхгүй оюутан цааш үргэлжилж, stub хийгээгүй `courseExists` нь Mockito-гийн анхдагч `false` буцаасан). Мутант 2-т A7 (ArgumentCaptor) унаагүй, учир нь тэр зөвхөн и-мэйлийн биеийг шалгадаг — хадгалалтыг A1, A5 л баталдаг. Мөн мутант 1-ийн үед A8 [2] мөр `OK` авсан: lenient `courseExists=true` байсан бөгөөд `prerequisites` stub хийгдээгүй тул Mockito хоосон жагсаалт буцааж, идэвхгүй оюутан бүх шалгалтыг давж бүртгэгдсэн. Аль ч мутантад `UnnecessaryStubbingException` (Errors) гараагүй, бүгд Failures хэлбэрээр унасан.

## Дүгнэлт

Mock нь Mockito-гоор ажиллах үед үүсдэг, юу буцаахыг `when(...)`-ээр тест бүрд зааж, дуудагдсан эсэхийг `verify`-ээр шалгаж болдог объект бол fake нь интерфейсийг гараар хэрэгжүүлсэн, жишээ нь `HashMap` дээр ажилладаг жижиг `StudentRepository` юм. Fake нь бодит логиктой тул олон тестэд дахин ашиглахад тохиромжтой ч хадгалалт дуудагдсан эсэх, и-мэйлийн бие яг юу байсан гэх мэт харилцан үйлчлэлийг шалгахад mock илүү хялбар байсан. `Student` record, `List` зэрэг энгийн утга бүхий объектыг mock хийх ёсгүй, бодитоор нь үүсгэх нь илүү ойлгомжтой; мөн тестлэгдэж буй класс өөрийгөө (`CourseRegistrationService`) mock хийвэл юу ч шалгахгүй. Мутант 3 нэг ч тестэд баригдаагүй нь тест ногоон байгаа нь бүрэн гэсэн үг биш, хил хязгаар бүрийг (энд хоёр аргументын null) тусад нь шалгах хэрэгтэйг харуулсан. Мутант 1-ийн үед Mockito-гийн анхдагч утга (`false`, хоосон жагсаалт) идэвхгүй оюутныг `OK` болгосон нь stub хийгээгүй дуудлага чимээгүйгээр «зөв» утга буцааж болзошгүйг анхааруулсан. `verify`-г хэтрүүлж бүх дуудлагын дараалал, тоог нарийн шалгавал код зөв ажиллаж байхад л дотоод хэрэгжүүлэлт өөрчлөгдөхөд (refactoring) тест унаж эмзэг болно. Иймээс би гадагш харагдах үр дагавартай дуудлагыг л (`saveRegistration`, `send`) verify хийж, бусдыг нь буцаах утгаар нь шалгахыг зорьсон. Strict stubs горим ашиглагдаагүй stub-ийг илрүүлдэг нь тестийг цэвэр байлгахад тустай боловч параметржүүлсэн тестэд `lenient()` шаардлагатай болгосон.
