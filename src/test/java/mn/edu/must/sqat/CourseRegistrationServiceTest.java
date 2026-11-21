package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "B222270827";
    private static final String COURSE_ID = "CS313";
    private static final String EMAIL = "b222270827@must.edu.mn";

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private static Student student(boolean active, List<String> coursesTaken) {
        return new Student(STUDENT_ID, EMAIL, active, coursesTaken);
    }

    // A1 — when + verify
    @Test
    void register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of("CS201")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), contains(COURSE_ID));
    }

    // A2 — verify(..., never())
    @Test
    void register_unknownStudent_returnsErrorNoStudentAndNothingSaved() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3 — verifyNoInteractions
    @Test
    void register_inactiveStudent_returnsErrorInactiveAndNoEmailInteraction() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(false, List.of("CS201")));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
    }

    // A4 — when + never
    @Test
    void register_missingPrerequisite_returnsErrorPrerequisitesAndNotSaved() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A5 — doThrow(...).when(...)
    @Test
    void register_emailSenderThrows_stillReturnsOkAndSaves() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of("CS201")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));
        doThrow(new RuntimeException("SMTP сервер ажиллахгүй"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
    }

    // A6 — assertThrows
    @Test
    void register_nullStudentId_throwsIllegalArgumentAndNoInteractions() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));

        verifyNoInteractions(repository, emailSender);
    }

    // A7 — ArgumentCaptor
    @Test
    void register_success_emailBodyContainsCourseCode() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of("CS201")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

        service.register(STUDENT_ID, COURSE_ID);

        verify(emailSender).send(eq(EMAIL), eq("Бүртгэл баталгаажлаа"), body.capture());
        assertTrue(body.getValue().contains(COURSE_ID),
                "И-мэйлийн биед хичээлийн код байх ёстой: " + body.getValue());
    }

    // A8 — @ParameterizedTest + @CsvSource (3 алдааны анги)
    @ParameterizedTest(name = "{0} оюутан, хичээл байгаа={1} → {2}")
    @CsvSource({
            "MISSING,  true,  ERROR_NO_STUDENT",
            "INACTIVE, true,  ERROR_INACTIVE_STUDENT",
            "ACTIVE,   false, ERROR_NO_COURSE"
    })
    void register_errorClasses_returnExpectedErrorAndNothingSavedOrSent(
            String studentState, boolean courseExists, String expected) {
        Student s = switch (studentState) {
            case "MISSING" -> null;
            case "INACTIVE" -> student(false, List.of());
            default -> student(true, List.of());
        };
        // Мөр бүрд эдгээр stub-ийн зөвхөн нэг хэсэг нь ашиглагдана — strict stubs-аас зайлсхийх.
        lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(s);
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(courseExists);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
