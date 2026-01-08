package lotteryauto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lotteryauto.config.LotteryConfig;
import lotteryauto.service.LottoService;
import lotteryauto.service.TelegramNotificationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 로또 자동 구매 시스템 메인 애플리케이션
 * GitHub Actions에서 실행되어 자동으로 로또를 구매합니다.
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class LotteryautoApplication implements CommandLineRunner {

	private final LottoService lottoService;
	private final TelegramNotificationService telegramNotificationService;
	private final LotteryConfig lotteryConfig;

	public static void main(String[] args) {
		SpringApplication.run(LotteryautoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("========================================");
		log.info("로또 자동 구매 시스템 시작");
		log.info("========================================");

		try {
			// 환경 변수 유효성 검증
			if (!lotteryConfig.isValid()) {
				log.error("환경 변수가 올바르게 설정되지 않았습니다.");
				log.error("필수 환경 변수: LOTTO_ID (또는 LOTTERY_USERNAME), LOTTO_PW (또는 LOTTERY_PASSWORD), TELEGRAM_TOKEN (또는 TELEGRAM_BOT_TOKEN), TELEGRAM_CHAT_ID");
				System.exit(1);
				return;
			}

			// 1. 로그인
			log.info("[1/3] 로그인 프로세스 시작...");
			boolean loginSuccess = lottoService.login();
			if (!loginSuccess) {
				log.error("로그인 실패로 프로그램을 종료합니다.");
				System.exit(1);
				return;
			}
			log.info("[1/3] 로그인 완료");

			// 2. 예치금 확인 및 구매 페이지 진입
			log.info("[2/3] 예치금 확인 및 구매 페이지 진입...");
			boolean navigationSuccess = lottoService.checkBalanceAndNavigateToPurchase();
			if (!navigationSuccess) {
				log.error("구매 페이지 진입 실패로 프로그램을 종료합니다.");
				// checkBalanceAndNavigateToPurchase 내부에서 잔액 부족 시 이미 System.exit(1) 호출됨
				System.exit(1);
				return;
			}
			log.info("[2/3] 구매 페이지 진입 완료");

			// 3. 로또 5게임(5,000원) 구매
			log.info("[3/3] 로또 5게임(5,000원) 구매 시작...");
			boolean purchaseSuccess = lottoService.purchaseLotto();
			if (purchaseSuccess) {
				log.info("[3/3] 로또 구매 완료!");
				log.info("========================================");
				log.info("모든 프로세스가 성공적으로 완료되었습니다.");
				log.info("========================================");
				// 작업 완료 후 프로세스 강제 종료 (GitHub Actions가 즉시 완료되도록 함)
				System.exit(0);
			} else {
				log.error("[3/3] 로또 구매 실패");
				log.error("========================================");
				log.error("구매 프로세스가 실패했습니다.");
				log.error("========================================");
				System.exit(1);
			}

		} catch (Exception e) {
			log.error("========================================");
			log.error("치명적 오류 발생: {}", e.getMessage(), e);
			log.error("========================================");
			telegramNotificationService.notifyError("시스템 오류가 발생했습니다: " + e.getMessage(), e);
			System.exit(1);
		}
	}
}
