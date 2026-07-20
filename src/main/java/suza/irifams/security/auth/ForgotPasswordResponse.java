package suza.irifams.security.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ForgotPasswordResponse {

    private String message;

    private String temporaryPassword;

}
