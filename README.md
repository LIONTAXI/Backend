### 멋사대학 13기 취업트랙 - swuLab
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

---
## 🤖 Backend(Spring Boot) CI/CD 자동화

---
## 🔒 HTTPS / SSL
- 

---
## 👥 Backend Contributors
- 서울여대 멋사 13기 소프트웨어융합학과 24학번 우예빈
- 서울여대 멋사 13기 문헌정보학과 22학번 양보윤
- 서울여대 멋사 13기 디지털미디어학과 22학번 이다겸
