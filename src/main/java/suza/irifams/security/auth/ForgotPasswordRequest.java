package suza.irifams.security.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    private String username;

    private String fullName;

    private String phone;

}