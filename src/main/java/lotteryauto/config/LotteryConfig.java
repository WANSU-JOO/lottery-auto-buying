package lotteryauto.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 로또 자동 구매 시스템 환경 변수 설정
 * GitHub Secrets에서 주입되는 값들을 관리합니다.
 */
@Configuration
@Getter
public class LotteryConfig {

    /**
     * 로또 사이트 로그인 아이디
     * 환경 변수: LOTTO_ID 또는 LOTTERY_USERNAME
     */
    @Value("${LOTTO_ID:${LOTTERY_USERNAME:}}")
    private String username;

    /**
     * 로또 사이트 로그인 비밀번호
     * 환경 변수: LOTTO_PW 또는 LOTTERY_PASSWORD
     */
    @Value("${LOTTO_PW:${LOTTERY_PASSWORD:}}")
    private String password;

    /**
     * Telegram Bot Token
     * 환경 변수: TELEGRAM_TOKEN 또는 TELEGRAM_BOT_TOKEN
     */
    @Value("${TELEGRAM_TOKEN:${TELEGRAM_BOT_TOKEN:}}")
    private String telegramBotToken;

    /**
     * Telegram Chat ID (알림을 받을 채팅방 ID)
     */
    @Value("${TELEGRAM_CHAT_ID:}")
    private String telegramChatId;

    /**
     * 환경 변수 유효성 검증
     */
    public boolean isValid() {
        return username != null && !username.isEmpty()
                && password != null && !password.isEmpty()
                && telegramBotToken != null && !telegramBotToken.isEmpty()
                && telegramChatId != null && !telegramChatId.isEmpty();
    }
}

