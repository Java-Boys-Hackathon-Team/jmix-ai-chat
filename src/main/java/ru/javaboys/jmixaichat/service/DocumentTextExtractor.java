package ru.javaboys.jmixaichat.service;

import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DocumentTextExtractor {
    private final FileStorageLocator fileStorageLocator;

    public DocumentTextExtractor(FileStorageLocator fileStorageLocator) {
        this.fileStorageLocator = fileStorageLocator;
    }

    public String extract(FileRef fileRef) {
        FileStorage fileStorage = fileStorageLocator.getByName(fileRef.getStorageName());
        try (InputStream inputStream = fileStorage.openStream(fileRef)) {
            return extract(inputStream, fileRef.getFileName());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read uploaded file", e);
        }
    }

    public String extract(InputStream inputStream, @Nullable String originalName) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();

            if (originalName != null) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, originalName);
            }

            parser.parse(inputStream, handler, metadata, new ParseContext());
            return handler.toString();
        } catch (IOException | TikaException | SAXException e) {
            throw new IllegalStateException("Unable to extract text from document", e);
        }
    }
}
