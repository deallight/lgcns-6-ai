package com.miniproject1.ai.document;

import com.miniproject1.ai.program.ProgramResponse;
import com.miniproject1.ai.program.ProgramService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 기업마당 공고의 첨부파일을 다운로드하는 서비스입니다.
 */
@Service
public class DocumentService {

    private static final Path DOWNLOAD_ROOT = Path.of("downloads").toAbsolutePath().normalize();
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "hwp", "hwpx");
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Pattern ENCODED_FILE_NAME_PATTERN = Pattern.compile(
            "filename\\*=UTF-8''([^;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(
            "filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

    private final ProgramService programService;
    private final HttpClient httpClient;

    public DocumentService(ProgramService programService) {
        this.programService = programService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 공고 ID에 연결된 첨부파일 한 개를 확장자별 폴더에 저장합니다.
     */
    public DocumentDownloadResponse download(String pblancId) {
        validatePblancId(pblancId);

        ProgramResponse program = programService.findByPblancId(pblancId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 공고 ID입니다."));

        URI fileUri = validateFileUrl(program.printFilePathUrl());
        HttpRequest request = createRequest(fileUri, pblancId);

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != HttpStatus.OK.value()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "기업마당 첨부파일 다운로드에 실패했습니다.");
            }

            byte[] fileData = response.body();
            validateFileSize(fileData.length);

            String fileName = extractFileName(response, pblancId);
            String extension = extractAllowedExtension(fileName);
            Path savedFile = saveFile(pblancId, extension, fileName, fileData);

            return new DocumentDownloadResponse(
                    pblancId,
                    fileName,
                    extension,
                    Path.of("downloads").resolve(DOWNLOAD_ROOT.relativize(savedFile)).toString(),
                    fileData.length);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "첨부파일 다운로드가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "첨부파일 저장에 실패했습니다.", exception);
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
     * DB에 저장된 주소가 기업마당 HTTPS 주소인지 확인합니다.
     */
    private URI validateFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일 URL이 없습니다.");
        }

        try {
            URI fileUri = URI.create(fileUrl);
            boolean isAllowedUrl = "https".equalsIgnoreCase(fileUri.getScheme())
                    && "www.bizinfo.go.kr".equalsIgnoreCase(fileUri.getHost());

            if (!isAllowedUrl) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "허용되지 않은 첨부파일 URL입니다.");
            }
            return fileUri;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "올바르지 않은 첨부파일 URL입니다.", exception);
        }
    }

    /**
     * 기업마당에서 허용하는 기본 요청 헤더를 구성합니다.
     */
    private HttpRequest createRequest(URI fileUri, String pblancId) {
        String referer = "https://www.bizinfo.go.kr/sii/siia/selectSIIA200Detail.do?pblancId=" + pblancId;

        return HttpRequest.newBuilder(fileUri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", referer)
                .GET()
                .build();
    }

    /**
     * 너무 큰 파일이 메모리와 디스크를 차지하지 않도록 크기를 제한합니다.
     */
    private void validateFileSize(long fileSize) {
        if (fileSize == 0 || fileSize > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "다운로드 가능한 파일 크기를 초과했습니다.");
        }
    }

    /**
     * 응답 헤더에서 원본 파일명을 추출하고 경로 문자를 제거합니다.
     */
    private String extractFileName(HttpResponse<byte[]> response, String pblancId) {
        String disposition = response.headers()
                .firstValue("Content-Disposition")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "첨부파일 이름을 확인할 수 없습니다."));

        Matcher encodedMatcher = ENCODED_FILE_NAME_PATTERN.matcher(disposition);
        String fileName;
        if (encodedMatcher.find()) {
            fileName = URLDecoder.decode(encodedMatcher.group(1), StandardCharsets.UTF_8);
        } else {
            Matcher fileNameMatcher = FILE_NAME_PATTERN.matcher(disposition);
            if (!fileNameMatcher.find()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "첨부파일 이름을 확인할 수 없습니다.");
            }
            fileName = decodeUtf8Header(fileNameMatcher.group(1));
        }

        String safeFileName = fileName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();

        return safeFileName.isBlank() ? pblancId : safeFileName;
    }

    /**
     * UTF-8 파일명이 ISO-8859-1로 읽힌 경우 한글 파일명을 복원합니다.
     */
    private String decodeUtf8Header(String fileName) {
        boolean containsUnicode = fileName.chars().anyMatch(character -> character > 255);
        if (containsUnicode) {
            return fileName;
        }

        String decoded = new String(fileName.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return decoded.contains("�") ? fileName : decoded;
    }

    /**
     * PDF, HWP, HWPX 확장자만 다운로드 대상으로 허용합니다.
     */
    private String extractAllowedExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 첨부파일 형식입니다.");
        }

        String extension = fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 첨부파일 형식입니다.");
        }
        return extension;
    }

    /**
     * 확장자와 공고 ID로 폴더를 나누어 파일을 저장합니다.
     */
    private Path saveFile(
            String pblancId,
            String extension,
            String fileName,
            byte[] fileData) throws IOException {
        Path programDirectory = DOWNLOAD_ROOT.resolve(extension).resolve(pblancId).normalize();
        if (!programDirectory.startsWith(DOWNLOAD_ROOT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 저장 경로입니다.");
        }

        Files.createDirectories(programDirectory);
        Path savedFile = programDirectory.resolve(fileName).normalize();
        if (!savedFile.startsWith(programDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 파일명입니다.");
        }

        return Files.write(
                savedFile,
                fileData,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
}
