package lotteryauto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lotteryauto.config.LotteryConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Telegram Bot API를 사용한 알림 서비스
 * 구매 성공, 잔액 부족, 에러 발생 시 사용자에게 알림을 전송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final LotteryConfig lotteryConfig;
    private final WebClient webClient;

    private static final String TELEGRAM_API_BASE_URL = "https://api.telegram.org/bot";

    /**
     * Telegram 메시지 전송
     * 
     * @param message 전송할 메시지
     * @return 전송 성공 여부
     */
    public boolean sendMessage(String message) {
        if (!isConfigured()) {
            log.warn("Telegram 설정이 완료되지 않아 알림을 전송할 수 없습니다.");
            return false;
        }

        try {
            String botToken = lotteryConfig.getTelegramBotToken();
            String chatId = lotteryConfig.getTelegramChatId();
            
            // POST 요청으로 변경하여 URL 인코딩 문제 해결
            String url = String.format("%s%s/sendMessage", TELEGRAM_API_BASE_URL, botToken);
            
            // form-urlencoded 형식으로 전송 (자동 인코딩 처리)
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("chat_id", chatId);
            formData.add("text", message);

            webClient.post()
                    .uri(url)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Telegram 메시지 전송 성공: {}", message);
            return true;
        } catch (Exception e) {
            log.error("Telegram 메시지 전송 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 구매 성공 알림
     * 
     * @param gameNumbers 구매한 번호들
     * @param gameCount 구매한 게임 수
     */
    public void notifyPurchaseSuccess(String gameNumbers, int gameCount) {
        String message = String.format(
                "🎉 로또 자동 구매 성공!\n\n" +
                "구매 게임 수: %d게임\n" +
                "구매 번호:\n%s\n\n" +
                "행운을 빕니다! 🍀",
                gameCount, gameNumbers
        );
        sendMessage(message);
    }

    /**
     * 구매 성공 알림 (잔액 정보 포함)
     * 
     * @param balance 현재 잔액
     */
    public void notifyPurchaseSuccessWithBalance(int balance) {
        String message = String.format(
                "✅ 로또 구매 성공! (잔액: %,d원)",
                balance
        );
        sendMessage(message);
    }

    /**
     * 구매 성공 알림 (5,000원 구매 완료)
     * 
     * @param balance 현재 잔액
     */
    public void notifyPurchase5000WonSuccess(int balance) {
        String message = String.format(
                "✅ 로또 5,000원 구매 완료! (잔액: %,d원)",
                balance
        );
        sendMessage(message);
    }

    /**
     * 구매 실패 알림
     * 
     * @param reason 실패 사유
     */
    public void notifyPurchaseFailure(String reason) {
        String message = String.format(
                "🚨 구매 실패: %s",
                reason
        );
        sendMessage(message);
    }

    /**
     * 잔액 부족 알림
     * 
     * @param requiredAmount 필요 금액
     * @param currentBalance 현재 잔액
     */
    public void notifyInsufficientBalance(int requiredAmount, int currentBalance) {
        String message = String.format(
                "⚠️ 잔액 부족 알림\n\n" +
                "필요 금액: %,d원\n" +
                "현재 잔액: %,d원\n" +
                "부족 금액: %,d원\n\n" +
                "잔액을 충전해주세요.",
                requiredAmount, currentBalance, (requiredAmount - currentBalance)
        );
        sendMessage(message);
    }

    /**
     * 에러 발생 알림
     * 
     * @param errorMessage 에러 메시지
     * @param exception 예외 정보 (선택적)
     */
    public void notifyError(String errorMessage, Exception exception) {
        String message = String.format(
                "❌ 로또 자동 구매 오류 발생\n\n" +
                "오류 내용: %s",
                errorMessage
        );
        
        if (exception != null) {
            String exceptionInfo = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            message += "\n예외 정보: " + exceptionInfo;
        }
        
        sendMessage(message);
    }

    /**
     * 로그인 실패 알림
     */
    public void notifyLoginFailure() {
        String message = "🔐 로그인 실패\n\n" +
                "아이디 또는 비밀번호를 확인해주세요.";
        sendMessage(message);
    }

    /**
     * 시스템 시작 알림
     */
    public void notifySystemStart() {
        String message = "🚀 로또 자동 구매 시스템 시작\n\n" +
                "구매 프로세스를 시작합니다...";
        sendMessage(message);
    }

    /**
     * Telegram 설정 확인
     */
    private boolean isConfigured() {
        return lotteryConfig.getTelegramBotToken() != null 
                && !lotteryConfig.getTelegramBotToken().isEmpty()
                && lotteryConfig.getTelegramChatId() != null 
                && !lotteryConfig.getTelegramChatId().isEmpty();
    }
}

