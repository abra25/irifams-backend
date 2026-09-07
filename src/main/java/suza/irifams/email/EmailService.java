package suza.irifams.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import suza.irifams.request.ServiceRequest;
import suza.irifams.user.User;
import suza.irifams.input.FarmInput;
import java.nio.charset.StandardCharsets;
import suza.irifams.water.WaterSchedule;
import suza.irifams.plot.Plot;
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;


    /*
     * =========================================================
     * SEND SERVICE REQUEST APPROVAL EMAIL
     * =========================================================
     */

    public void sendRequestApprovalEmail(
            ServiceRequest request
    ) {

        if (request.getFarmer() == null
                || request.getFarmer().getEmail() == null
                || request.getFarmer().getEmail().isBlank()) {

            return;
        }

        String farmerName =
                request.getFarmer().getFullName();

        String email =
                request.getFarmer().getEmail();

        String serviceName =
                request.getServiceType().name();

        String controlNumber =
                request.getControlNumber();

        Double amount =
                request.getAmount();

        String subject =
                "IRIFAMS - Service Request Approved";

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">

                    <h2>IRIFAMS Service Request Approved</h2>

                    <p>
                        Dear <strong>%s</strong>,
                    </p>

                    <p>
                        Your service request has been approved
                        successfully.
                    </p>

                    <table border="1"
                           cellpadding="10"
                           cellspacing="0"
                           style="border-collapse: collapse;">

                        <tr>
                            <td><strong>Request ID</strong></td>
                            <td>#%d</td>
                        </tr>

                        <tr>
                            <td><strong>Service</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Amount</strong></td>
                            <td>TZS %,.2f</td>
                        </tr>

                        <tr>
                            <td><strong>Control Number</strong></td>
                            <td><strong>%s</strong></td>
                        </tr>

                    </table>

                    <p>
                        Please use the control number above
                        when making your payment.
                    </p>

                    <p>
                        <strong>
                            Do not share your control number
                            with unauthorized persons.
                        </strong>
                    </p>

                    <br>

                    <p>
                        Regards,<br>
                        <strong>IRIFAMS</strong><br>
                        Irrigation and Farming Management System
                    </p>

                </body>
                </html>
                """.formatted(
                farmerName,
                request.getId(),
                serviceName,
                amount,
                controlNumber
        );

        sendHtmlEmail(
                email,
                subject,
                html
        );
    }


    /*
     * =========================================================
     * SEND PAYMENT RECEIPT EMAIL
     * =========================================================
     */

    public void sendPaymentReceiptEmail(
            ServiceRequest request,
            String receiptNumber,
            byte[] pdf
    ) {

        if (request.getFarmer() == null
                || request.getFarmer().getEmail() == null
                || request.getFarmer().getEmail().isBlank()) {

            return;
        }

        String farmerName =
                request.getFarmer().getFullName();

        String email =
                request.getFarmer().getEmail();

        String subject =
                "IRIFAMS - Payment Receipt";

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">

                    <h2>IRIFAMS Payment Confirmation</h2>

                    <p>
                        Dear <strong>%s</strong>,
                    </p>

                    <p>
                        Your payment has been successfully
                        verified by IRIFAMS.
                    </p>

                    <table border="1"
                           cellpadding="10"
                           cellspacing="0"
                           style="border-collapse: collapse;">

                        <tr>
                            <td><strong>Request ID</strong></td>
                            <td>#%d</td>
                        </tr>

                        <tr>
                            <td><strong>Service</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Amount Paid</strong></td>
                            <td>TZS %,.2f</td>
                        </tr>

                        <tr>
                            <td><strong>Control Number</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Receipt Number</strong></td>
                            <td><strong>%s</strong></td>
                        </tr>

                    </table>

                    <p>
                        Your official payment receipt is attached
                        to this email as a PDF document.
                    </p>

                    <br>

                    <p>
                        Regards,<br>
                        <strong>IRIFAMS</strong>
                    </p>

                </body>
                </html>
                """.formatted(
                farmerName,
                request.getId(),
                request.getServiceType().name(),
                request.getAmount(),
                request.getControlNumber(),
                receiptNumber
        );

        sendEmailWithAttachment(
                email,
                subject,
                html,
                pdf,
                "IRIFAMS-Payment-Receipt-"
                        + receiptNumber
                        + ".pdf"
        );
    }


    /*
     * =========================================================
     * SEND TEMPORARY PASSWORD EMAIL
     * =========================================================
     */

    public void sendTemporaryPasswordEmail(
            String email,
            String fullName,
            String temporaryPassword
    ) {

        if (email == null
                || email.isBlank()) {

            return;
        }

        String subject =
                "IRIFAMS - Temporary Password";

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">

                    <h2>IRIFAMS Password Recovery</h2>

                    <p>
                        Dear <strong>%s</strong>,
                    </p>

                    <p>
                        Your password has been reset successfully.
                    </p>

                    <p>
                        Your temporary password is:
                    </p>

                    <h2 style="letter-spacing: 5px;">
                        %s
                    </h2>

                    <p>
                        Please login using this temporary password
                        and change it immediately.
                    </p>

                    <p>
                        If you did not request a password reset,
                        please contact the system administrator.
                    </p>

                    <br>

                    <p>
                        Regards,<br>
                        <strong>IRIFAMS</strong>
                    </p>

                </body>
                </html>
                """.formatted(
                fullName,
                temporaryPassword
        );

        sendHtmlEmail(
                email,
                subject,
                html
        );
    }

    /*
     * =========================================================
     * SEND PLOT EMAIL
     * =========================================================
     *
     * Sent when a farmer's plot is created, updated or deleted.
     */

    public void sendPlotEmail(
            User farmer,
            Plot plot,
            String action
    ) {

        if (farmer == null
                || farmer.getEmail() == null
                || farmer.getEmail().isBlank()) {

            return;
        }

        String subject =
                "IRIFAMS - Plot " + action;

        String html = """
            <html>
            <body style="font-family: Arial, sans-serif;">

                <h2>IRIFAMS Plot Management</h2>

                <p>
                    Dear <strong>%s</strong>,
                </p>

                <p>
                    Your plot information has been
                    <strong>%s</strong>.
                </p>

                <table border="1"
                       cellpadding="10"
                       cellspacing="0"
                       style="border-collapse: collapse;">

                    <tr>
                        <td><strong>Plot Number</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Block</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Size</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Soil Type</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Location</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Irrigation Method</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Season</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Status</strong></td>
                        <td>%s</td>
                    </tr>

                </table>

                <p>
                    Please keep this information for your
                    farming records.
                </p>

                <br>

                <p>
                    Regards,<br>
                    <strong>IRIFAMS</strong><br>
                    Irrigation and Farming Management System
                </p>

            </body>
            </html>
            """.formatted(

                farmer.getFullName(),

                action,

                plot.getPlotNo() != null
                        ? plot.getPlotNo()
                        : "N/A",

                plot.getBlock() != null
                        ? plot.getBlock()
                        : "N/A",

                plot.getSize() != null
                        ? plot.getSize().toString()
                        : "N/A",

                plot.getSoilType() != null
                        ? plot.getSoilType()
                        : "N/A",

                plot.getLocationDescription() != null
                        ? plot.getLocationDescription()
                        : "N/A",

                plot.getIrrigationMethod() != null
                        ? plot.getIrrigationMethod()
                        : "N/A",

                plot.getSeason() != null
                        ? plot.getSeason()
                        : "N/A",

                plot.getStatus() != null
                        ? plot.getStatus()
                        : "N/A"
        );

        sendHtmlEmail(
                farmer.getEmail(),
                subject,
                html
        );
    }

    // nOTP
    public void sendOtpEmail(
            String email,
            String fullName,
            String otp
    ) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "User email is not available"
            );
        }

        String subject =
                "IRIFAMS - Password Recovery OTP";

        String html = """
            <html>
            <body style="font-family: Arial, sans-serif;">

                <h2>IRIFAMS Password Recovery</h2>

                <p>Dear <strong>%s</strong>,</p>

                <p>
                    We received a request to reset your
                    IRIFAMS account password.
                </p>

                <p>
                    Your One-Time Password (OTP) is:
                </p>

                <div style="
                    font-size: 36px;
                    font-weight: bold;
                    letter-spacing: 8px;
                    margin: 25px 0;
                ">
                    %s
                </div>

                <p>
                    This OTP will expire in
                    <strong>5 minutes</strong>.
                </p>

                <p>
                    Do not share this OTP with anyone.
                </p>

                <p>
                    If you did not request a password reset,
                    please ignore this email.
                </p>

                <br>

                <p>
                    Regards,<br>
                    <strong>IRIFAMS</strong><br>
                    Irrigation and Farming Management System
                </p>

            </body>
            </html>
            """.formatted(
                fullName,
                otp
        );

        sendHtmlEmail(
                email,
                subject,
                html
        );
    }

    /*
     * =========================================================
     * SEND ACCOUNT CREATED EMAIL
     * =========================================================
     *
     * Sent when Admin/Supervisor creates a new account.
     */

    public void sendAccountCreatedEmail(
            User user
    ) {

        if (user == null
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            return;
        }

        String subject =
                "IRIFAMS - Account Created";

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">

                    <h2>Welcome to IRIFAMS</h2>

                    <p>
                        Dear <strong>%s</strong>,
                    </p>

                    <p>
                        Your IRIFAMS account has been created
                        successfully.
                    </p>

                    <table border="1"
                           cellpadding="10"
                           cellspacing="0"
                           style="border-collapse: collapse;">

                        <tr>
                            <td><strong>Full Name</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Username</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Role</strong></td>
                            <td>%s</td>
                        </tr>

                    </table>

                    <p>
                        You can now login to the IRIFAMS system
                        using your registered username and password.
                    </p>

                    <p>
                        Please keep your login credentials secure.
                    </p>

                    <br>

                    <p>
                        Regards,<br>
                        <strong>IRIFAMS</strong><br>
                        Irrigation and Farming Management System
                    </p>

                </body>
                </html>
                """.formatted(
                user.getFullName(),
                user.getFullName(),
                user.getUsername(),
                user.getRole().name()
        );

        sendHtmlEmail(
                user.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * SEND ACCOUNT STATUS EMAIL
     * =========================================================
     *
     * Sent when Admin activates/deactivates an account.
     */

    public void sendAccountStatusEmail(
            User user
    ) {

        if (user == null
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            return;
        }

        String status =
                user.isEnabled()
                        ? "ACTIVATED"
                        : "DEACTIVATED";

        String subject =
                "IRIFAMS - Account Status Update";

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">

                    <h2>IRIFAMS Account Status Update</h2>

                    <p>
                        Dear <strong>%s</strong>,
                    </p>

                    <p>
                        Your IRIFAMS account status has been updated.
                    </p>

                    <table border="1"
                           cellpadding="10"
                           cellspacing="0"
                           style="border-collapse: collapse;">

                        <tr>
                            <td><strong>Username</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Account Status</strong></td>
                            <td><strong>%s</strong></td>
                        </tr>

                    </table>

                    <p>
                        %s
                    </p>

                    <br>

                    <p>
                        Regards,<br>
                        <strong>IRIFAMS</strong>
                    </p>

                </body>
                </html>
                """.formatted(
                user.getFullName(),
                user.getUsername(),
                status,
                user.isEnabled()
                        ? "You can now login and continue using the system."
                        : "You cannot login while your account is deactivated."
        );

        sendHtmlEmail(
                user.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * SEND PROFILE UPDATED EMAIL
     * =========================================================
     *
     * Sent when Admin/Supervisor updates user information.
     */

    public void sendProfileUpdatedEmail(
            User user
    ) {

        if (user == null
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            return;
        }

        String subject =
                "IRIFAMS - Profile Updated";

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">

                    <h2>IRIFAMS Profile Update</h2>

                    <p>
                        Dear <strong>%s</strong>,
                    </p>

                    <p>
                        Your profile information has been
                        updated successfully.
                    </p>

                    <table border="1"
                           cellpadding="10"
                           cellspacing="0"
                           style="border-collapse: collapse;">

                        <tr>
                            <td><strong>Username</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Full Name</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Email</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Phone</strong></td>
                            <td>%s</td>
                        </tr>

                        <tr>
                            <td><strong>Role</strong></td>
                            <td>%s</td>
                        </tr>

                    </table>

                    <p>
                        If you did not expect these changes,
                        please contact the system administrator.
                    </p>

                    <br>

                    <p>
                        Regards,<br>
                        <strong>IRIFAMS</strong>
                    </p>

                </body>
                </html>
                """.formatted(
                user.getFullName(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );

        sendHtmlEmail(
                user.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * SEND PASSWORD CHANGED EMAIL
     * =========================================================
     */

    public void sendPasswordChangedEmail(
            User user
    ) {

        if (user == null
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            return;
        }

        String subject =
                "IRIFAMS - Password Changed";

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">

                    <h2>IRIFAMS Password Changed</h2>

                    <p>
                        Dear <strong>%s</strong>,
                    </p>

                    <p>
                        Your IRIFAMS account password has been
                        changed successfully.
                    </p>

                    <p>
                        For your security, please do not share
                        your password with anyone.
                    </p>

                    <p>
                        If you did not make this change,
                        please contact the system administrator
                        immediately.
                    </p>

                    <br>

                    <p>
                        Regards,<br>
                        <strong>IRIFAMS</strong>
                    </p>

                </body>
                </html>
                """.formatted(
                user.getFullName()
        );

        sendHtmlEmail(
                user.getEmail(),
                subject,
                html
        );
    }

    /*
     * =========================================================
     * SEND WATER SCHEDULE EMAIL
     * =========================================================
     *
     * Sent when an irrigation schedule is created, updated
     * or deleted.
     */

    public void sendWaterScheduleEmail(
            User farmer,
            WaterSchedule schedule,
            String action
    ) {

        if (farmer == null
                || farmer.getEmail() == null
                || farmer.getEmail().isBlank()) {

            return;
        }

        String subject =
                "IRIFAMS - Irrigation Schedule " + action;

        String html = """
            <html>
            <body style="font-family: Arial, sans-serif;">

                <h2>IRIFAMS Irrigation Schedule</h2>

                <p>
                    Dear <strong>%s</strong>,
                </p>

                <p>
                    Your irrigation schedule has been
                    <strong>%s</strong>.
                </p>

                <table border="1"
                       cellpadding="10"
                       cellspacing="0"
                       style="border-collapse: collapse;">

                    <tr>
                        <td><strong>Schedule ID</strong></td>
                        <td>#%d</td>
                    </tr>

                    <tr>
                        <td><strong>Plot Number</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Block</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Irrigation Date</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Start Time</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>End Time</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Canal</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Season</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Status</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Notes</strong></td>
                        <td>%s</td>
                    </tr>

                </table>

                <p>
                    Please keep this information for your
                    farming records.
                </p>

                <br>

                <p>
                    Regards,<br>
                    <strong>IRIFAMS</strong><br>
                    Irrigation and Farming Management System
                </p>

            </body>
            </html>
            """.formatted(

                farmer.getFullName(),

                action,

                schedule.getId(),

                schedule.getPlot() != null
                        ? schedule.getPlot().getPlotNo()
                        : "N/A",

                schedule.getPlot() != null
                        ? schedule.getPlot().getBlock()
                        : "N/A",

                schedule.getIrrigationDate() != null
                        ? schedule.getIrrigationDate().toString()
                        : "N/A",

                schedule.getStartTime() != null
                        ? schedule.getStartTime().toString()
                        : "N/A",

                schedule.getEndTime() != null
                        ? schedule.getEndTime().toString()
                        : "N/A",

                schedule.getCanal() != null
                        ? schedule.getCanal()
                        : "N/A",

                schedule.getSeason() != null
                        ? schedule.getSeason()
                        : "N/A",

                schedule.getStatus() != null
                        ? schedule.getStatus().name()
                        : "N/A",

                schedule.getNotes() != null
                        ? schedule.getNotes()
                        : "N/A"
        );

        sendHtmlEmail(
                farmer.getEmail(),
                subject,
                html
        );
    }

    /*
     * =========================================================
     * SEND INPUT DISTRIBUTION EMAIL
     * =========================================================
     */

    public void sendInputDistributionEmail(
            User farmer,
            FarmInput input,
            Double quantity
    ) {

        if (farmer == null
                || farmer.getEmail() == null
                || farmer.getEmail().isBlank()) {

            return;
        }

        String subject =
                "IRIFAMS - Farm Input Distribution";

        String html = """
            <html>
            <body style="font-family: Arial, sans-serif;">

                <h2>IRIFAMS Farm Input Distribution</h2>

                <p>
                    Dear <strong>%s</strong>,
                </p>

                <p>
                    You have received farm input through
                    the IRIFAMS system.
                </p>

                <table border="1"
                       cellpadding="10"
                       cellspacing="0"
                       style="border-collapse: collapse;">

                    <tr>
                        <td><strong>Input</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Category</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Quantity Received</strong></td>
                        <td>%s %s</td>
                    </tr>

                    <tr>
                        <td><strong>Season</strong></td>
                        <td>%s</td>
                    </tr>

                    <tr>
                        <td><strong>Block</strong></td>
                        <td>%s</td>
                    </tr>

                </table>

                <p>
                    Please keep this information for your records.
                </p>

                <br>

                <p>
                    Regards,<br>
                    <strong>IRIFAMS</strong><br>
                    Irrigation and Farming Management System
                </p>

            </body>
            </html>
            """.formatted(

                farmer.getFullName(),

                input.getName(),

                input.getCategory(),

                quantity,

                input.getUnit(),

                input.getSeason(),

                farmer.getBlockName()
        );

        sendHtmlEmail(
                farmer.getEmail(),
                subject,
                html
        );
    }

    /*
     * =========================================================
     * INTERNAL HTML EMAIL
     * =========================================================
     */

    private void sendHtmlEmail(
            String to,
            String subject,
            String html
    ) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException e) {

            throw new RuntimeException(
                    "Failed to send email",
                    e
            );
        }
    }


    /*
     * =========================================================
     * INTERNAL EMAIL WITH PDF ATTACHMENT
     * =========================================================
     */

    private void sendEmailWithAttachment(
            String to,
            String subject,
            String html,
            byte[] pdf,
            String fileName
    ) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            helper.addAttachment(
                    fileName,
                    new ByteArrayResource(pdf)
            );

            mailSender.send(message);

        } catch (MessagingException e) {

            throw new RuntimeException(
                    "Failed to send email with attachment",
                    e
            );
        }
    }
}