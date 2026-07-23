package com.mobilestore.mobile_store.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.mobilestore.mobile_store.entity.Order;
import com.mobilestore.mobile_store.entity.OrderItem;
import com.mobilestore.mobile_store.service.PdfService;

@Service
public class PdfServiceImpl implements PdfService {

    @Override
    public byte[] generateInvoicePdf(Order order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // A4 page with margins
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(242, 118, 42)); // TechPulse Orange
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(107, 107, 104));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(23, 23, 23));
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(23, 23, 23));
            Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(107, 107, 104));
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(107, 107, 104));
            
            // Header table (Logo / Brand on left, INVOICE on right)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});
            
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.addElement(new Paragraph("TechPulse", titleFont));
            Paragraph tag = new Paragraph("TRUSTED MOBILE STORE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(107, 107, 104)));
            logoCell.addElement(tag);
            headerTable.addCell(logoCell);
            
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph invTitle = new Paragraph("INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(23, 23, 23)));
            invTitle.setAlignment(Element.ALIGN_RIGHT);
            titleCell.addElement(invTitle);
            Paragraph ordNum = new Paragraph("Order #" + order.getOrderNumber(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(107, 107, 104)));
            ordNum.setAlignment(Element.ALIGN_RIGHT);
            titleCell.addElement(ordNum);
            headerTable.addCell(titleCell);
            
            headerTable.setComplete(true);
            document.add(headerTable);
            document.add(new Paragraph("\n"));
            
            // Horizontal rule
            PdfPTable hrTable = new PdfPTable(1);
            hrTable.setWidthPercentage(100);
            PdfPCell hrCell = new PdfPCell();
            hrCell.setBorder(Rectangle.BOTTOM);
            hrCell.setBorderWidth(1.5f);
            hrCell.setBorderColor(new Color(234, 234, 234));
            hrCell.setFixedHeight(1);
            hrTable.addCell(hrCell);
            hrTable.setComplete(true);
            document.add(hrTable);
            document.add(new Paragraph("\n"));
            
            // Details grid (Billed To on left, Shipping Details on right)
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{50, 50});
            
            // Billed To Column
            PdfPCell billedToCell = new PdfPCell();
            billedToCell.setBorder(Rectangle.NO_BORDER);
            billedToCell.addElement(new Paragraph("BILLED TO", sectionTitleFont));
            billedToCell.addElement(new Paragraph(order.getCustomer().getName(), boldFont));
            billedToCell.addElement(new Paragraph(order.getCustomer().getEmail(), mutedFont));
            billedToCell.addElement(new Paragraph(order.getCustomer().getPhone(), mutedFont));
            detailsTable.addCell(billedToCell);
            
            // Shipping Details Column
            PdfPCell shipToCell = new PdfPCell();
            shipToCell.setBorder(Rectangle.NO_BORDER);
            shipToCell.addElement(new Paragraph("SHIPPING DETAILS", sectionTitleFont));
            shipToCell.addElement(new Paragraph(order.getCustomer().getAddress(), bodyFont));
            shipToCell.addElement(new Paragraph(order.getCustomer().getCity() + ", " + order.getCustomer().getPostalCode(), bodyFont));
            shipToCell.addElement(new Paragraph(order.getCustomer().getCountry(), bodyFont));
            shipToCell.addElement(new Paragraph("Method: " + order.getShippingMethod().toUpperCase(), mutedFont));
            detailsTable.addCell(shipToCell);
            
            detailsTable.setComplete(true);
            document.add(detailsTable);
            document.add(new Paragraph("\n"));
            
            // Info Row (Invoice Date & Order Status)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});
            
            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.addElement(new Paragraph("INVOICE DATE", sectionTitleFont));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            dateCell.addElement(new Paragraph(order.getCreatedAt().format(formatter), bodyFont));
            infoTable.addCell(dateCell);
            
            PdfPCell statusCell = new PdfPCell();
            statusCell.setBorder(Rectangle.NO_BORDER);
            statusCell.addElement(new Paragraph("ORDER STATUS", sectionTitleFont));
            statusCell.addElement(new Paragraph(order.getStatus().name(), boldFont));
            infoTable.addCell(statusCell);
            
            infoTable.setComplete(true);
            document.add(infoTable);
            document.add(new Paragraph("\n\n"));
            
            // Items Table
            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{30, 18, 22, 10, 20});
            
            // Table headers
            String[] headers = {"DESCRIPTION", "VARIANT", "UNIT PRICE", "QTY", "AMOUNT"};
            for (int i = 0; i < headers.length; i++) {
                PdfPCell cell = new PdfPCell(new Paragraph(headers[i], tableHeaderFont));
                cell.setBackgroundColor(new Color(248, 248, 247));
                cell.setPadding(8);
                cell.setBorder(Rectangle.BOTTOM);
                cell.setBorderWidth(1f);
                cell.setBorderColor(new Color(234, 234, 234));
                if (i >= 2) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                itemsTable.addCell(cell);
            }
            
            // Table rows
            for (OrderItem item : order.getItems()) {
                // Description
                PdfPCell descCell = new PdfPCell(new Paragraph(item.getProductName(), bodyFont));
                descCell.setPadding(10);
                descCell.setBorder(Rectangle.BOTTOM);
                descCell.setBorderColor(new Color(234, 234, 234));
                itemsTable.addCell(descCell);
                
                // Variant
                PdfPCell varCell = new PdfPCell(new Paragraph(item.getVariantColor(), mutedFont));
                varCell.setPadding(10);
                varCell.setBorder(Rectangle.BOTTOM);
                varCell.setBorderColor(new Color(234, 234, 234));
                itemsTable.addCell(varCell);
                
                // Unit Price
                PdfPCell priceCell = new PdfPCell(new Paragraph("Rs. " + String.format("%,d", item.getUnitPrice().longValue()), bodyFont));
                priceCell.setPadding(10);
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                priceCell.setBorder(Rectangle.BOTTOM);
                priceCell.setBorderColor(new Color(234, 234, 234));
                itemsTable.addCell(priceCell);
                
                // Qty
                PdfPCell qtyCell = new PdfPCell(new Paragraph(String.valueOf(item.getQuantity()), bodyFont));
                qtyCell.setPadding(10);
                qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qtyCell.setBorder(Rectangle.BOTTOM);
                qtyCell.setBorderColor(new Color(234, 234, 234));
                itemsTable.addCell(qtyCell);
                
                // Amount
                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                PdfPCell amtCell = new PdfPCell(new Paragraph("Rs. " + String.format("%,d", amount.longValue()), bodyFont));
                amtCell.setPadding(10);
                amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                amtCell.setBorder(Rectangle.BOTTOM);
                amtCell.setBorderColor(new Color(234, 234, 234));
                itemsTable.addCell(amtCell);
            }
            
            itemsTable.setComplete(true);
            document.add(itemsTable);
            document.add(new Paragraph("\n"));
            
            // Summary alignment table
            PdfPTable summaryAlign = new PdfPTable(2);
            summaryAlign.setWidthPercentage(100);
            summaryAlign.setWidths(new float[]{60, 40});
            
            // Left column (Customer instructions/blank)
            PdfPCell instructionsCell = new PdfPCell();
            instructionsCell.setBorder(Rectangle.NO_BORDER);
            if (order.getInstructions() != null && !order.getInstructions().trim().isEmpty()) {
                instructionsCell.addElement(new Paragraph("CUSTOMER INSTRUCTIONS", sectionTitleFont));
                instructionsCell.addElement(new Paragraph(order.getInstructions(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, new Color(23, 23, 23))));
            }
            summaryAlign.addCell(instructionsCell);
            
            // Right column (Pricing breakdown)
            PdfPCell breakdownCell = new PdfPCell();
            breakdownCell.setBorder(Rectangle.NO_BORDER);
            
            PdfPTable breakdownTable = new PdfPTable(2);
            breakdownTable.setWidthPercentage(100);
            breakdownTable.setWidths(new float[]{50, 50});
            
            // Subtotal
            breakdownTable.addCell(getNoBorderCell("Subtotal", mutedFont, Element.ALIGN_LEFT));
            breakdownTable.addCell(getNoBorderCell("Rs. " + String.format("%,d", order.getSubtotal().longValue()), bodyFont, Element.ALIGN_RIGHT));
            
            // Shipping Cost
            breakdownTable.addCell(getNoBorderCell("Shipping Cost", mutedFont, Element.ALIGN_LEFT));
            breakdownTable.addCell(getNoBorderCell("Rs. " + String.format("%,d", order.getShippingCost().longValue()), bodyFont, Element.ALIGN_RIGHT));
            
            // Discount Applied
            BigDecimal calculatedDiscount = order.getSubtotal().add(order.getShippingCost()).subtract(order.getTotal());
            if (calculatedDiscount.compareTo(BigDecimal.ZERO) > 0) {
                breakdownTable.addCell(getNoBorderCell("Discount Applied", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(16, 185, 129)), Element.ALIGN_LEFT));
                breakdownTable.addCell(getNoBorderCell("-Rs. " + String.format("%,d", calculatedDiscount.longValue()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(16, 185, 129)), Element.ALIGN_RIGHT));
            }
            
            // VAT (15% Included)
            BigDecimal taxableAmount = order.getTotal().subtract(order.getShippingCost()).max(BigDecimal.ZERO);
            BigDecimal vat = taxableAmount.multiply(BigDecimal.valueOf(15)).divide(BigDecimal.valueOf(115), 2, java.math.RoundingMode.HALF_UP);
            breakdownTable.addCell(getNoBorderCell("VAT (15% Included)", mutedFont, Element.ALIGN_LEFT));
            breakdownTable.addCell(getNoBorderCell("Rs. " + String.format("%,d", vat.longValue()), mutedFont, Element.ALIGN_RIGHT));
            
            // Total (double line top border)
            PdfPCell totalLabelCell = new PdfPCell(new Paragraph("Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(23, 23, 23))));
            totalLabelCell.setBorder(Rectangle.TOP);
            totalLabelCell.setBorderWidth(1.5f);
            totalLabelCell.setPaddingTop(8);
            breakdownTable.addCell(totalLabelCell);
            
            PdfPCell totalValCell = new PdfPCell(new Paragraph("Rs. " + String.format("%,d", order.getTotal().longValue()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(242, 118, 42))));
            totalValCell.setBorder(Rectangle.TOP);
            totalValCell.setBorderWidth(1.5f);
            totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValCell.setPaddingTop(8);
            breakdownTable.addCell(totalValCell);
            
            breakdownTable.setComplete(true);
            breakdownCell.addElement(breakdownTable);
            summaryAlign.addCell(breakdownCell);
            
            summaryAlign.setComplete(true);
            document.add(summaryAlign);
            
            // Footer
            Paragraph finalFooter = new Paragraph("\n\n\nThank you for shopping with TechPulse!\nIf you have any questions about this invoice, contact us at support@techpulse.lk", FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(107, 107, 104)));
            finalFooter.setAlignment(Element.ALIGN_CENTER);
            document.add(finalFooter);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
        
        return out.toByteArray();
    }
    
    private PdfPCell getNoBorderCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}
