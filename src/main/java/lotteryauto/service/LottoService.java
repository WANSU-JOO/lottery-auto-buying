package lotteryauto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lotteryauto.config.LotteryConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 로또 자동 구매 서비스
 * 동행복권 사이트에서 자동으로 로또를 구매하는 로직을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LottoService {

    private final LotteryConfig lotteryConfig;
    private final TelegramNotificationService telegramNotificationService;
    private final WebDriver webDriver;
    private final WebDriverWait webDriverWait;

    private static final String LOGIN_URL = "https://www.dhlottery.co.kr/login";
    private static final String MAIN_URL = "https://www.dhlottery.co.kr/main";
    private static final String MY_PAGE_URL = "https://www.dhlottery.co.kr/mypage/home";
    private static final String LOTTO_PURCHASE_URL = "https://ol.dhlottery.co.kr/olotto/game/game645.do";
    private static final int MINIMUM_BALANCE = 5000; // 최소 잔액 (원)
    private static final int FIXED_GAME_COUNT = 5; // 고정 구매 게임 수 (5,000원)

    /**
     * 로그인 처리
     * 
     * @return 로그인 성공 여부
     */
    public boolean login() {
        try {
            log.info("로그인 프로세스를 시작합니다.");

            // 1. 로그인 페이지로 이동
            log.info("로그인 페이지로 이동: {}", LOGIN_URL);
            webDriver.get(LOGIN_URL);
            
            // 페이지 로드 완료 대기 (간단하게)
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(2000); // 스크립트 로드 대기
            
            // 2. 팝업 닫기 처리
            closeAllPopups();
            
            // 3. RSA 모듈러스 가져오기 (암호화를 위해 필요)
            waitForRsaModulus();

            // 4. 로그인 정보 확인
            String userId = lotteryConfig.getUsername();
            String userPw = lotteryConfig.getPassword();

            if (userId == null || userId.isEmpty() || userPw == null || userPw.isEmpty()) {
                log.error("로그인 정보가 설정되지 않았습니다. 환경 변수를 확인해주세요.");
                telegramNotificationService.notifyError("로그인 정보가 설정되지 않았습니다.", null);
                return false;
            }

            log.info("JavaScript로 로그인 정보 입력 중...");
            
            // 5. JavaScript로 직접 값 입력 및 로그인
            JavascriptExecutor js = (JavascriptExecutor) webDriver;
            
            // JavaScript로 직접 입력 필드에 값 설정
            js.executeScript(
                "var userIdInput = document.getElementById('inpUserId');" +
                "var passwordInput = document.getElementById('inpUserPswdEncn');" +
                "if (userIdInput) userIdInput.value = arguments[0];" +
                "if (passwordInput) passwordInput.value = arguments[1];",
                userId, userPw
            );
            log.info("입력 필드에 값 설정 완료");
            
            // 6. RSA 암호화 수행 및 hidden 필드에 설정
            encryptAndSetCredentials(userId, userPw);

            // 7. 로그인 폼 제출 (JavaScript로 직접)
            Thread.sleep(500); // 암호화 완료 대기
            js.executeScript("document.getElementById('loginForm').submit();");
            log.info("JavaScript로 로그인 폼 제출 완료");

            // 7. 로그인 완료 대기 (페이지 이동 또는 로그인 결과 확인)
            // 페이지가 로드되거나 URL이 변경될 때까지 대기
            try {
                webDriverWait.until(ExpectedConditions.or(
                        ExpectedConditions.urlContains("/main"),
                        ExpectedConditions.urlContains("/mypage"),
                        ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(text(), '로그아웃')]"))
                ));
            } catch (Exception e) {
                log.debug("페이지 이동 대기 중 타임아웃 (계속 진행): {}", e.getMessage());
                Thread.sleep(2000); // 대체 대기
            }

            // 8. 로그인 성공 여부 확인
            boolean loginSuccess = verifyLogin();

            if (loginSuccess) {
                log.info("로그인 성공!");
                return true;
            } else {
                log.error("로그인 실패: 로그인 검증에 실패했습니다.");
                telegramNotificationService.notifyLoginFailure();
                return false;
            }

        } catch (Exception e) {
            log.error("로그인 중 오류 발생: {}", e.getMessage(), e);
            telegramNotificationService.notifyError("로그인 중 오류가 발생했습니다: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 모든 레이어 팝업 닫기
     * '오늘 하루 열지 않기' 등의 팝업을 모두 찾아서 닫습니다.
     */
    private void closeAllPopups() {
        try {
            log.info("팝업 닫기 처리 시작...");

            // 일반적인 팝업 선택자들
            List<String> popupSelectors = List.of(
                    // 레이어 팝업
                    ".layer-popup",
                    ".popup-layer",
                    ".modal-popup",
                    ".popup-modal",
                    ".layer",
                    ".popup",
                    ".modal",
                    // 닫기 버튼
                    ".btn-close",
                    ".popup-close",
                    ".layer-close",
                    ".modal-close",
                    "[class*='close']",
                    "[class*='Close']",
                    // 오늘 하루 열지 않기 관련
                    ".btn-today-close",
                    ".today-close",
                    "[data-close='today']",
                    // X 버튼
                    "button[aria-label*='닫기']",
                    "button[aria-label*='close']",
                    ".icon-close",
                    ".btn-x"
            );

            // 팝업 배경 클릭으로 닫기 시도
            List<String> overlaySelectors = List.of(
                    ".popup-bg",
                    ".overlay",
                    ".modal-backdrop",
                    ".layer-backdrop",
                    "[class*='overlay']",
                    "[class*='backdrop']"
            );

            // 팝업 닫기 시도 (여러 번 시도)
            for (int attempt = 0; attempt < 5; attempt++) {
                boolean closed = false;

                // 1. 닫기 버튼 클릭 시도
                for (String selector : popupSelectors) {
                    try {
                        List<WebElement> closeButtons = webDriver.findElements(By.cssSelector(selector));
                        for (WebElement button : closeButtons) {
                            if (button.isDisplayed()) {
                                try {
                                    button.click();
                                    log.debug("팝업 닫기 버튼 클릭: {}", selector);
                                    closed = true;
                                    Thread.sleep(500);
                                } catch (Exception e) {
                                    // 클릭 실패 시 JavaScript로 클릭 시도
                                    try {
                                        ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", button);
                                        log.debug("JavaScript로 팝업 닫기 버튼 클릭: {}", selector);
                                        closed = true;
                                        Thread.sleep(500);
                                    } catch (Exception jsE) {
                                        log.debug("팝업 닫기 버튼 클릭 실패: {}", selector);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 선택자로 요소를 찾지 못한 경우 무시
                    }
                }

                // 2. ESC 키로 닫기 시도
                try {
                    WebElement body = webDriver.findElement(By.tagName("body"));
                    body.sendKeys(org.openqa.selenium.Keys.ESCAPE);
                    Thread.sleep(300);
                } catch (Exception e) {
                    // ESC 키 전송 실패 무시
                }

                // 3. 오버레이 클릭으로 닫기 시도 (마지막 수단)
                for (String selector : overlaySelectors) {
                    try {
                        List<WebElement> overlays = webDriver.findElements(By.cssSelector(selector));
                        for (WebElement overlay : overlays) {
                            if (overlay.isDisplayed()) {
                                try {
                                    overlay.click();
                                    log.debug("오버레이 클릭으로 팝업 닫기: {}", selector);
                                    closed = true;
                                    Thread.sleep(500);
                                } catch (Exception e) {
                                    // 오버레이 클릭 실패 무시
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 선택자로 요소를 찾지 못한 경우 무시
                    }
                }

                if (!closed) {
                    break; // 더 이상 닫을 팝업이 없으면 종료
                }
            }

            log.info("팝업 닫기 처리 완료");

        } catch (Exception e) {
            log.warn("팝업 닫기 처리 중 오류 발생 (무시하고 계속 진행): {}", e.getMessage());
        }
    }

    /**
     * RSA 모듈러스가 로드될 때까지 대기
     */
    private void waitForRsaModulus() {
        try {
            log.info("RSA 모듈러스 로드 대기 중...");
            
            // JavaScript에서 RSA 모듈러스가 설정될 때까지 대기
            WebDriverWait shortWait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            shortWait.until(webDriver -> {
                JavascriptExecutor js = (JavascriptExecutor) webDriver;
                Object modulus = js.executeScript("return typeof rsaModulus !== 'undefined' && rsaModulus !== null ? rsaModulus : null;");
                return modulus != null && !modulus.toString().isEmpty();
            });
            
            log.info("RSA 모듈러스 로드 완료");
        } catch (Exception e) {
            log.warn("RSA 모듈러스 로드 대기 중 오류 발생 (계속 진행): {}", e.getMessage());
        }
    }

    /**
     * 자격증명을 RSA로 암호화하여 hidden 필드에 설정
     */
    private void encryptAndSetCredentials(String userId, String userPw) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) webDriver;

            // JavaScript에서 RSA 암호화 함수 호출
            String encryptedUserId = (String) js.executeScript(
                    "if (typeof fnRSAencrypt === 'function') { " +
                    "  return fnRSAencrypt(arguments[0]); " +
                    "} else if (typeof rsa !== 'undefined' && rsa.encrypt) { " +
                    "  return rsa.encrypt(arguments[0]); " +
                    "} else { " +
                    "  return null; " +
                    "}",
                    userId
            );

            String encryptedPassword = (String) js.executeScript(
                    "if (typeof fnRSAencrypt === 'function') { " +
                    "  return fnRSAencrypt(arguments[0]); " +
                    "} else if (typeof rsa !== 'undefined' && rsa.encrypt) { " +
                    "  return rsa.encrypt(arguments[0]); " +
                    "} else { " +
                    "  return null; " +
                    "}",
                    userPw
            );

            if (encryptedUserId == null || encryptedPassword == null) {
                throw new RuntimeException("RSA 암호화 실패: 암호화 함수를 찾을 수 없습니다.");
            }

            // Hidden 필드에 암호화된 값 설정
            js.executeScript(
                    "document.getElementById('userId').value = arguments[0]; " +
                    "document.getElementById('userPswdEncn').value = arguments[1];",
                    encryptedUserId, encryptedPassword
            );

            log.info("자격증명 암호화 및 설정 완료");

        } catch (Exception e) {
            log.error("자격증명 암호화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("자격증명 암호화 실패", e);
        }
    }

    /**
     * 로그인 성공 여부 확인
     * 
     * @return 로그인 성공 여부
     */
    private boolean verifyLogin() {
        try {
            // 메인 페이지로 이동하여 로그인 상태 확인
            webDriver.get(MAIN_URL);
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // 팝업 닫기 (메인 페이지에도 팝업이 있을 수 있음)
            closeAllPopups();

            // 여러 방법으로 로그인 상태 확인
            boolean isLoggedIn = false;

            // 방법 1: 로그아웃 버튼 존재 확인
            try {
                List<WebElement> logoutElements = webDriver.findElements(By.xpath(
                        "//a[contains(text(), '로그아웃')] | " +
                        "//button[contains(text(), '로그아웃')] | " +
                        "//*[@id='btnLogout'] | " +
                        "//*[contains(@class, 'logout')] | " +
                        "//*[contains(@class, 'btn-logout')]"
                ));
                if (!logoutElements.isEmpty() && logoutElements.stream().anyMatch(WebElement::isDisplayed)) {
                    log.info("로그인 확인: 로그아웃 버튼 발견");
                    isLoggedIn = true;
                }
            } catch (Exception e) {
                log.debug("로그아웃 버튼 확인 실패: {}", e.getMessage());
            }

            // 방법 2: 로그인 버튼이 사라졌는지 확인
            if (!isLoggedIn) {
                try {
                    List<WebElement> loginElements = webDriver.findElements(By.xpath(
                            "//a[contains(@href, '/login') and contains(text(), '로그인')] | " +
                            "//*[@id='loginBtn'] | " +
                            "//*[contains(@class, 'btn-login')]"
                    ));
                    boolean loginButtonVisible = loginElements.stream()
                            .anyMatch(WebElement::isDisplayed);
                    if (!loginButtonVisible) {
                        log.info("로그인 확인: 로그인 버튼이 보이지 않음 (로그인된 상태로 추정)");
                        isLoggedIn = true;
                    }
                } catch (Exception e) {
                    log.debug("로그인 버튼 확인 실패: {}", e.getMessage());
                }
            }

            // 방법 3: 마이페이지 링크 존재 확인
            if (!isLoggedIn) {
                try {
                    List<WebElement> mypageElements = webDriver.findElements(By.xpath(
                            "//a[contains(@href, '/mypage')] | " +
                            "//*[@id='mypageBtn'] | " +
                            "//*[contains(@class, 'mypage')]"
                    ));
                    if (!mypageElements.isEmpty() && mypageElements.stream().anyMatch(WebElement::isDisplayed)) {
                        log.info("로그인 확인: 마이페이지 링크 발견");
                        isLoggedIn = true;
                    }
                } catch (Exception e) {
                    log.debug("마이페이지 링크 확인 실패: {}", e.getMessage());
                }
            }

            // 방법 4: JavaScript로 isLoggedIn 변수 확인
            if (!isLoggedIn) {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) webDriver;
                    Object loggedIn = js.executeScript("return typeof isLoggedIn !== 'undefined' ? isLoggedIn : null;");
                    if (loggedIn != null && Boolean.TRUE.equals(loggedIn)) {
                        log.info("로그인 확인: JavaScript isLoggedIn 변수 확인");
                        isLoggedIn = true;
                    }
                } catch (Exception e) {
                    log.debug("JavaScript isLoggedIn 확인 실패: {}", e.getMessage());
                }
            }

            return isLoggedIn;

        } catch (Exception e) {
            log.error("로그인 검증 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 마이페이지에서 예치금 확인 후 메인 페이지로 이동하여 구매 페이지 진입
     * 
     * @return 구매 페이지 진입 성공 여부
     * @throws RuntimeException 잔액 부족 시 프로그램 종료
     */
    public boolean checkBalanceAndNavigateToPurchase() {
        try {
            log.info("예치금 확인 및 구매 페이지 진입 프로세스를 시작합니다.");

            // 1. 마이페이지로 이동하여 예치금 확인
            log.info("마이페이지로 이동: {}", MY_PAGE_URL);
            webDriver.get(MY_PAGE_URL);
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            
            // 팝업 닫기 처리
            closeAllPopups();
            
            // 잔액 확인
            int balance = getBalanceFromMyPage();
            log.info("현재 예치금: {}원", balance);
            log.info("최소 필요 금액: {}원", MINIMUM_BALANCE);
            log.info("잔액 비교: {} < {} = {}", balance, MINIMUM_BALANCE, balance < MINIMUM_BALANCE);

            // 2. 잔액이 5,000원 미만이면 알림 보내고 종료
            if (balance < MINIMUM_BALANCE) {
                log.error("잔액 부족: 현재 잔액 {}원, 최소 필요 금액 {}원", balance, MINIMUM_BALANCE);
                telegramNotificationService.notifyInsufficientBalance(MINIMUM_BALANCE, balance);
                log.info("프로그램을 종료합니다.");
                System.exit(1);
                return false; // 실제로는 도달하지 않음
            }
            
            log.info("잔액 충분: 현재 잔액 {}원 >= 최소 필요 금액 {}원", balance, MINIMUM_BALANCE);

            // 3. 메인 페이지로 이동
            log.info("메인 페이지로 이동: {}", MAIN_URL);
            webDriver.get(MAIN_URL);
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            
            // 팝업 닫기 처리
            closeAllPopups();

            // 4. 로또 6/45 구매 페이지로 이동
            log.info("로또 6/45 구매 페이지로 이동: {}", LOTTO_PURCHASE_URL);
            webDriver.get(LOTTO_PURCHASE_URL);
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // 5. 팝업 닫기 처리
            closeAllPopups();

            // 6. iframe으로 전환
            switchToPurchaseIframe();

            log.info("구매 페이지 진입 완료");
            return true;

        } catch (Exception e) {
            log.error("예치금 확인 및 구매 페이지 진입 중 오류 발생: {}", e.getMessage(), e);
            telegramNotificationService.notifyError("예치금 확인 및 구매 페이지 진입 중 오류가 발생했습니다: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 마이페이지에서 예치금 금액 파싱
     * 
     * @return 예치금 금액 (원, 콤마 제거된 숫자)
     */
    private int getBalanceFromMyPage() {
        try {
            log.info("마이페이지에서 예치금 확인 중...");

            // 페이지 로드 대기
            Thread.sleep(3000);

            JavascriptExecutor js = (JavascriptExecutor) webDriver;
            String balanceText = null;

            // 방법 1: 사이트 내부 API (getUserMndp) 호출
            log.info("방법 1: getUserMndp API 호출 시도...");
            try {
                webDriver.manage().timeouts().scriptTimeout(java.time.Duration.ofSeconds(10));
                Object result = js.executeAsyncScript(
                    "var cb = arguments[arguments.length - 1];" +
                    "if (typeof cmmUtil !== 'undefined' && typeof cmmUtil.getUserMndp === 'function') {" +
                    "  cmmUtil.getUserMndp(function(d) {" +
                    "    if (d) cb((d.totalAmt || d.crntEntrsAmt || 0).toString());" +
                    "    else cb('0');" +
                    "  });" +
                    "} else {" +
                    "  cb(null);" +
                    "}"
                );
                if (result != null) {
                    balanceText = result.toString();
                    log.info("방법 1 결과: {}원", balanceText);
                }
            } catch (Exception e) {
                log.warn("방법 1 실패: {}", e.getMessage());
            }

            // 방법 2: DOM 요소에서 직접 추출 (방법 1 실패 시)
            if (balanceText == null || balanceText.isEmpty() || balanceText.equals("0")) {
                log.info("방법 2: DOM 요소 직접 추출 시도...");
                try {
                    Object result = js.executeScript(
                        "var ids = ['totalAmt', 'divCrntEntrsAmt', 'navTotalAmt', 'tooltipTotalAmt'];" +
                        "for (var i=0; i<ids.length; i++) {" +
                        "  var el = document.getElementById(ids[i]);" +
                        "  if (el) {" +
                        "    var val = el.textContent || el.innerText || '';" +
                        "    val = val.replace(/[^0-9]/g, '');" +
                        "    if (val && val !== '0') return val;" +
                        "  }" +
                        "}" +
                        "return '0';"
                    );
                    if (result != null) {
                        balanceText = result.toString();
                        log.info("방법 2 결과: {}원", balanceText);
                    }
                } catch (Exception e) {
                    log.warn("방법 2 실패: {}", e.getMessage());
                }
            }

            // 방법 3: 페이지 소스 정규식 파싱 (최후 수단)
            if (balanceText == null || balanceText.isEmpty() || balanceText.equals("0")) {
                log.info("방법 3: 페이지 소스 정규식 파싱 시도...");
                String source = webDriver.getPageSource();
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("id=\"(?:totalAmt|divCrntEntrsAmt)\"[^>]*>([0-9,]+)");
                java.util.regex.Matcher m = p.matcher(source);
                if (m.find()) {
                    balanceText = m.group(1);
                    log.info("방법 3 결과: {}원", balanceText);
                }
            }

            // 최종 정수 변환
            String cleaned = (balanceText != null) ? balanceText.replaceAll("[^0-9]", "") : "0";
            if (cleaned.isEmpty()) cleaned = "0";
            
            int balance = Integer.parseInt(cleaned);
            log.info("✅ 예치금 확인 완료: {}원", balance);
            
            return balance;

        } catch (Exception e) {
            log.error("예치금 확인 중 치명적 오류: {}", e.getMessage());
            return 0; // 오류 발생 시 안전하게 0 반환 (이후 로직에서 잔액 부족으로 처리됨)
        }
    }

    /**
     * 구매 페이지의 iframe으로 전환
     */
    private void switchToPurchaseIframe() {
        try {
            log.info("구매 페이지 iframe으로 전환 중...");

            // iframe이 로드될 때까지 대기
            WebElement iframe = webDriverWait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("ifrm_answer"))
            );

            // iframe으로 전환
            webDriver.switchTo().frame(iframe);
            log.info("iframe으로 전환 완료");

            // iframe 내부가 로드될 때까지 대기
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        } catch (Exception e) {
            log.error("iframe 전환 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("iframe 전환 실패: " + e.getMessage(), e);
        }
    }

    /**
     * iframe 내부에서 로또 5게임(5,000원) 자동 구매 실행
     * 무조건 5게임(5,000원)만 구매합니다.
     * 
     * @return 구매 성공 여부
     */
    public boolean purchaseLotto() {
        try {
            log.info("로또 {}게임(5,000원) 자동 구매 프로세스를 시작합니다. (고정 구매)", FIXED_GAME_COUNT);

            // iframe 내부에서 작업 (이미 전환된 상태라고 가정)
            // 만약 전환되지 않았다면 다시 전환 시도
            try {
                webDriver.switchTo().defaultContent();
                WebElement iframe = webDriverWait.until(
                        ExpectedConditions.presenceOfElementLocated(By.id("ifrm_answer"))
                );
                webDriver.switchTo().frame(iframe);
                log.info("iframe으로 재전환 완료");
            } catch (Exception e) {
                log.debug("iframe 재전환 시도 (이미 iframe 내부일 수 있음): {}", e.getMessage());
            }

            // 1. 5게임 선택: 자동선택 버튼을 5번 클릭하고 확인 버튼을 5번 클릭
            log.info("{}게임 선택 시작... (고정 구매)", FIXED_GAME_COUNT);
            selectFiveGames();

            // 2. 왼쪽 리스트에 5게임이 모두 있는지 확인
            log.info("선택된 게임 수 확인 중...");
            int selectedGameCount = verifySelectedGameCount();
            if (selectedGameCount < FIXED_GAME_COUNT) {
                log.warn("선택된 게임 수가 부족합니다: {}게임 (목표: {}게임)", selectedGameCount, FIXED_GAME_COUNT);
                // 부족한 만큼 추가 선택 시도
                int remainingGames = FIXED_GAME_COUNT - selectedGameCount;
                log.info("부족한 {}게임 추가 선택 시도...", remainingGames);
                for (int i = 0; i < remainingGames; i++) {
                    selectSingleGame();
                    Thread.sleep(800); // 각 선택 사이 대기
                }
                selectedGameCount = verifySelectedGameCount();
            }

            if (selectedGameCount < FIXED_GAME_COUNT) {
                throw new RuntimeException(String.format("%d게임 선택 실패: 현재 %d게임만 선택됨", FIXED_GAME_COUNT, selectedGameCount));
            }

            log.info("{}게임 선택 완료 확인: {}게임 (고정 구매)", FIXED_GAME_COUNT, selectedGameCount);

            // 3. 구매하기 버튼 클릭
            log.info("구매하기 버튼 클릭 중...");
            WebElement buyButton = webDriverWait.until(
                    ExpectedConditions.elementToBeClickable(By.id("btnBuy"))
            );
            buyButton.click();
            log.info("구매하기 버튼 클릭 완료");

            // 4. Alert 처리 (구매 확인) - ExpectedConditions.alertIsPresent() 사용
            try {
                log.info("구매 확인 Alert 대기 중...");
                org.openqa.selenium.Alert alert = webDriverWait.until(
                        ExpectedConditions.alertIsPresent()
                );
                String alertText = alert.getText();
                log.info("Alert 확인: {}", alertText);

                if (alertText.contains("구매하시겠습니까") || alertText.contains("구매")) {
                    alert.accept();
                    log.info("구매 확인 Alert 승인 완료");
                } else {
                    log.warn("예상치 못한 Alert: {}", alertText);
                    alert.accept(); // 일단 승인
                }
            } catch (Exception e) {
                log.warn("Alert 처리 중 오류 발생 (계속 진행): {}", e.getMessage());
            }

            // 5. 결과 확인 대기
            log.info("구매 결과 확인 대기 중...");
            Thread.sleep(2000); // 결과 팝업이 나타날 때까지 대기

            // 결과 팝업 확인
            boolean purchaseSuccess = checkPurchaseResult();

            // 6. 결과 알림 전송
            if (purchaseSuccess) {
                // 구매 후 잔액 확인 (iframe 밖으로 나가서 확인)
                int remainingBalance = getRemainingBalanceAfterPurchase();
                telegramNotificationService.notifyPurchase5000WonSuccess(remainingBalance);
                log.info("구매 성공 알림 전송 완료");
            } else {
                String failureReason = getPurchaseFailureReason();
                telegramNotificationService.notifyPurchaseFailure(failureReason);
                log.error("구매 실패 알림 전송: {}", failureReason);
            }

            // 7. 브라우저 종료
            log.info("브라우저를 종료합니다.");
            webDriver.quit();

            return purchaseSuccess;

        } catch (Exception e) {
            log.error("로또 구매 중 오류 발생: {}", e.getMessage(), e);
            telegramNotificationService.notifyError("로또 구매 중 오류가 발생했습니다: " + e.getMessage(), e);
            
            // 오류 발생 시에도 브라우저 종료
            try {
                webDriver.quit();
            } catch (Exception quitException) {
                log.error("브라우저 종료 중 오류: {}", quitException.getMessage());
            }
            
            return false;
        }
    }

    /**
     * 5게임 선택: 자동선택 버튼과 확인 버튼을 5번 반복 클릭
     * 무조건 5게임(5,000원)만 구매합니다.
     */
    private void selectFiveGames() {
        try {
            for (int i = 1; i <= FIXED_GAME_COUNT; i++) {
                log.info("게임 {} 선택 중...", i);

                // 자동선택 버튼 클릭
                WebElement autoSelectButton = webDriverWait.until(
                        ExpectedConditions.elementToBeClickable(By.id("num2"))
                );
                autoSelectButton.click();
                log.debug("자동선택 버튼 클릭 완료 (게임 {})", i);

                // 버튼 클릭 후 짧은 대기
                Thread.sleep(500);

                // 확인 버튼 클릭
                WebElement confirmButton = webDriverWait.until(
                        ExpectedConditions.elementToBeClickable(By.id("btnSelectNum"))
                );
                confirmButton.click();
                log.debug("확인 버튼 클릭 완료 (게임 {})", i);

                // 각 게임 선택 사이 적절한 대기 (사이트 부하 방지 및 차단 방지)
                if (i < FIXED_GAME_COUNT) {
                    Thread.sleep(800); // 마지막 게임이 아니면 대기
                }
            }

            log.info("{}게임 선택 프로세스 완료 (고정 구매)", FIXED_GAME_COUNT);

        } catch (Exception e) {
            log.error("5게임 선택 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("5게임 선택 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 단일 게임 선택 (자동선택 + 확인)
     */
    private void selectSingleGame() {
        try {
            // 자동선택 버튼 클릭
            WebElement autoSelectButton = webDriverWait.until(
                    ExpectedConditions.elementToBeClickable(By.id("num2"))
            );
            autoSelectButton.click();
            Thread.sleep(500);

            // 확인 버튼 클릭
            WebElement confirmButton = webDriverWait.until(
                    ExpectedConditions.elementToBeClickable(By.id("btnSelectNum"))
            );
            confirmButton.click();

        } catch (Exception e) {
            log.error("단일 게임 선택 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("단일 게임 선택 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 선택된 게임 수 확인
     * 
     * @return 선택된 게임 수
     */
    private int verifySelectedGameCount() {
        try {
            // 왼쪽 선택 목록에서 게임 수 확인
            // 일반적으로 리스트 아이템이나 게임 번호가 표시되는 요소를 찾아야 함
            // 여러 방법으로 시도

            // 방법 1: 리스트 아이템 개수 확인
            try {
                List<WebElement> gameItems = webDriver.findElements(By.cssSelector(
                        ".selected-list li, .game-list li, [class*='selected'] li, [class*='game-item']"
                ));
                if (!gameItems.isEmpty()) {
                    int count = (int) gameItems.stream()
                            .filter(WebElement::isDisplayed)
                            .count();
                    log.debug("리스트 아이템으로 게임 수 확인: {}게임", count);
                    return count;
                }
            } catch (Exception e) {
                log.debug("리스트 아이템 확인 실패: {}", e.getMessage());
            }

            // 방법 2: JavaScript로 선택된 게임 수 확인
            try {
                JavascriptExecutor js = (JavascriptExecutor) webDriver;
                Object countObj = js.executeScript(
                        "try { " +
                        "  var items = document.querySelectorAll('.selected-list li, .game-list li, [class*=\"selected\"] li'); " +
                        "  return items ? items.length : 0; " +
                        "} catch(e) { return 0; }"
                );
                if (countObj != null) {
                    int count = ((Number) countObj).intValue();
                    log.debug("JavaScript로 게임 수 확인: {}게임", count);
                    return count;
                }
            } catch (Exception e) {
                log.debug("JavaScript 게임 수 확인 실패: {}", e.getMessage());
            }

            // 방법 3: 게임 번호 표시 영역 확인
            try {
                List<WebElement> numberElements = webDriver.findElements(By.cssSelector(
                        "[class*='number'], [class*='ball'], [class*='lotto']"
                ));
                // 게임 번호가 표시되는 패턴을 찾아서 개수 계산
                // 일반적으로 6개의 번호가 1게임이므로, 번호 개수를 6으로 나눔
                int visibleNumbers = (int) numberElements.stream()
                        .filter(WebElement::isDisplayed)
                        .count();
                if (visibleNumbers > 0 && visibleNumbers % 6 == 0) {
                    int count = visibleNumbers / 6;
                    log.debug("게임 번호로 게임 수 확인: {}게임 (번호 {}개)", count, visibleNumbers);
                    return count;
                }
            } catch (Exception e) {
                log.debug("게임 번호 확인 실패: {}", e.getMessage());
            }

            // 방법 4: 페이지 텍스트에서 "게임" 키워드로 확인
            try {
                String pageText = webDriver.findElement(By.tagName("body")).getText();
                // "1게임", "2게임" 등의 패턴 찾기
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)게임");
                java.util.regex.Matcher matcher = pattern.matcher(pageText);
                if (matcher.find()) {
                    int count = Integer.parseInt(matcher.group(1));
                    log.debug("페이지 텍스트로 게임 수 확인: {}게임", count);
                    return count;
                }
            } catch (Exception e) {
                log.debug("페이지 텍스트 확인 실패: {}", e.getMessage());
            }

            // 확인할 수 없는 경우, 선택 프로세스를 완료했다고 가정하고 고정 게임 수로 반환
            log.warn("선택된 게임 수를 정확히 확인할 수 없습니다. 선택 프로세스 완료를 가정합니다.");
            return FIXED_GAME_COUNT;

        } catch (Exception e) {
            log.error("게임 수 확인 중 오류: {}", e.getMessage(), e);
            // 오류 발생 시에도 선택 프로세스는 완료되었다고 가정
            return FIXED_GAME_COUNT;
        }
    }

    /**
     * 구매 결과 확인
     * 
     * @return 구매 성공 여부
     */
    private boolean checkPurchaseResult() {
        try {
            log.info("구매 결과 확인 중...");

            // 여러 방법으로 결과 확인
            boolean success = false;

            // 방법 1: 결과 팝업에서 성공 메시지 확인
            try {
                // iframe 내부에서 결과 팝업 찾기
                List<WebElement> successMessages = webDriver.findElements(By.xpath(
                        "//*[contains(text(), '구매가 완료되었습니다')] | " +
                        "//*[contains(text(), '구매 완료')] | " +
                        "//*[contains(text(), '구매되었습니다')] | " +
                        "//*[contains(text(), '완료되었습니다')]"
                ));

                if (!successMessages.isEmpty()) {
                    for (WebElement element : successMessages) {
                        if (element.isDisplayed()) {
                            String text = element.getText();
                            log.info("구매 성공 메시지 발견: {}", text);
                            success = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("성공 메시지 확인 실패: {}", e.getMessage());
            }

            // 방법 2: JavaScript로 결과 확인
            if (!success) {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) webDriver;
                    String pageText = (String) js.executeScript("return document.body.innerText || document.body.textContent;");
                    
                    if (pageText != null && (
                            pageText.contains("구매가 완료되었습니다") ||
                            pageText.contains("구매 완료") ||
                            pageText.contains("구매되었습니다")
                    )) {
                        log.info("JavaScript로 구매 성공 확인");
                        success = true;
                    }
                } catch (Exception e) {
                    log.debug("JavaScript 결과 확인 실패: {}", e.getMessage());
                }
            }

            // 방법 3: 실패 메시지 확인
            if (!success) {
                try {
                    List<WebElement> failureMessages = webDriver.findElements(By.xpath(
                            "//*[contains(text(), '실패')] | " +
                            "//*[contains(text(), '오류')] | " +
                            "//*[contains(text(), '에러')] | " +
                            "//*[contains(text(), '불가')]"
                    ));

                    for (WebElement element : failureMessages) {
                        if (element.isDisplayed()) {
                            String text = element.getText();
                            log.warn("구매 실패 메시지 발견: {}", text);
                            // 실패 메시지가 있으면 실패로 간주
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.debug("실패 메시지 확인 실패: {}", e.getMessage());
                }
            }

            // 방법 4: 페이지 상태로 확인
            if (!success) {
                try {
                    String pageSource = webDriver.getPageSource();
                    
                    // 성공적인 구매 후에는 특정 요소나 URL이 변경될 수 있음
                    if (pageSource.contains("구매가 완료") || pageSource.contains("구매 완료")) {
                        log.info("페이지 소스에서 구매 성공 확인");
                        success = true;
                    }
                } catch (Exception e) {
                    log.debug("페이지 상태 확인 실패: {}", e.getMessage());
                }
            }

            log.info("구매 결과 확인 완료: {}", success ? "성공" : "실패");
            return success;

        } catch (Exception e) {
            log.error("구매 결과 확인 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 구매 실패 사유 파싱
     * 
     * @return 실패 사유
     */
    private String getPurchaseFailureReason() {
        try {
            // iframe 내부에서 실패 메시지 찾기
            List<String> failureSelectors = List.of(
                    ".error",
                    ".fail",
                    ".alert",
                    "[class*='error']",
                    "[class*='fail']",
                    "[class*='alert']"
            );

            for (String selector : failureSelectors) {
                try {
                    List<WebElement> elements = webDriver.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        if (element.isDisplayed()) {
                            String text = element.getText();
                            if (text != null && !text.trim().isEmpty()) {
                                return text.trim();
                            }
                        }
                    }
                } catch (Exception e) {
                    // 계속 시도
                }
            }

            // JavaScript로 실패 메시지 찾기
            try {
                JavascriptExecutor js = (JavascriptExecutor) webDriver;
                String pageText = (String) js.executeScript("return document.body.innerText || document.body.textContent;");
                
                if (pageText != null) {
                    // 실패 관련 키워드가 포함된 문장 찾기
                    String[] lines = pageText.split("\n");
                    for (String line : lines) {
                        if (line.contains("실패") || line.contains("오류") || line.contains("에러") || 
                            line.contains("불가") || line.contains("부족") || line.contains("한도")) {
                            return line.trim();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("JavaScript로 실패 사유 확인 실패: {}", e.getMessage());
            }

            return "원인 불명 (팝업 텍스트를 확인할 수 없습니다)";

        } catch (Exception e) {
            log.error("구매 실패 사유 파싱 중 오류: {}", e.getMessage(), e);
            return "오류 발생: " + e.getMessage();
        }
    }

    /**
     * 구매 후 잔액 확인
     * 
     * @return 현재 잔액
     */
    private int getRemainingBalanceAfterPurchase() {
        try {
            log.info("구매 후 잔액 확인 중...");

            // iframe 밖으로 나가기
            webDriver.switchTo().defaultContent();

            // 잔액 정보가 표시되는 요소 찾기
            // 구매 페이지의 예치금 잔액 표시 영역 확인
            try {
                WebElement balanceElement = webDriverWait.until(
                        ExpectedConditions.presenceOfElementLocated(By.id("crntEntrsAmt"))
                );
                String balanceText = balanceElement.getText();
                String balanceNumber = balanceText.replaceAll("[^0-9]", "");
                if (!balanceNumber.isEmpty()) {
                    int balance = Integer.parseInt(balanceNumber);
                    log.info("구매 후 잔액: {}원", balance);
                    return balance;
                }
            } catch (Exception e) {
                log.debug("crntEntrsAmt에서 잔액 확인 실패: {}", e.getMessage());
            }

            // 마이페이지로 이동하여 잔액 확인
            try {
                webDriver.get(MY_PAGE_URL);
                webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                closeAllPopups();
                return getBalanceFromMyPage();
            } catch (Exception e) {
                log.warn("마이페이지에서 잔액 확인 실패: {}", e.getMessage());
            }

            // 잔액을 확인할 수 없는 경우 0 반환
            log.warn("잔액을 확인할 수 없어 0원으로 표시합니다.");
            return 0;

        } catch (Exception e) {
            log.error("구매 후 잔액 확인 중 오류: {}", e.getMessage(), e);
            return 0;
        }
    }
}

