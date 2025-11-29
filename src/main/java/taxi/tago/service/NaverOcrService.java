package taxi.tago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverOcrService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${naver.ocr.api-key}")
    private String apiKey;

    @Value("${naver.ocr.url}")
    private String ocrUrl;

    /**
     * 이미지를 네이버 OCR API로 전송하여 텍스트 추출
     * @param imageBytes 이미지 바이트 배열
     * @return OCR 결과 JSON
     */
    public OcrResult extractText(byte[] imageBytes) {
        try {
            // Base64 인코딩
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 네이버 OCR API 표준 요청 형식에 맞춰 요청 바디 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("version", "V2");
            requestBody.put("requestId", UUID.randomUUID().toString());
            requestBody.put("timestamp", System.currentTimeMillis());
            
            // 이미지 정보 (네이버 OCR API 표준 형식)
            Map<String, Object> image = new HashMap<>();
            image.put("format", "jpg");  // 이미지 형식
            image.put("name", "library_card");  // 이미지 이름
            image.put("data", base64Image);  // Base64 인코딩된 이미지 데이터
            image.put("url", null);  // URL 사용 안 함
            
            List<Map<String, Object>> images = new ArrayList<>();
            images.add(image);
            requestBody.put("images", images);

            // 헤더 설정 (네이버 OCR API 표준)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", apiKey);  // OCR Secret Key

            // 요청 엔티티 생성
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            log.debug("네이버 OCR API 호출 - URL: {}, RequestId: {}", ocrUrl, requestBody.get("requestId"));

            // API 호출 (POST 메서드로 /general 엔드포인트에 요청)
            ResponseEntity<String> response = restTemplate.exchange(
                    ocrUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseOcrResponse(response.getBody());
            } else {
                log.error("OCR API 호출 실패: {}", response.getStatusCode());
                throw new RuntimeException("OCR API 호출에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("OCR 처리 중 오류 발생", e);
            throw new RuntimeException("이미지 인식에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // OCR 응답을 파싱하여 이름과 학번 추출
    private OcrResult parseOcrResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode images = root.path("images");
            
            if (!images.isArray() || images.size() == 0) {
                throw new RuntimeException("OCR 결과에서 이미지 정보를 찾을 수 없습니다.");
            }

            JsonNode imageNode = images.get(0);
            JsonNode fields = imageNode.path("fields");

            String name = null;
            String studentId = null;
            StringBuilder fullText = new StringBuilder();

            // OCR 결과에서 모든 텍스트 추출
            for (JsonNode field : fields) {
                String inferText = field.path("inferText").asText("");
                fullText.append(inferText).append(" ");

                // 이름 패턴 찾기 (예: "이름:", "Name:", "이름 " 등)
                if (inferText.matches(".*이름[:：]?\\s*([가-힣]+).*")) {
                    name = inferText.replaceAll(".*이름[:：]?\\s*([가-힣]+).*", "$1").trim();
                } else if (inferText.matches(".*Name[:：]?\\s*([가-힣]+).*")) {
                    name = inferText.replaceAll(".*Name[:：]?\\s*([가-힣]+).*", "$1").trim();
                }

                // 학번 패턴 찾기 (예: "학번:", "Student ID:", "2021111222" 등)
                if (inferText.matches(".*학번[:：]?\\s*(\\d+).*")) {
                    studentId = inferText.replaceAll(".*학번[:：]?\\s*(\\d+).*", "$1").trim();
                } else if (inferText.matches("(\\d{10})")) { // 10자리 숫자만 있는 경우
                    studentId = inferText.trim();
                }
            }

            // 추가로 전체 텍스트에서 패턴 검색
            String text = fullText.toString();
            if (name == null) {
                Pattern namePattern = Pattern.compile("이름[:：]?\\s*([가-힣]{2,4})");
                Matcher nameMatcher = namePattern.matcher(text);
                if (nameMatcher.find()) {
                    name = nameMatcher.group(1).trim();
                }
            }

            if (studentId == null) {
                Pattern idPattern = Pattern.compile("학번[:：]?\\s*(\\d{10})");
                Matcher idMatcher = idPattern.matcher(text);
                if (idMatcher.find()) {
                    studentId = idMatcher.group(1).trim();
                } else {
                    // 10자리 숫자 패턴 직접 검색
                    Pattern directIdPattern = Pattern.compile("(\\d{10})");
                    Matcher directIdMatcher = directIdPattern.matcher(text);
                    if (directIdMatcher.find()) {
                        studentId = directIdMatcher.group(1).trim();
                    }
                }
            }

            return new OcrResult(name, studentId, text.trim());

        } catch (Exception e) {
            log.error("OCR 응답 파싱 중 오류 발생", e);
            throw new RuntimeException("OCR 결과를 분석하는 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // OCR 결과를 담는 클래스
    public static class OcrResult {
        private final String name;
        private final String studentId;
        private final String fullText;

        public OcrResult(String name, String studentId, String fullText) {
            this.name = name;
            this.studentId = studentId;
            this.fullText = fullText;
        }

        public String getName() {
            return name;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getFullText() {
            return fullText;
        }
    }
}

