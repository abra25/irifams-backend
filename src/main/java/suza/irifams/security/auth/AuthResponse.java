package suza.irifams.security.auth;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class AuthResponse {

    private String token;

    private String image;

    private String username;

    private String fullName;

    private String role;
}