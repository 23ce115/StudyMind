package com.studymind.backend.service;

import com.studymind.backend.exception.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
    private static final int MAX_PAGES = 800;

    /** @return the text of each page (index 0 = page 1). Plain .txt/.md files are split into pseudo-pages. */
    public List<String> extractPages(byte[] bytes, String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return splitPlain(new String(bytes, StandardCharsets.UTF_8));
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            int n = Math.min(doc.getNumberOfPages(), MAX_PAGES);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<String> pages = new ArrayList<>(n);
            for (int i = 1; i <= n; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                pages.add(stripper.getText(doc));
            }
            return pages;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Couldn't read that PDF. Is it password-protected or damaged?");
        }
    }

    private static List<String> splitPlain(String text) {
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < text.length(); i += 3000) pages.add(text.substring(i, Math.min(text.length(), i + 3000)));
        return pages;
    }
}
