package suza.irifams.security.auth;


import lombok.Data;
import suza.irifams.enums.Role;

@Data
public class RegisterRequest {

    private String employeeNo;

    private String fullName;

    private String username;
    private String image;

    private String email;

    private String phone;

    private String password;

    private String gender;

    private String blockName;

    private String institution;

    private Role role;

}