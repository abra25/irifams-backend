package suza.irifams.security.util;

import java.util.*;

public class PasswordGenerator {

    public static String generate() {

        List<Integer> digits = Arrays.asList(

                0,1,2,3,4,5,6,7,8,9

        );

        Collections.shuffle(digits);

        StringBuilder builder = new StringBuilder();

        for(int i=0;i<4;i++){

            builder.append(digits.get(i));

        }

        return builder.toString();

    }

}
