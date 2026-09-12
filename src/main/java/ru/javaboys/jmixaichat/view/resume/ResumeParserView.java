package ru.javaboys.jmixaichat.view.resume;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import io.jmix.core.FileRef;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.upload.FileStorageUploadField;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.javaboys.jmixaichat.dto.ResumeFormData;
import ru.javaboys.jmixaichat.service.ResumeStructuredOutputService;
import ru.javaboys.jmixaichat.view.main.MainView;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Route(value = "resume-parser", layout = MainView.class)
@ViewController("ResumeParserView")
@ViewDescriptor(path = "resume-parser-view.xml")
public class ResumeParserView extends StandardView {
    @ViewComponent
    private FileStorageUploadField resumeUpload;
    @ViewComponent
    private TypedTextField<String> fullNameField;
    @ViewComponent
    private TypedTextField<String> positionField;
    @ViewComponent
    private TypedTextField<String> seniorityField;
    @ViewComponent
    private TypedTextField<String> emailField;
    @ViewComponent
    private TypedTextField<String> phoneField;
    @ViewComponent
    private TypedTextField<String> cityField;
    @ViewComponent
    private TextArea summaryField;
    @ViewComponent
    private TextArea skillsField;
    @ViewComponent
    private TextArea experienceField;
    @ViewComponent
    private TextArea educationField;

    @Autowired
    private ResumeStructuredOutputService resumeStructuredOutputService;
    @Autowired
    private Notifications notifications;

    private FileRef uploadedFileRef;

    @Subscribe("resumeUpload")
    public void onResumeUpload(AbstractField.ComponentValueChangeEvent<FileStorageUploadField, FileRef> event) {
        uploadedFileRef = event.getValue();

        if (uploadedFileRef != null) {
            parseUploadedResume();
        }
    }

    private void parseUploadedResume() {
        try {
            ResumeFormData data = resumeStructuredOutputService.extractFromDocument(uploadedFileRef);
            fillForm(data == null ? new ResumeFormData() : data);
            notifications.create("Форма заполнена structured output")
                    .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                    .show();
        } catch (RuntimeException e) {
            notifications.create("Не удалось разобрать резюме: " + e.getMessage())
                    .withThemeVariant(NotificationVariant.LUMO_ERROR)
                    .show();
        }
    }

    private void fillForm(ResumeFormData data) {
        fullNameField.setValue(valueOrEmpty(data.getFullName()));
        positionField.setValue(valueOrEmpty(data.getPosition()));
        seniorityField.setValue(data.getSeniority() == null ? "" : data.getSeniority().name());
        emailField.setValue(valueOrEmpty(data.getEmail()));
        phoneField.setValue(valueOrEmpty(data.getPhone()));
        cityField.setValue(valueOrEmpty(data.getCity()));
        summaryField.setValue(valueOrEmpty(data.getSummary()));
        skillsField.setValue(formatStrings(data.getSkills()));
        experienceField.setValue(formatExperience(data.getExperience()));
        educationField.setValue(formatEducation(data.getEducation()));
    }

    private static String formatStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private static String formatExperience(List<ResumeFormData.ExperienceItem> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(item -> joinParts(
                        item.getCompany(),
                        item.getPosition(),
                        item.getPeriod(),
                        item.getDescription()))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private static String formatEducation(List<ResumeFormData.EducationItem> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(item -> joinParts(item.getPlace(), item.getDegree(), item.getPeriod()))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private static String joinParts(String... parts) {
        return Arrays.stream(parts)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" - "));
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
