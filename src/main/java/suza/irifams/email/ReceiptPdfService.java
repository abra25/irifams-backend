package suza.irifams.email;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import suza.irifams.request.ServiceRequest;

import java.io.ByteArrayOutputStream;

@Service
public class ReceiptPdfService {


    public byte[] generateReceipt(
            ServiceRequest request,
            String receiptNumber
    ) {

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();


            Document document =
                    new Document();


            PdfWriter.getInstance(
                    document,
                    outputStream
            );


            document.open();


            /*
             * Header
             */

            Paragraph title =
                    new Paragraph(
                            "IRIFAMS"
                    );

            title.setAlignment(
                    Paragraph.ALIGN_CENTER
            );

            document.add(title);


            Paragraph subtitle =
                    new Paragraph(
                            "PAYMENT RECEIPT"
                    );

            subtitle.setAlignment(
                    Paragraph.ALIGN_CENTER
            );

            document.add(subtitle);


            document.add(
                    new Paragraph(" ")
            );


            /*
             * Receipt information
             */

            PdfPTable table =
                    new PdfPTable(2);

            table.setWidthPercentage(100);


            addRow(
                    table,
                    "Receipt Number",
                    receiptNumber
            );

            addRow(
                    table,
                    "Request ID",
                    "#" + request.getId()
            );

            addRow(
                    table,
                    "Farmer",
                    request.getFarmer()
                            .getFullName()
            );

            addRow(
                    table,
                    "Service",
                    request.getServiceType()
                            .name()
            );

            addRow(
                    table,
                    "Control Number",
                    request.getControlNumber()
            );

            addRow(
                    table,
                    "Amount Paid",
                    "TZS "
                            + String.format(
                            "%,.2f",
                            request.getAmount()
                    )
            );


            document.add(table);


            document.add(
                    new Paragraph(" ")
            );


            Paragraph thankYou =
                    new Paragraph(
                            "Payment successfully verified. "
                                    + "Thank you for using IRIFAMS."
                    );

            thankYou.setAlignment(
                    Paragraph.ALIGN_CENTER
            );

            document.add(thankYou);


            document.close();


            return outputStream.toByteArray();


        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate payment receipt",
                    e
            );
        }
    }


    private void addRow(
            PdfPTable table,
            String label,
            String value
    ) {

        table.addCell(
                new PdfPCell(
                        new Phrase(label)
                )
        );

        table.addCell(
                new PdfPCell(
                        new Phrase(value)
                )
        );
    }
}