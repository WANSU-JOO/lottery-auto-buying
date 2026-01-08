# 🎰 로또 6/45 자동 구매 시스템

> Java Spring Boot와 Selenium을 활용한 동행복권 자동 구매 템플릿  
> **별도 서버 없이 GitHub Actions를 활용하여 완전 무료로 구동됩니다!**

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Selenium](https://img.shields.io/badge/Selenium-4.27.0-green.svg)](https://www.selenium.dev/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📖 프로젝트 소개

이 프로젝트는 **Java Spring Boot**와 **Selenium WebDriver**를 활용하여 동행복권 사이트에서 자동으로 로또 6/45를 구매하는 오픈소스 템플릿입니다.

### ✨ 핵심 특징

- 🆓 **완전 무료**: GitHub Actions의 무료 플랜으로 별도 서버 없이 구동
- 🤖 **완전 자동화**: 설정만 완료하면 자동으로 로또 구매
- 📱 **실시간 알림**: Telegram 봇을 통한 구매 결과 알림
- 👥 **멀티 계정 지원**: 가족 계정 등 여러 계정을 독립적으로 운영 가능
- 🔒 **보안 강화**: GitHub Secrets를 통한 안전한 개인정보 관리

## 🚀 주요 기능

### ✅ 자동 구매
- 매주 **토요일 오전** 자동으로 로또 6/45 구매 (5게임, 5,000원)
- Cron 스케줄링을 통한 정확한 시간 실행
- GitHub Actions에서 자동 실행

### 📱 Telegram 알림
- ✅ **구매 성공**: "✅ 로또 5,000원 구매 완료! (잔액: OOO원)"
- 🚨 **구매 실패**: "🚨 구매 실패: [사유]"
- ⚠️ **잔액 부족**: "⚠️ 잔액 부족 알림"
- ❌ **시스템 오류**: "❌ 로또 자동 구매 오류 발생"

### 👨‍👩‍👧‍👦 멀티 계정 지원
- Workflow 파일을 분리하여 여러 계정 독립 운영
- 각 계정별로 별도의 Telegram 알림 수신 가능

## 📋 사전 요구사항

- GitHub 계정
- 동행복권 계정 (예치금 충전 필요)
- Telegram 계정 (알림 수신용)

## 🛠️ 설치 및 설정 방법

### Step 1: 저장소 복제하기

1. 이 저장소 페이지 상단의 **"Use this template"** 버튼을 클릭합니다.
2. 저장소 이름을 입력하고 **반드시 Private으로 설정**합니다.
3. **"Create repository from template"** 버튼을 클릭합니다.

> ⚠️ **중요**: 보안을 위해 반드시 **Private 저장소**로 생성하세요!

### Step 2: Telegram 봇 생성 및 Chat ID 확인

#### 2-1. Telegram 봇 생성

1. Telegram에서 [@BotFather](https://t.me/botfather)를 검색하여 대화를 시작합니다.
2. `/newbot` 명령어를 입력합니다.
3. 봇 이름을 입력합니다 (예: "로또 자동 구매 봇").
4. 봇 사용자 이름을 입력합니다 (예: "my_lotto_bot").
5. BotFather가 제공하는 **Bot Token**을 복사해둡니다.
   ```
   예시: 1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
   ```

#### 2-2. Chat ID 확인

1. 생성한 봇과 대화를 시작합니다 (Telegram에서 봇을 검색하여 `/start` 입력).
2. 브라우저에서 아래 URL을 열어 Chat ID를 확인합니다:
   ```
   https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
   ```
   `<YOUR_BOT_TOKEN>` 부분을 위에서 받은 Bot Token으로 교체하세요.
3. 응답 JSON에서 `"chat":{"id":123456789}` 형태의 숫자를 찾습니다.
4. 이 숫자가 **Chat ID**입니다.

> 💡 **팁**: 봇과 메시지를 주고받은 후에 `getUpdates`를 호출하면 더 쉽게 찾을 수 있습니다.

### Step 3: GitHub Secrets 설정

1. 생성한 저장소에서 **Settings** > **Secrets and variables** > **Actions**로 이동합니다.
2. **"New repository secret"** 버튼을 클릭합니다.
3. 아래 4개의 Secret를 각각 추가합니다:

| Secret 이름 | 설명 | 예시 |
|------------|------|------|
| `LOTTO_ID` | 동행복권 로그인 아이디 | `my_lotto_id` |
| `LOTTO_PW` | 동행복권 로그인 비밀번호 | `my_password123` |
| `TELEGRAM_TOKEN` | Telegram Bot Token | `1234567890:ABCdef...` |
| `TELEGRAM_CHAT_ID` | Telegram Chat ID | `123456789` |

> 📝 **참고**: 
> - `LOTTO_ID` 대신 `LOTTERY_USERNAME` 사용 가능
> - `LOTTO_PW` 대신 `LOTTERY_PASSWORD` 사용 가능
> - `TELEGRAM_TOKEN` 대신 `TELEGRAM_BOT_TOKEN` 사용 가능
> - `TELEGRAM_CHAT_ID` 대신 `TELEGRAM_CHAT_ID` 사용 가능

### Step 4: 실행 스케줄 확인 및 수정

1. `.github/workflows/lottery-auto-buy.yml` 파일을 엽니다.
2. `cron` 설정을 확인합니다:
   ```yaml
   - cron: '30 11 * * 6'  # UTC 기준 토요일 11:30
   ```
3. 원하는 시간으로 수정할 수 있습니다 (UTC 기준):
   ```yaml
   # 한국 시간(KST) = UTC + 9시간
   # 예: 한국 시간 토요일 오전 8시 30분 = UTC 금요일 23시 30분
   - cron: '30 23 * * 5'  # UTC 기준 금요일 23:30 (한국 시간 토요일 08:30)
   ```

#### Cron 표현식 설명

```
분 시 일 월 요일
*  *  *  *  *
│  │  │  │  │
│  │  │  │  └─ 요일 (0=일요일, 6=토요일)
│  │  │  └──── 월 (1-12)
│  │  └─────── 일 (1-31)
│  └────────── 시 (0-23, UTC 기준)
└───────────── 분 (0-59)
```

> 💡 **추천 시간**: 로또는 매주 토요일 오후 8시에 판매가 마감되므로, 그 전 시간(예: 오전 8시~오후 6시)에 구매하는 것을 권장합니다.

## 🎯 사용 방법

### 자동 실행

설정을 완료하면 GitHub Actions가 스케줄에 따라 자동으로 실행됩니다.

### 수동 실행

1. GitHub 저장소의 **Actions** 탭으로 이동합니다.
2. 왼쪽 사이드바에서 **"로또 자동 구매 (계정 1)"** 워크플로우를 선택합니다.
3. **"Run workflow"** 버튼을 클릭합니다.
4. **"Run workflow"** 버튼을 다시 클릭하여 실행합니다.

## 👨‍👩‍👧‍👦 멀티 계정 설정 (선택사항)

가족 계정 등 여러 계정을 사용하려면:

1. `.github/workflows/lottery-auto-buy-account2.yml` 파일을 참고합니다.
2. 새로운 workflow 파일을 생성합니다 (예: `lottery-auto-buy-account3.yml`).
3. GitHub Secrets에 계정별 정보를 추가합니다:
   - `LOTTO_ID_ACCOUNT2` / `LOTTO_PW_ACCOUNT2`
   - `TELEGRAM_TOKEN_ACCOUNT2` / `TELEGRAM_CHAT_ID_ACCOUNT2`
4. Workflow 파일에서 해당 Secret 이름을 사용하도록 수정합니다.

## 🏗️ 프로젝트 구조

```
lotteryauto/
├── .github/
│   └── workflows/
│       ├── lottery-auto-buy.yml          # 계정 1용 워크플로우
│       └── lottery-auto-buy-account2.yml # 계정 2용 워크플로우 (예시)
├── src/
│   └── main/
│       ├── java/lotteryauto/
│       │   ├── config/
│       │   │   ├── LotteryConfig.java              # 환경 변수 설정
│       │   │   ├── SeleniumConfig.java             # Selenium 설정
│       │   │   └── WebClientConfig.java            # WebClient 설정
│       │   ├── service/
│       │   │   ├── LottoService.java               # 로또 구매 로직
│       │   │   └── TelegramNotificationService.java # Telegram 알림
│       │   └── LotteryautoApplication.java         # 메인 애플리케이션
│       └── resources/
│           └── application.yaml                     # 설정 파일
├── pom.xml                                          # Maven 의존성
└── README.md                                        # 이 파일
```

## 🔧 기술 스택

- **Java 25**: 최신 Java 버전
- **Spring Boot 4.0.2**: 애플리케이션 프레임워크
- **Selenium 4.27.0**: 웹 자동화 (Headless Chrome)
- **WebDriverManager 5.9.2**: WebDriver 자동 관리
- **Maven**: 빌드 도구
- **GitHub Actions**: CI/CD 및 스케줄링

## 🔒 보안 주의사항

### ⚠️ 필수 사항

1. **반드시 저장소를 Private으로 설정하세요**
   - Public 저장소는 코드가 공개되므로 보안 위험이 있습니다.
   - Settings > General > Danger Zone > Change visibility

2. **코드에 개인정보를 하드코딩하지 마세요**
   - 모든 민감한 정보는 GitHub Secrets를 통해 관리합니다.
   - 코드 커밋 전에 하드코딩된 정보가 없는지 확인하세요.

3. **로그에 개인정보가 노출되지 않도록 주의하세요**
   - GitHub Actions 로그는 공개될 수 있습니다.
   - 로그에 비밀번호나 토큰이 포함되지 않도록 주의하세요.

### ✅ 권장 사항

- 정기적으로 비밀번호를 변경하세요.
- Telegram Bot Token이 유출되면 즉시 BotFather에서 토큰을 재발급하세요.
- GitHub Secrets는 암호화되어 저장되지만, 필요시 주기적으로 갱신하세요.

## 📞 문제 해결

### ❓ Telegram 알림이 오지 않는 경우

1. **Bot Token 확인**
   - BotFather에서 `/token` 명령어로 현재 토큰 확인
   - GitHub Secrets의 `TELEGRAM_TOKEN`과 일치하는지 확인

2. **Chat ID 확인**
   - 봇과 대화를 시작했는지 확인
   - `getUpdates` API로 Chat ID 재확인

3. **봇 차단 확인**
   - 봇을 차단하지 않았는지 확인
   - 봇과 대화가 가능한지 테스트

### ❓ 구매가 실행되지 않는 경우

1. **GitHub Actions 로그 확인**
   - Actions 탭에서 실행 로그 확인
   - 에러 메시지 확인

2. **환경 변수 확인**
   - GitHub Secrets가 올바르게 설정되었는지 확인
   - Secret 이름이 정확한지 확인 (대소문자 구분)

3. **예치금 확인**
   - 동행복권 계정에 충분한 예치금이 있는지 확인
   - 최소 5,000원 이상 필요

### ❓ 로그인 실패

1. **아이디/비밀번호 확인**
   - 동행복권 사이트에서 직접 로그인 테스트
   - GitHub Secrets의 값이 정확한지 확인

2. **계정 상태 확인**
   - 계정이 정지되지 않았는지 확인
   - 비밀번호 변경 후 90일 경과 알림이 있는지 확인

## ⚖️ 면책 조항 (Disclaimer)

본 프로그램은 개인적인 학습 및 편의를 위한 오픈소스 도구입니다.

**제작자는 다음에 대해 책임을 지지 않습니다:**

- 자동 구매 중 발생하는 어떠한 금전적 손실
- 계정 정지, 제재 등 계정 관련 문제
- 프로그램 사용으로 인한 직접적 또는 간접적 손해
- 프로그램의 오작동으로 인한 문제
- 사이트 정책 변경으로 인한 프로그램 미작동

**사용 전 주의사항:**

- 본 프로그램 사용은 사용자의 자유의사에 따른 것입니다.
- 동행복권의 이용약관 및 정책을 준수해야 합니다.
- 프로그램 사용 전 충분한 테스트를 권장합니다.
- 문제 발생 시 즉시 사용을 중단하고 수동으로 확인하세요.

## 📄 라이선스

이 프로젝트는 개인 사용 목적으로 제공됩니다.  
상업적 이용이나 재배포 시 원작자에게 문의하시기 바랍니다.

## 🤝 기여

버그 리포트, 기능 제안, Pull Request를 환영합니다!

---

**행운을 빕니다! 🍀**
