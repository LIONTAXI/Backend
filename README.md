### 멋사대학 13기 취업트랙 - SWULab
# 🚕 슈슝 swushoong - Backend
택시팟 모집부터 정산 관리까지 한번에, 서울여자대학교 학우들의 택시 동승 매칭 서비스 슈슝의 백엔드 레포지토리입니다.

---
## 🔗 배포 링크
[**swushoong_BE**](https://swushoong.click/) / [**swushoong_FE**](https://frontend-lac-nine-20.vercel.app/)

---
## 📚 Tech Stack

### **Backend**
- Java 17
- Spring Boot 3
- Spring Data JPA
- Spring Security (JWT)
- MySQL 8
- WebSocket (STOMP)

### **Infra / DevOps**
- AWS EC2
- GitHub Actions (CI/CD)
- Nginx Reverse Proxy

---
## 📂 Project Structure
```bash
tago-backend
├── build.gradle
├── settings.gradle
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── taxi.tago
    │   │       ├── TagoApplication.java            # Spring Boot 메인 클래스
    │   │       ├── config                          # 보안, Swagger, WebSocket, 메일 등 설정
    │   │       ├── constant                        # ENUM 상수 (TaxiPartyStatus, UserRole 등)
    │   │       ├── controller                      # REST API 컨트롤러
    │   │       ├── dto                             # 요청/응답 DTO 모음
    │   │       ├── entity                          # 도메인 모델(Entity)
    │   │       │   ├── User, TaxiParty, TaxiUser
    │   │       │   ├── ChatRoom, ChatMessage
    │   │       │   ├── Review, Notification
    │   │       │   └── Settlement, SettlementParticipant
    │   │       ├── exception                       # 글로벌 예외 처리
    │   │       ├── repository                      # Spring Data JPA Repositories
    │   │       ├── security                        # JWT 인증/인가
    │   │       ├── service                         # 핵심 비즈니스 로직
    │   │       │   ├── TaxiPartyService, ChatRoomService
    │   │       │   ├── EmailAuthService, NotificationService
    │   │       │   ├── ReviewService, SettlementService
    │   │       │   └── UserService
    │   │       └── util                            # JWT Util, SSE Manager 등
    │   └── resources
    │       ├── application.properties              # 로컬 환경 설정
    │       └── ws-test.html                        # WebSocket 테스트용 파일
    └── test
        └── TagoApplicationTests.java
```
---
## 🏛 Domain Overview (주요 도메인)
### **User**

- 학번, 이메일 인증, JWT 기반 로그인
- 위치 정보 업데이트(lat/lng)
- 프로필 이미지

### **TaxiParty (택시팟)**

- 출발지, 목적지, 예상 가격, 모집 인원
- 매칭 상태(MATCHING / FINISHED)
- 랜덤 이모지 마커 생성
- 총대슈니 / 동승슈니 관리

### **TaxiUser (동승 관계)**

- WAITING / ACCEPTED / KICKED
- 신청 → 승인 → 인원 증가 로직

### **ChatRoom / ChatMessage**

- WebSocket 기반 실시간 그룹채팅
- SYSTEM 메시지 & TEXT 메시지 구분
- 채팅방 종료(close) 기능

### **Settlement (정산)**

- 총대가 입력한 정산 금액/계좌 정보
- 참여자별 금액 분할
- 납부 상태(PAID) 업데이트
- 재촉 메시지(2시간 제한)

### **Review**

- 긍정/부정 태그 기반 후기 시스템
- 택시팟 종료 후 작성 가능

### **Notification**

- 이메일 & 알림 전송
- 참여요청, 승인, 정산요청 등 Event 기반 알림

---
## 🌐 Architecture Overview
``` bash
[Client App]
        ↓ HTTPS(443)
[Nginx Reverse Proxy]
        ↓ Proxy pass → 8080
[Spring Boot Application] 
        ↓
     [MySQL]
```

---
## ⚙️ CI/CD 자동 배포 파이프라인

슈슝 백엔드는 GitHub Actions 기반 CI/CD로 운영하며, **main 브랜치에 머지/푸시될 때마다 자동으로 빌드 → EC2 배포 → 서비스 재기동**까지 이어지도록 구성했습니다.

### ✅ 파이프라인 흐름
- **GitHub Actions 트리거**
  - `main` 브랜치에 `push` 발생 시 자동 실행
- **Gradle 빌드 & 테스트**
  - JDK 17 환경에서 `./gradlew clean build`
  - 테스트는 프로젝트 정책에 따라 실행/스킵 가능
- **배포 산출물 전송**
  - 생성된 `*.jar` 파일을 EC2로 전송 (SCP)
- **EC2에서 서비스 반영**
  - 기존 프로세스 종료 또는 `systemd` 재시작
  - `systemctl status`, `journalctl -u ...` 등을 통해 정상 기동 확인

### 🔐 GitHub Secrets (필수)
CI/CD 워크플로우에서 아래 값들을  
`GitHub Repository → Settings → Secrets and variables → Actions`에 등록합니다.

- **EC2_HOST**: EC2 Public IPv4 / 도메인
- **EC2_USER**: `ubuntu`
- **EC2_SSH_KEY**: `pem` 키 전체 내용 (멀티라인 그대로)
- **EC2_PORT**: 보통 `22`
- **(선택) APP_DIR**: `/home/ubuntu/swushoong/app` 같은 배포 디렉토리
- **(선택) SERVICE_NAME**: `swushoong` (systemd 서비스명)

### 🧩 운영 방식 포인트
- **CI에서 빌드 후 산출물(JAR)만 배포**해 서버 환경 이슈를 최소화했습니다.
- 서버 실행은 `nohup` 대신 **systemd 서비스**로 관리하여, 재부팅/장애 상황에서도 자동 복구가 가능하도록 했습니다.

---
## 🤖 Backend(Spring Boot) CI/CD 자동화

### 📦 빌드 기준
- **Java 17** (Amazon Corretto 17 / Temurin 17 호환)
- **Gradle 기반 빌드**
- **빌드 산출물**: `build/libs/*.jar`

### 🚀 배포 구조 예시
EC2 내부에서 백엔드 파일을 아래와 같이 관리합니다.

```bash
/home/ubuntu/swushoong/
 ├─ app/
 │   ├─ application.yml (또는 application-prod.yml)
 │   └─ swushoong.jar
 └─ logs/ 
```

### 🧷 systemd 서비스 예시 운영
- **서비스 이름**: `swushoong.service`
- **배포 시 수행되는 핵심 작업**
  - 최신 `jar`로 교체
  - 필요 시 `sudo systemctl daemon-reload`
  - `sudo systemctl restart swushoong`
  - `sudo systemctl status swushoong`

### 🔎 배포/장애 확인 커맨드
```bash
sudo systemctl status swushoong
sudo journalctl -u swushoong -n 200 --no-pager
sudo journalctl -u swushoong -f
```

---
## 🔒 HTTPS / SSL

슈슝은 사용자 로그인/토큰(JWT) 및 개인정보가 오가는 서비스이므로, 운영 환경에서는 **HTTPS(SSL/TLS)**를 필수로 적용했습니다.  
EC2 앞단에 **Nginx Reverse Proxy**를 두고, 외부는 **443(HTTPS)**로 받고 내부 Spring Boot는 **8080**으로 프록시합니다.

### 🧭 전체 구조
```
Client (Browser)  
   ↓ HTTPS :443  
Nginx (EC2)  
   ├─ SSL Termination (Let’s Encrypt)  
   └─ Reverse Proxy → `http://localhost:8080` (Spring Boot)
```

### 🔑 인증서 발급 방식
- **Let’s Encrypt + Certbot** 사용
- 도메인 기반으로 인증서 발급 후 **자동 갱신** 설정

### ♻️ 자동 갱신
Let’s Encrypt 인증서는 유효기간이 짧기 때문에 **자동 갱신**을 구성합니다.

### 🧱 Nginx 핵심 설정 요약
- **80 → 443 리다이렉트**
- `location /api/**` 요청은 Spring Boot로 프록시
- 필요 시 CORS / 업로드 용량 / 타임아웃 등을 Nginx에서 보완

개념 :
- `listen 80;` 에서는 HTTPS로 강제 이동
- `listen 443 ssl;` 에서 인증서 적용 + 프록시 설정

### 🔐 운영 보안 체크리스트
- **보안그룹 인바운드**
  - `22 (SSH)`: 팀/본인 IP만 허용 권장
  - `80, 443`: `0.0.0.0/0` 허용
  - `8080`: 외부 오픈 금지 (Nginx 통해서만 접근)
- **Spring Boot 설정**
  - 운영 환경에서는 `application-prod.yml` + 환경변수로 민감정보 분리
- **도메인(DNS) 설정**
  - 호스팅 레코드에서 **A 레코드가 EC2 IP를 가리키도록** 설정
---
## 👥 Backend Contributors
- 서울여대 멋사 13기 소프트웨어융합학과 24학번 우예빈
- 서울여대 멋사 13기 문헌정보학과 22학번 양보윤
- 서울여대 멋사 13기 디지털미디어학과 22학번 이다겸
