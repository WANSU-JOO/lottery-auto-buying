package lotteryauto.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

/**
 * Selenium WebDriver 설정 클래스
 * GitHub Actions 환경에 최적화된 Headless 모드 설정
 */
@Slf4j
@Configuration
public class SeleniumConfig {

    /**
     * Chrome WebDriver를 Headless 모드로 생성
     * GitHub Actions 서버 환경에 최적화된 설정
     */
    @Bean
    @Scope("prototype")
    public WebDriver webDriver() {
        // WebDriverManager를 사용하여 ChromeDriver 자동 관리
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        
        // 환경 변수 LOTTO_HEADLESS가 false이면 브라우저를 화면에 띄움
        String headlessEnv = System.getenv("LOTTO_HEADLESS");
        boolean isHeadless = headlessEnv == null || !headlessEnv.equalsIgnoreCase("false");

        if (isHeadless) {
            options.addArguments("--headless=new");
            log.info("Chrome WebDriver를 Headless 모드로 초기화합니다.");
        } else {
            log.info("Chrome WebDriver를 일반 모드(브라우저 가시화)로 초기화합니다.");
        }
        
        // Linux 환경 최적화
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        
        // 봇 차단 방지를 위한 User-Agent 설정 (최신 Chrome 버전)
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        
        // 추가 보안 및 성능 옵션
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        // 이미지 로딩은 활성화 (일부 사이트에서 이미지 없으면 차단할 수 있음)
        // options.addArguments("--disable-images");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--lang=ko-KR,ko");
        options.addArguments("--accept-lang=ko-KR,ko;q=0.9");
        
        // 자동화 감지 방지
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        // 추가 봇 차단 우회 설정
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        // 로그 레벨 설정
        System.setProperty("webdriver.chrome.logfile", "/dev/null");
        System.setProperty("webdriver.chrome.verboseLogging", "false");

        log.info("Chrome WebDriver를 Headless 모드로 초기화합니다.");
        ChromeDriver driver = new ChromeDriver(options);
        
        // 자동화 감지 우회를 위한 JavaScript 주입
        try {
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            // webdriver 속성 숨기기
            js.executeScript(
                "Object.defineProperty(navigator, 'webdriver', {" +
                "  get: () => undefined" +
                "});"
            );
            // Chrome 객체 추가
            js.executeScript(
                "window.chrome = {" +
                "  runtime: {}" +
                "};"
            );
            // Permissions 객체 추가
            js.executeScript(
                "const originalQuery = window.navigator.permissions.query;" +
                "window.navigator.permissions.query = (parameters) => (" +
                "  parameters.name === 'notifications' ?" +
                "    Promise.resolve({ state: Notification.permission }) :" +
                "    originalQuery(parameters)" +
                ");"
            );
            log.info("자동화 감지 우회 JavaScript 주입 완료");
        } catch (Exception e) {
            log.warn("자동화 감지 우회 JavaScript 주입 실패 (계속 진행): {}", e.getMessage());
        }
        
        return driver;
    }

    /**
     * WebDriverWait 빈 생성
     * 명시적 대기 시간 설정
     */
    @Bean
    @Scope("prototype")
    public WebDriverWait webDriverWait(WebDriver webDriver) {
        return new WebDriverWait(webDriver, Duration.ofSeconds(30));
    }
}

