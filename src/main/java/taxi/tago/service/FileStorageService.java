package taxi.tago.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${image.upload.path:uploads/library-cards}")
    private String uploadPath;

    // 이미지 파일을 저장하고 저장된 파일 경로를 반환
    public String saveImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 업로드 디렉토리 생성 (상대 경로 또는 절대 경로 모두 지원)
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // 파일 저장
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("이미지 파일 저장 완료: {}", filePath.toString());

        // 절대 경로로 반환 (경로 제한 없이 모든 경로 지원)
        return filePath.toAbsolutePath().toString();
    }

    // 파일 경로로부터 파일을 읽어서 byte 배열로 반환 (상대 경로, 절대 경로 모두 지원)
    public byte[] loadImageFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IOException("파일 경로가 비어있습니다.");
        }

        // 상대 경로와 절대 경로 모두 처리
        Path path = Paths.get(filePath);
        
        // 상대 경로인 경우 절대 경로로 변환 시도
        if (!path.isAbsolute()) {
            // 먼저 상대 경로로 시도
            if (!Files.exists(path)) {
                // 상대 경로가 없으면 기본 업로드 경로 기준으로 시도
                Path absolutePath = Paths.get(uploadPath).resolve(filePath).normalize();
                if (Files.exists(absolutePath)) {
                    path = absolutePath;
                }
            }
        }
        
        if (!Files.exists(path)) {
            throw new IOException("파일을 찾을 수 없습니다: " + filePath);
        }
        
        return Files.readAllBytes(path);
    }
}

