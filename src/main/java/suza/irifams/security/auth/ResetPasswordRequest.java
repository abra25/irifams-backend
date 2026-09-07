package suza.irifams.security.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    private String username;

    private String recoveryToken;

    private String newPassword;
}