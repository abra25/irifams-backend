package suza.irifams.security.util;

import java.security.SecureRandom;

public class OtpGenerator {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private OtpGenerator() {
    }

    public static String generate() {

        int number =
                100000 + RANDOM.nextInt(900000);

        return String.valueOf(number);
    }
}