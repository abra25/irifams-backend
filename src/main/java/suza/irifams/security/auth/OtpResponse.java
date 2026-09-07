package suza.irifams.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OtpResponse {

    private String message;

    private String recoveryToken;
}