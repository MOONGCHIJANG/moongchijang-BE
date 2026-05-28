# Email Provider 전환 가이드 (SES ↔ GOOGLE SMTP)

## 목적
- SES 승인 지연 또는 발송 이슈 상황에서 `EMAIL_PROVIDER` 설정만으로 발송 경로를 전환합니다.
- 이메일 인증/안내 메일 중단 없이 운영을 지속합니다.

## 설정 키
- `EMAIL_PROVIDER`: `SES` 또는 `GOOGLE`
- `SES_ENABLED`: `true`/`false`
- `SES_REGION`
- `SES_FROM_EMAIL`
- `GOOGLE_SMTP_HOST` (기본: `smtp.gmail.com`)
- `GOOGLE_SMTP_PORT` (기본: `587`)
- `GOOGLE_SMTP_USERNAME`
- `GOOGLE_SMTP_APP_PASSWORD`
- `GOOGLE_SMTP_FROM`

## 권장 운영값
- 기본 운영: `EMAIL_PROVIDER=SES`, `SES_ENABLED=true`
- 긴급 우회: `EMAIL_PROVIDER=GOOGLE`

## GOOGLE SMTP 사전 준비
1. 발신 계정에 2단계 인증(2FA)을 활성화합니다.
2. Google 앱 비밀번호(16자리)를 생성합니다.
3. `GOOGLE_SMTP_APP_PASSWORD`에는 공백 없이 붙여서 입력합니다.
4. `GOOGLE_SMTP_FROM`은 발신 계정 주소와 동일하게 설정합니다.

## 전환 절차 (SES -> GOOGLE)
1. 운영 환경변수를 아래처럼 변경합니다.
   - `EMAIL_PROVIDER=GOOGLE`
   - `GOOGLE_SMTP_USERNAME=noreply.moongchijang@gmail.com`
   - `GOOGLE_SMTP_APP_PASSWORD=<16자리 앱 비밀번호>`
   - `GOOGLE_SMTP_FROM=noreply.moongchijang@gmail.com`
2. 애플리케이션을 재배포합니다.
3. 이메일 인증코드 발송 API로 실제 발송을 점검합니다.
4. 서버 로그에서 `EmailVerificationService`의 provider 값이 `GOOGLE`인지 확인합니다.

## 롤백 절차 (GOOGLE -> SES)
1. 운영 환경변수를 아래처럼 되돌립니다.
   - `EMAIL_PROVIDER=SES`
   - `SES_ENABLED=true`
2. 애플리케이션을 재배포합니다.
3. 이메일 인증코드 발송 API로 재점검합니다.
4. 서버 로그에서 provider 값이 `SES`인지 확인합니다.

## 장애 대응 체크리스트
- 인증 실패: 앱 비밀번호 오입력 여부 확인
- 발신 실패: `GOOGLE_SMTP_FROM`/`GOOGLE_SMTP_USERNAME` 불일치 여부 확인
- 발송 지연/차단: Gmail 일일 발송량 제한 여부 확인
- 즉시 복구 필요: `EMAIL_PROVIDER=SES`로 즉시 롤백
