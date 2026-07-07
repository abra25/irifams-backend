package suza.irifams.security.auth;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class AuthResponse {

    private Long id;

    private String token;

    private String image;

    private String username;

    private String fullName;

    private String email;

    private String phone;

    private String gender;

    private String institution;

    private String blockName;

    private String role;
}