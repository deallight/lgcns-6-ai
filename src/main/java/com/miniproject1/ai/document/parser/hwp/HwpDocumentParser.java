package com.miniproject1.ai.document.parser.hwp;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 다운로드한 HWP 파일의 본문과 표 안에서 텍스트를 추출하는 파서입니다.
 */
@Service
public class HwpDocumentParser {

    private static final Path HWP_ROOT = Path.of("downloads", "hwp").toAbsolutePath().normalize();
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    /**
     * 공고 ID 폴더에 있는 HWP 한 개를 읽어 텍스트를 반환합니다.
     */
    public HwpParseResponse parse(String pblancId) {
        validatePblancId(pblancId);
        Path hwpFilePath = findHwpFile(pblancId);
        validateFileSize(hwpFilePath);

        try {
            HWPFile hwpFile = HWPReader.fromFile(hwpFilePath.toFile());
            String text = extractAndCleanText(hwpFile);

            if (text.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "HWP에서 텍스트를 추출할 수 없습니다.");
            }

            return new HwpParseResponse(
                    pblancId,
                    hwpFilePath.getFileName().toString(),
                    hwpFile.getBodyText().getSectionList().size(),
                    text.length(),
                    text);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "HWP 파일을 읽을 수 없습니다. 암호화되었거나 손상된 파일일 수 있습니다.",
                    exception);
        }
    }

    /**
     * 표 같은 컨트롤의 텍스트를 본문 사이에 포함해 추출합니다.
     */
    private String extractAndCleanText(HWPFile hwpFile) throws UnsupportedEncodingException {
        String bodyText = cleanText(TextExtractor.extract(
                hwpFile,
                TextExtractMethod.InsertControlTextBetweenParagraphText));
        String tableText = extractTables(hwpFile);

        if (tableText.isBlank()) {
            return bodyText;
        }

        // 본문 뒤에 셀 구분이 살아 있는 표를 한 번 더 제공해 GPT가 조건 관계를 이해하게 합니다.
        return bodyText + "\n\n[STRUCTURED TABLES]\n" + tableText;
    }

    /**
     * 문서 안의 표를 행 단위로 읽고 각 셀을 | 문자로 구분합니다.
     */
    private String extractTables(HWPFile hwpFile) throws UnsupportedEncodingException {
        StringBuilder tables = new StringBuilder();
        int tableNumber = 1;

        for (Section section : hwpFile.getBodyText().getSectionList()) {
            for (Paragraph paragraph : section.getParagraphs()) {
                if (paragraph.getControlList() == null) {
                    continue;
                }

                for (Control control : paragraph.getControlList()) {
                    if (!(control instanceof ControlTable controlTable)) {
                        continue;
                    }

                    String tableText = extractTableRows(controlTable);
                    if (!tableText.isBlank()) {
                        tables.append("[TABLE ")
                                .append(tableNumber++)
                                .append("]\n")
                                .append(tableText)
                                .append("\n\n");
                    }
                }
            }
        }

        return tables.toString().trim();
    }

    /**
     * 표 한 개의 셀 내용을 행별로 정리합니다.
     */
    private String extractTableRows(ControlTable controlTable) throws UnsupportedEncodingException {
        StringBuilder tableText = new StringBuilder();

        for (Row row : controlTable.getRowList()) {
            StringBuilder rowText = new StringBuilder();
            boolean hasText = false;

            for (Cell cell : row.getCellList()) {
                String cellText = cleanCellText(cell.getParagraphList().getNormalString());
                if (rowText.length() > 0) {
                    rowText.append(" | ");
                }
                rowText.append(cellText);
                hasText |= !cellText.isBlank();
            }

            if (hasText) {
                tableText.append(rowText).append('\n');
            }
        }

        return tableText.toString().trim();
    }

    /**
     * 셀 안의 여러 문단은 한 줄로 합쳐 표의 행 구조를 유지합니다.
     */
    private String cleanCellText(String text) {
        return cleanText(text)
                .replaceAll("\\s*\\n\\s*", " ")
                .trim();
    }

    /**
     * 문단 구조는 유지하면서 GPT 분석에 방해되는 제어문자와 반복 공백을 정리합니다.
     */
    private String cleanText(String text) {
        String normalizedText = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "");

        return normalizedText.lines()
                .map(line -> line.replace('\t', ' '))
                .map(line -> line.replaceAll(" {2,}", " ").trim())
                .collect(Collectors.joining("\n"))
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 비정상적으로 큰 파일은 메모리를 과도하게 사용할 수 있어 처리하지 않습니다.
     */
    private void validateFileSize(Path hwpFilePath) {
        try {
            long fileSize = Files.size(hwpFilePath);
            if (fileSize == 0 || fileSize > MAX_FILE_SIZE) {
                throw new ResponseStatusException(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "파싱 가능한 HWP 파일 크기를 초과했습니다.");
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "HWP 파일 크기를 확인할 수 없습니다.",
                    exception);
        }
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
     * 해당 공고 폴더에서 HWP 파일 한 개를 찾습니다.
     */
    private Path findHwpFile(String pblancId) {
        Path programDirectory = HWP_ROOT.resolve(pblancId).normalize();
        if (!programDirectory.startsWith(HWP_ROOT) || !Files.isDirectory(programDirectory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "다운로드한 HWP 파일이 없습니다.");
        }

        try (Stream<Path> files = Files.list(programDirectory)) {
            return files
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".hwp"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "다운로드한 HWP 파일이 없습니다."));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "HWP 저장 폴더를 확인할 수 없습니다.",
                    exception);
        }
    }
}
