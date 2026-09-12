package ru.javaboys.jmixaichat.service;

import io.jmix.core.FileRef;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import ru.javaboys.jmixaichat.dto.ResumeFormData;
import ru.javaboys.jmixaichat.dto.ResumeSeniority;

import java.util.List;

@Service
public class ResumeStructuredOutputService {
    private static final String RESUME_EXTRACTION_SYSTEM_PROMPT = """
            Ты HR-ассистент. Извлеки данные из резюме и верни структурированный объект ResumeFormData.

            Правила:
            - Заполняй только те поля, которые действительно есть в документе.
            - Если значение не найдено, верни null.
            - experience верни списком мест работы: company, position, period, description.
            - education верни списком образований: place, degree, period.
            - summary сделай коротким: 1-2 предложения для карточки кандидата.
            """;

    private static final String RESUME_EXTRACTION_USER_PROMPT_TEMPLATE = """
            Файл: %s

            Текст резюме:
            ----------------
            %s
            """;

    private static final String SKILLS_EXTRACTION_SYSTEM_PROMPT = """
            Ты HR-ассистент. Найди в резюме только профессиональные навыки.
            Верни результат как JSON-массив строк верхнего уровня.
            Не возвращай объект с полем skills.
            Не добавляй markdown, пояснения и группировку.

            Пример правильного ответа:
            ["Java", "Spring Boot", "PostgreSQL"]
            """;

    private static final String SENIORITY_CLASSIFICATION_SYSTEM_PROMPT = """
            Ты HR-ассистент. Оцени уровень кандидата по резюме.
            Верни только одно enum-значение: INTERN, JUNIOR, MIDDLE, SENIOR, LEAD или UNKNOWN.
            """;

    private final ChatClient chatClient;
    private final DocumentTextExtractor documentTextExtractor;

    public ResumeStructuredOutputService(ChatClient chatClient, DocumentTextExtractor documentTextExtractor) {
        this.chatClient = chatClient;
        this.documentTextExtractor = documentTextExtractor;
    }

    public ResumeFormData extractFromDocument(FileRef fileRef) {
        String documentText = documentTextExtractor.extract(fileRef);

        ResumeFormData formData = extractResumeFormData(fileRef.getFileName(), documentText);
        formData.setSkills(extractSkills(documentText));
        formData.setSeniority(classifySeniority(documentText));

        return formData;
    }

    private ResumeFormData extractResumeFormData(String fileName, String documentText) {
        ResumeFormData data = chatClient.prompt()
                .system(RESUME_EXTRACTION_SYSTEM_PROMPT)
                .user(RESUME_EXTRACTION_USER_PROMPT_TEMPLATE.formatted(fileName, documentText))
                .call()
                .entity(ResumeFormData.class);

        return data == null ? new ResumeFormData() : data;
    }

    private List<String> extractSkills(String documentText) {
        return chatClient.prompt()
                .system(SKILLS_EXTRACTION_SYSTEM_PROMPT)
                .user(documentText)
                .call()
                .entity(new ParameterizedTypeReference<List<String>>() {
                });
    }

    private ResumeSeniority classifySeniority(String documentText) {
        ResumeSeniority seniority = chatClient.prompt()
                .system(SENIORITY_CLASSIFICATION_SYSTEM_PROMPT)
                .user(documentText)
                .call()
                .entity(ResumeSeniority.class);

        return seniority == null ? ResumeSeniority.UNKNOWN : seniority;
    }
}
