package suza.irifams.security.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    private String username;

    private String otp;
}