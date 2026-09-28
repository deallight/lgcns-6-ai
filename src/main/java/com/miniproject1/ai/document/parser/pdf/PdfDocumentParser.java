package com.miniproject1.ai.document.parser.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 다운로드한 PDF 파일에서 텍스트를 추출하는 파서입니다.
 */
@Service
public class PdfDocumentParser {

    private static final Path PDF_ROOT = Path.of("downloads", "pdf").toAbsolutePath().normalize();

    /**
     * 공고 ID 폴더에 있는 PDF 한 개를 읽어 텍스트를 반환합니다.
     */
    public PdfParseResponse parse(String pblancId) {
        validatePblancId(pblancId);
        Path pdfFile = findPdfFile(pblancId);

        try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
            String text = extractTextByPage(document);

            if (text.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "PDF에서 텍스트를 추출할 수 없습니다. 이미지 PDF일 수 있습니다.");
            }

            return new PdfParseResponse(
                    pblancId,
                    pdfFile.getFileName().toString(),
                    document.getNumberOfPages(),
                    text.length(),
                    text);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PDF 파일을 읽을 수 없습니다.",
                    exception);
        }
    }

    /**
     * 글자 위치 순서로 페이지별 텍스트를 추출하고 페이지 표시를 추가합니다.
     */
    private String extractTextByPage(PDDocument document) throws IOException {
        LayoutPdfTextStripper textStripper = new LayoutPdfTextStripper();

        StringBuilder parsedText = new StringBuilder();
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            textStripper.setStartPage(page);
            textStripper.setEndPage(page);

            String pageText = cleanText(textStripper.getText(document));
            if (!pageText.isBlank()) {
                parsedText.append("[PAGE ")
                        .append(page)
                        .append("]\n")
                        .append(pageText)
                        .append("\n\n");
            }
        }

        return parsedText.toString().trim();
    }

    /**
     * 줄 구조는 유지하면서 불필요한 공백과 빈 줄만 정리합니다.
     */
    private String cleanText(String text) {
        String normalizedText = text
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        return normalizedText.lines()
                .map(String::stripTrailing)
                .map(line -> line.replaceAll("(?<=\\S) {3,}(?=\\S)", " | "))
                .collect(Collectors.joining("\n"))
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 경로 조작을 막기 위해 공고 ID에 안전한 문자만 허용합니다.
     */
    private void validatePblancId(String pblancId) {
        if (pblancId == null || !pblancId.matches("[A-Za-z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 공고 ID입니다.");
        }
    }

    /**
     * 해당 공고 폴더에서 PDF 파일 한 개를 찾습니다.
     */
    private Path findPdfFile(String pblancId) {
        Path programDirectory = PDF_ROOT.resolve(pblancId).normalize();
        if (!programDirectory.startsWith(PDF_ROOT) || !Files.isDirectory(programDirectory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "다운로드한 PDF 파일이 없습니다.");
        }

        try (Stream<Path> files = Files.list(programDirectory)) {
            return files
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "다운로드한 PDF 파일이 없습니다."));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF 저장 폴더를 확인할 수 없습니다.",
                    exception);
        }
    }
}
