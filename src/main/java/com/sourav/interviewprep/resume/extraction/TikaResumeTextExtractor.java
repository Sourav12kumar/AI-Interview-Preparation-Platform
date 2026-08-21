package com.sourav.interviewprep.resume.extraction;

import com.sourav.interviewprep.resume.config.ResumeProperties;
import com.sourav.interviewprep.resume.exception.ResumeValidationException;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteLimitReachedException;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Map;

@Component
public class TikaResumeTextExtractor implements ResumeTextExtractor {

    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "txt", "text/plain");

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();
    private final int characterLimit;

    public TikaResumeTextExtractor(ResumeProperties properties) {
        this.characterLimit = properties.maxExtractedCharacters();
    }

    @Override
    public ExtractedResume extract(String filename, byte[] content) {
        String extension = extension(filename);
        String expectedType = SUPPORTED_TYPES.get(extension);
        if (expectedType == null) {
            throw new ResumeValidationException("Only PDF, DOCX, and TXT resumes are supported");
        }

        try {
            String detectedType = tika.detect(content, filename);
            if (!expectedType.equals(detectedType)) {
                throw new ResumeValidationException("Resume content does not match its file extension");
            }

            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
            BodyContentHandler handler = new BodyContentHandler(characterLimit);
            try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
                try {
                    parser.parse(input, handler, metadata, new ParseContext());
                } catch (SAXException exception) {
                    if (!WriteLimitReachedException.isWriteLimitReached(exception)) {
                        throw exception;
                    }
                }
            }
            String text = normalize(handler.toString());
            if (text.isBlank()) {
                throw new ResumeValidationException("No readable text was found in the resume");
            }
            return new ExtractedResume(detectedType, text);
        } catch (ResumeValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResumeValidationException("Resume text could not be extracted", exception);
        }
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value.replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
