package com.miniproject1.ai.document.parser.hwpx;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 다운로드한 HWPX 파일의 문단과 표에서 텍스트를 추출하는 파서입니다.
 */
@Service
public class HwpxDocumentParser {

    private static final Path HWPX_ROOT = Path.of("downloads", "hwpx").toAbsolutePath().normalize();
    private static final long MAX_SECTION_SIZE = 20L * 1024 * 1024;
    private static final Pattern SECTION_PATH_PATTERN = Pattern.compile("Contents/section(\\d+)\\.xml");

    /**
     * 공고 ID 폴더에 있는 HWPX 한 개를 읽어 텍스트를 반환합니다.
     */
    public HwpxParseResponse parse(String pblancId) {
        validatePblancId(pblancId);
        Path hwpxFile = findHwpxFile(pblancId);

        try (ZipFile zipFile = new ZipFile(hwpxFile.toFile())) {
            List<ZipEntry> sectionEntries = findSectionEntries(zipFile);
            if (sectionEntries.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "HWPX 본문 XML을 찾을 수 없습니다.");
            }

            StringBuilder parsedText = new StringBuilder();
            for (int index = 0; index < sectionEntries.size(); index++) {
                ZipEntry sectionEntry = sectionEntries.get(index);
                validateSectionSize(sectionEntry);

                try (InputStream inputStream = zipFile.getInputStream(sectionEntry)) {
                    String sectionText = parseSection(inputStream);
                    if (!sectionText.isBlank()) {
                        parsedText.append("[SECTION ")
                                .append(index + 1)
                                .append("]\n")
                                .append(sectionText)
                                .append("\n\n");
                    }
                }
            }

            String text = parsedText.toString().trim();
            if (text.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "HWPX에서 텍스트를 추출할 수 없습니다.");
            }

            return new HwpxParseResponse(
                    pblancId,
                    hwpxFile.getFileName().toString(),
                    sectionEntries.size(),
                    text.length(),
                    text);
        } catch (IOException | XMLStreamException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "HWPX 파일을 읽을 수 없습니다.",
                    exception);
        }
    }

    /**
     * HWPX section XML을 순서대로 읽어 문단과 표를 구분합니다.
     */
    private String parseSection(InputStream inputStream) throws XMLStreamException {
        XMLInputFactory factory = createSecureXmlFactory();
        XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

        StringBuilder output = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();
        StringBuilder cell = null;
        List<String> rowCells = null;
        boolean readingText = false;
        int tableDepth = 0;

        try {
            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();

                    if ("tbl".equals(elementName)) {
                        if (tableDepth == 0) {
                            appendParagraph(output, paragraph);
                            output.append("[TABLE]\n");
                        }
                        tableDepth++;
                    } else if ("tr".equals(elementName) && tableDepth == 1) {
                        rowCells = new ArrayList<>();
                    } else if ("tc".equals(elementName) && tableDepth == 1) {
                        cell = new StringBuilder();
                    } else if ("t".equals(elementName)) {
                        readingText = true;
                    } else if ("lineBreak".equals(elementName)) {
                        appendToCurrentTarget(cell, paragraph, "\n");
                    } else if ("tab".equals(elementName)) {
                        appendToCurrentTarget(cell, paragraph, " ");
                    }
                } else if (event == XMLStreamConstants.CHARACTERS && readingText) {
                    appendToCurrentTarget(cell, paragraph, reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String elementName = reader.getLocalName();

                    if ("t".equals(elementName)) {
                        readingText = false;
                    } else if ("p".equals(elementName)) {
                        if (cell != null) {
                            cell.append(' ');
                        } else if (tableDepth == 0) {
                            appendParagraph(output, paragraph);
                        }
                    } else if ("tc".equals(elementName) && tableDepth == 1 && cell != null) {
                        if (rowCells != null) {
                            rowCells.add(cleanInlineText(cell.toString()));
                        }
                        cell = null;
                    } else if ("tr".equals(elementName) && tableDepth == 1 && rowCells != null) {
                        appendTableRow(output, rowCells);
                        rowCells = null;
                    } else if ("tbl".equals(elementName)) {
                        tableDepth--;
                        if (tableDepth == 0) {
                            output.append('\n');
                        }
                    }
                }
            }
        } finally {
            reader.close();
        }

        appendParagraph(output, paragraph);
        return output.toString()
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 외부 문서나 DTD를 읽지 않도록 XML 기능을 제한합니다.
     */
    private XMLInputFactory createSecureXmlFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    /**
     * 현재 표 셀 또는 일반 문단에 텍스트를 추가합니다.
     */
    private void appendToCurrentTarget(
            StringBuilder cell,
            StringBuilder paragraph,
            String text) {
        StringBuilder target = cell != null ? cell : paragraph;
        target.append(text);
    }

    /**
     * 일반 문단을 한 줄로 정리해 결과에 추가합니다.
     */
    private void appendParagraph(StringBuilder output, StringBuilder paragraph) {
        String paragraphText = cleanInlineText(paragraph.toString());
        if (!paragraphText.isBlank()) {
            output.append(paragraphText).append('\n');
        }
        paragraph.setLength(0);
    }

    /**
     * 표 한 행의 셀을 구분자로 연결합니다.
     */
    private void appendTableRow(StringBuilder output, List<String> rowCells) {
        boolean hasText = rowCells.stream().anyMatch(cell -> !cell.isBlank());
        if (hasText) {
            output.append(String.join(" | ", rowCells)).append('\n');
        }
    }

    /**
     * 문단과 표 셀 안의 연속된 공백을 정리합니다.
     */
    private String cleanInlineText(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * section XML만 문서 순서대로 찾습니다.
     */
    private List<ZipEntry> findSectionEntries(ZipFile zipFile) {
        return zipFile.stream()
                .map(ZipEntry.class::cast)
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> SECTION_PATH_PATTERN.matcher(entry.getName()).matches())
                .sorted(Comparator.comparingInt(entry -> extractSectionNumber(entry.getName())))
                .toList();
    }

    /**
     * section 파일명에서 정렬에 사용할 번호를 추출합니다.
     */
    private int extractSectionNumber(String entryName) {
        Matcher matcher = SECTION_PATH_PATTERN.matcher(entryName);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
    }

    /**
     * 비정상적으로 큰 XML 항목은 처리하지 않습니다.
     */
    private void validateSectionSize(ZipEntry sectionEntry) {
        long sectionSize = sectionEntry.getSize();
        if (sectionSize < 0 || sectionSize > MAX_SECTION_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "HWPX 본문 XML 크기를 확인할 수 없거나 제한을 초과했습니다.");
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
     * 해당 공고 폴더에서 HWPX 파일 한 개를 찾습니다.
     */
    private Path findHwpxFile(String pblancId) {
        Path programDirectory = HWPX_ROOT.resolve(pblancId).normalize();
        if (!programDirectory.startsWith(HWPX_ROOT) || !Files.isDirectory(programDirectory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "다운로드한 HWPX 파일이 없습니다.");
        }

        try (Stream<Path> files = Files.list(programDirectory)) {
            return files
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".hwpx"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "다운로드한 HWPX 파일이 없습니다."));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "HWPX 저장 폴더를 확인할 수 없습니다.",
                    exception);
        }
    }
}
