package suza.irifams.security.config;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import suza.irifams.enums.Role;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;

@Configuration
@RequiredArgsConstructor
public class AdminConfig {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner createDefaultAdmin(
            UserRepository userRepository
    ) {

        return args -> {

            String username = "admin";

            if (!userRepository.existsByUsername(username)) {

                User admin = User.builder()

                        .employeeNo("EMP-ADM-001")

                        .fullName("System Administrator")

                        .username("admin")

                        .email("admin@irifams.com")

                        .phone("+255700000001")

                        .password(
                                passwordEncoder.encode("admin##123")
                        )

                        .gender("Male")

                        .blockName("-")

                        .institution("IRIFAMS Administration")

                        .role(Role.ADMIN)

                        .image("Downloads/admin.jfif")

                        .enabled(true)

                        .build();

                userRepository.save(admin);

                System.out.println(
                        "======================================");
                System.out.println(
                        "DEFAULT ADMIN CREATED SUCCESSFULLY");
                System.out.println(
                        "Username : admin");
                System.out.println(
                        "Password : *******");
                System.out.println(
                        "======================================");

            } else {

                System.out.println(
                        "Default Admin already exists.");
            }

        };

    }

}
