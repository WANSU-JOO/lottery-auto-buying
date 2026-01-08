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
        
        // Headless 모드 필수 (GitHub Actions에는 디스플레이가 없음)
        options.addArguments("--headless=new");
        
        // Linux 환경 최적화
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        
        // 봇 차단 방지를 위한 User-Agent 설정
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        // 추가 보안 및 성능 옵션
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-images"); // 이미지 로딩 비활성화로 성능 향상
        options.addArguments("--window-size=1920,1080");
        
        // 자동화 감지 방지
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        // 로그 레벨 설정
        System.setProperty("webdriver.chrome.logfile", "/dev/null");
        System.setProperty("webdriver.chrome.verboseLogging", "false");

        log.info("Chrome WebDriver를 Headless 모드로 초기화합니다.");
        return new ChromeDriver(options);
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

