# AI Server

지원사업 AI 검수 기능을 위한 Spring Boot 서버입니다.

## MariaDB 연결 설정

AI 서버는 기존 백엔드와 같은 `miniproject1` 데이터베이스를 사용합니다.
DB 비밀번호는 코드에 저장하지 않고 AI 서버 전용 `.env`에 입력합니다.
공고문 요약에 사용할 OpenAI 키도 같은 `.env`에 입력합니다.

```bash
cp .env.example .env
# .env의 DB_PASSWORD와 OPEN_AI_KEY에 로컬 값을 입력
```

## 실행

```bash
./gradlew bootRun
```

`bootRun`은 `.env`에서 DB와 OpenAI에 필요한 값만 자동으로 불러옵니다.

`.env`는 Git에 포함되지 않으며 팀원마다 자신의 로컬 값으로 만들어야 합니다.

## 확인 주소

- Health API: `http://localhost:8001/api/health`
- MariaDB Health API: `http://localhost:8001/api/health/database`
- 공고 단건 조회 API: `http://localhost:8001/api/programs/{pblancId}`
- 첨부파일 다운로드 API: `POST http://localhost:8001/api/documents/{pblancId}/download`
- PDF 파싱 및 저장 API: `POST http://localhost:8001/api/documents/{pblancId}/parse/pdf`
- HWP 파싱 및 저장 API: `POST http://localhost:8001/api/documents/{pblancId}/parse/hwp`
- HWPX 파싱 및 저장 API: `POST http://localhost:8001/api/documents/{pblancId}/parse/hwpx`
- Swagger UI: `http://localhost:8001/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8001/v3/api-docs`

파싱 원문은 `program_documents`에, 6개 AI 요약 항목은 `ai_summary`에 저장됩니다.

## 첨부파일 저장 위치

다운로드한 파일은 확장자와 공고 ID에 따라 다음 위치에 저장됩니다.

```text
downloads/{pdf|hwp|hwpx}/{pblancId}/{원본파일명}
```

`downloads/` 폴더는 Git에 포함되지 않습니다.
