# 🥐 MOONGCHIJANG 
> **개발 기간**: 2026.04.30 ~
<img width="1102" height="618" alt="스크린샷 2026-06-04 오후 11 49 19" src="https://github.com/user-attachments/assets/35efae1b-848f-4196-a494-870d47b10a9a" />

## 🙌🏻 프로젝트 소개

**기다림은 없애고 좋아하는 빵은 더 가깝게!**</br>
moongchijang은 베이커리 웨이팅이 싫은 빵 덕후들이 원하는 매장의 빵을 목표 수량 달성 조건으로 함께 모아 공동구매하고, 지정 픽업일에 웨이팅 없이 직접 수령할 수 있는 **베이커리 단체구매 중개 서비스**입니다.

</br>
</br>

## 👥 팀원 소개

### Backend 
| <img src="https://github.com/eun-seoo.png?size=120" width="120"/> | ![](https://github.com/JoonKyoLee.png?size=120) |
|:---:|:---:|
|[김은서](https://github.com/eun-seoo)|[이준교](https://github.com/JoonKyoLee)|
|BE 파트장|BE|

</br>
</br>

## 🌐 시스템 아키텍처 

<img width="917" height="443" alt="KakaoTalk_Photo_2026-06-04-23-52-37" src="https://github.com/user-attachments/assets/4e252a45-d60f-47d6-ba1d-27c66fbb1374" />

</br>
</br>

## ⚙️ 기술 스택

| 분류        | 기술 스택                                 |
| --------- | ------------------------------------- |
| 언어        | Kotlin                                |
| 프레임워크     | Spring Boot 4.0.5                     |
| 데이터 접근    | Spring Data JPA, Querydsl             |
| 검색        | N-Gram, Naver Local Search API                                |
| 인증/인가     | JWT Authentication, OAuth 2.0 (Kakao) |
| 캐시/동시성 처리 | Redis                                 |
| 데이터베이스    | MySQL                                 |
| DB 마이그레이션 | Flyway                                |
| 인프라 관리    | Terraform                             |
| 컨테이너 환경   | Docker, Docker Compose                |
| 모니터링      | Prometheus, Grafana                   |

</br>
</br>

## ✨ 주요 기능

  ### 🔐 인증 및 사용자 관리

  - **카카오 로그인**: Kakao OAuth 기반 소셜 로그인
  - **이메일 회원가입/로그인**: 이메일 인증 코드를 통한 회원가입 및 로그인
  - **JWT 기반 인증**: Access Token 재발급, 로그아웃 처리
  - **휴대폰 인증**: 회원가입 및 내 정보 변경 시 휴대폰 인증 코드 발송/검증
  - **회원 정보 관리**: 내 정보 조회, 닉네임/전화번호/비밀번호 변경
  - **역할 전환**: 구매자/판매자 역할 전환
  - **판매자 정보 관리**: 사업자 정보, 정산 계좌, 판매자 프로필 등록/수정
  - **회원 탈퇴**: 구매자/판매자 탈퇴 및 법적 보존 데이터 처리

  ### 🛒 공동구매 관리

  - **공동구매 목록 조회**: 공동구매 피드 조회 및 정렬
  - **공동구매 상세 조회**: 상품 정보, 참여 현황, 진행 상태 조회
  - **공동구매 공유 정보**: 공유용 공동구매 데이터 제공
  - **진행률 조회**: 개별/전체 공동구매 진행률 확인
  - **실시간 조회자 관리**: Redis 기반 조회자 heartbeat 처리
  - **공동구매 요청**: 사용자의 공동구매 개설 요청 생성/조회
  - **공동구매 개설 요청**: 원하는 매장/상품에 대한 개설 요청 등록
  - **매장 추천 요청**: 공동구매 개설을 위한 매장 추천

  ### ❤️ 찜/위시리스트

  - **공동구매 찜하기**: 관심 공동구매를 위시리스트에 추가
  - **찜 취소**: 등록된 공동구매 찜 해제
  - **찜 목록 조회**: 로그인 사용자 기준 위시리스트 조회

  ### 💳 결제 및 참여 관리

  - **결제 정보 조회**: 공동구매 참여 전 checkout 정보 제공
  - **결제 주문 생성**: PortOne 결제를 위한 결제 주문 생성
  - **결제 완료 처리**: PortOne 결제 완료 요청 검증 및 참여 확정
  - **웹훅 처리**: PortOne 결제 웹훅 수신 및 결제 상태 동기화
  - **참여 취소**: 공동구매 참여 취소 및 환불 흐름 연동
  - **결제 감사 로그**: 결제 상태 변경 이력 기록
  - **환불 스케줄러**: 대기 중인 환불 건 자동 처리

  ### 📦 픽업 관리

  - **픽업 정보 조회**: 참여 건별 픽업 상태 및 정보 조회
  - **QR 코드 조회**: 공동구매 픽업용 QR 정보 제공
  - **가장 가까운 픽업 QR 조회**: 사용자 기준 픽업 대기 건 확인
  - **픽업 검증**: QR 코드 기반 픽업 완료 처리


  ### 🔔 알림 기능

  - **알림 목록 조회**: 사용자별 알림 리스트 조회
  - **읽음 처리**: 단일 알림 읽음 처리 및 전체 읽음 처리
  - **안 읽은 알림 수 조회**: 미확인 알림 카운트 제공
  - **알림 템플릿**: 상황별 알림 메시지 렌더링
  - **알림톡 연동**: 픽업 리마인드 등 외부 알림톡 발송
  - **디스코드 알림**: 관리자용 운영 알림 전송
  - **스케줄 기반 알림**: 예약된 알림 트리거 처리

  ### 🔍 검색 및 가게 조회

  - **통합 검색**: 공동구매/가게 추천 검색
  - **검색어 보정**: 오타 및 유사 검색어 보정
  - **최근 검색어 관리**: 사용자별 최근 검색어 조회/삭제
  - **가게 검색**: Naver Local Search API 기반 가게 검색
  - **추천 가게 이미지 관리**: 추천 가게 이미지 조회/관리


</br>
</br>

## 📁 프로젝트 구조

  ```text
  moongchijang-BE/
  ├── src/
  │   ├── main/
  │   │   ├── kotlin/com/moongchijang/
  │   │   │   ├── MoongchijangApplication.kt       # Spring Boot 애플리케이션 진입점
  │   │   │   ├── global/                          # 전역 공통 모듈
  │   │   │   │   ├── config/                      # Jackson, S3, CORS, Querydsl, SMTP, OpenAPI 설정
  │   │   │   │   ├── entity/                      # BaseEntity 등 공통 엔티티
  │   │   │   │   ├── exception/                   # 커스텀 예외, 에러 코드
  │   │   │   │   ├── handler/                     # 전역 예외 핸들러
  │   │   │   │   ├── health/                      # 헬스 체크 API
  │   │   │   │   ├── response/                    # 공통 API 응답
  │   │   │   │   └── util/                        # 공통 유틸리티
  │   │   │   ├── security/                        # 보안 및 인증/인가
  │   │   │   │   ├── authorization/               # 역할 기반 권한 AOP
  │   │   │   │   ├── config/                      # Spring Security 설정
  │   │   │   │   ├── crypto/                      # 개인정보 암호화/해싱
  │   │   │   │   ├── jwt/                         # JWT 필터, 토큰 Provider
  │   │   │   │   └── principal/                   # 인증 사용자 Principal
  │   │   │   └── domain/                          # 비즈니스 도메인
  │   │   │       ├── admin/                       # 관리자: 대시보드, 주문, 환불, 정산, CS 관리
  │   │   │       ├── auth/                        # 로그인, 회원가입, 카카오 OAuth, 이메일/휴대폰 인증
  │   │   │       ├── csticket/                    # 고객센터 티켓
  │   │   │       ├── favorite/                    # 찜/위시리스트
  │   │   │       ├── groupbuy/                    # 공동구매, 공동구매 요청, 오픈 요청
  │   │   │       ├── image/                       # 이미지 업로드/삭제
  │   │   │       ├── mypage/                      # 마이페이지, 내 참여 내역
  │   │   │       ├── notification/                # 알림, 알림톡, 디스코드 알림
  │   │   │       ├── owner/                       # 점주 공동구매 관리, 점주 요청, 정산
  │   │   │       ├── participation/               # 공동구매 참여, 취소, 참여 상태 관리
  │   │   │       ├── payment/                     # 결제 주문, PortOne 결제/웹훅, 결제 로그
  │   │   │       ├── pickup/                      # 픽업 QR/토큰, 픽업 처리
  │   │   │       ├── refund/                      # 환불 요청
  │   │   │       ├── search/                      # 통합 검색, 검색 보정
  │   │   │       ├── store/                       # 가게 검색, 추천 가게, Naver 로컬 검색
  │   │   │       └── user/                        # 회원, 역할, 판매자 정보, 탈퇴/개인정보 처리
  │   │   └── resources/
  │   │       ├── application.properties           # 공통 설정
  │   │       ├── application-local.yml            # 로컬 환경 설정
  │   │       ├── application-local-demo.yml       # 로컬 데모 환경 설정
  │   │       ├── application-dev.yml              # 개발 환경 설정
  │   │       ├── application-prod.yml             # 운영 환경 설정
  │   │       ├── db/migration/                    # Flyway DB 마이그레이션
  │   │       └── static/                          # OpenAPI 문서, 목업 이미지
  │   └── test/
  │       ├── kotlin/com/moongchijang/             # 테스트 코드
  │       └── resources/                           # 테스트 설정 및 테스트 데이터
  ├── docker/                                      # Docker 및 로컬 실행 환경
  │   ├── Dockerfile
  │   ├── docker-compose.yml
  │   ├── nginx/                                  # Nginx 설정
  │   └── monitoring/                             # Prometheus, Grafana, Alertmanager
  ├── infra/
  │   └── terraform/                              # Terraform 인프라 설정
  ├── docs/                                       # 개발/운영 문서, 시드 SQL
  ├── .github/
  │   ├── workflows/                              # CI/CD, Jira 연동 워크플로우
  │   └── ISSUE_TEMPLATE/                         # GitHub 이슈 템플릿
  ├── build.gradle.kts                            # Gradle 빌드 설정
  ├── settings.gradle.kts                         # Gradle 프로젝트 설정
  ├── gradlew                                     # Gradle Wrapper
  └── README.md
  ```

</br>
</br>

## 🗄️ ERD

<img width="3977" height="3060" alt="뭉치장 ERD v3" src="https://github.com/user-attachments/assets/099f989f-30d8-4d15-9024-d16d6e996502" />

