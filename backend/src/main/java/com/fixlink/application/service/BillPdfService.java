package com.fixlink.application.service;

import com.fixlink.adapter.out.persistence.entity.BillJpaEntity;
import com.fixlink.domain.model.BillType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BillPdfService {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String generateBillHtml(BillJpaEntity bill) {
        boolean isFinal = bill.getBillType() == BillType.FINAL;

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <title>Hóa đơn %s</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: 'Segoe UI', Arial, sans-serif; padding: 40px; max-width: 800px; margin: auto; color: #222; }
                    .header { text-align: center; border-bottom: 3px solid #1a73e8; padding-bottom: 20px; margin-bottom: 30px; }
                    .header h1 { color: #1a73e8; font-size: 28px; margin-bottom: 5px; }
                    .header .bill-number { color: #666; font-size: 14px; }
                    .header .bill-type { display: inline-block; background: %s; color: white; padding: 4px 16px; border-radius: 20px; font-size: 13px; margin-top: 8px; }
                    .section { margin-bottom: 24px; }
                    .section h3 { color: #1a73e8; font-size: 16px; margin-bottom: 10px; border-bottom: 1px solid #e0e0e0; padding-bottom: 6px; }
                    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 30px; }
                    .info-item label { color: #888; font-size: 12px; display: block; }
                    .info-item span { font-size: 14px; font-weight: 500; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 10px; }
                    th { background: #f0f7ff; text-align: left; padding: 10px 12px; font-size: 13px; color: #444; border-bottom: 2px solid #1a73e8; }
                    td { padding: 10px 12px; font-size: 14px; border-bottom: 1px solid #eee; }
                    td.amount { text-align: right; font-weight: 600; }
                    .total-row td { font-size: 18px; font-weight: 700; color: #1a73e8; border-top: 2px solid #1a73e8; }
                    .footer { text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #999; font-size: 12px; }
                    @media print { body { padding: 20px; } }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>FIXLINK</h1>
                    <p class="bill-number">%s</p>
                    <span class="bill-type">%s</span>
                </div>
                <div class="section">
                    <h3>Thong tin don hang</h3>
                    <div class="info-grid">
                        <div class="info-item"><label>Ma don</label><span>#%d</span></div>
                        <div class="info-item"><label>Ngay xuat</label><span>%s</span></div>
                        <div class="info-item"><label>Danh muc</label><span>%s</span></div>
                        <div class="info-item"><label>Mo ta</label><span>%s</span></div>
                    </div>
                </div>
                <div class="section">
                    <h3>Khach hang</h3>
                    <div class="info-grid">
                        <div class="info-item"><label>Ho ten</label><span>%s</span></div>
                        <div class="info-item"><label>SDT</label><span>%s</span></div>
                        <div class="info-item"><label>Dia chi</label><span>%s</span></div>
                    </div>
                </div>
                <div class="section">
                    <h3>Ky thuat vien</h3>
                    <div class="info-grid">
                        <div class="info-item"><label>Ho ten</label><span>%s</span></div>
                        <div class="info-item"><label>SDT</label><span>%s</span></div>
                        <div class="info-item"><label>Hinh thuc</label><span>%s</span></div>
                    </div>
                </div>
                <div class="section">
                    <h3>Chi tiet thanh toan</h3>
                    <table>
                        <thead><tr><th>Hang muc</th><th style="text-align:right">Thanh tien</th></tr></thead>
                        <tbody>
                            <tr><td>Gia bao (deal)</td><td class="amount">%s VND</td></tr>
                            %s
                            %s
                            <tr class="total-row"><td><strong>%s</strong></td><td class="amount">%s VND</td></tr>
                        </tbody>
                    </table>
                </div>
                <div class="footer">
                    <p>FixLink — Nen tang ket noi dich vu sua chua</p>
                    <p>Hoa don nay duoc tao tu dong boi he thong FixLink</p>
                </div>
                <script>
                    window.addEventListener('DOMContentLoaded', function() {
                        setTimeout(function() { window.print(); }, 400);
                    });
                </script>
            </body>
            </html>
            """.formatted(
                bill.getBillNumber(),
                isFinal ? "#2e7d32" : "#1a73e8",
                bill.getBillNumber(),
                isFinal ? "HOA DON TONG" : "HOA DON DAT COC",
                bill.getRequestId(),
                bill.getCreatedAt() != null ? bill.getCreatedAt().format(DATE_FMT) : "—",
                nvl(bill.getServiceCategory()),
                nvl(bill.getServiceDescription()),
                nvl(bill.getCustomerName()),
                nvl(bill.getCustomerPhone()),
                nvl(bill.getCustomerAddress()),
                nvl(bill.getTechnicianName()),
                nvl(bill.getTechnicianPhone()),
                "CASH".equalsIgnoreCase(bill.getPaymentMethod()) ? "Tien mat truc tiep cho tho" : "Chuyen khoan VietQR",
                formatVnd(bill.getQuotedPrice()),
                isFinal ? "<tr><td>Da dat coc</td><td class=\"amount\">-" + formatVnd(bill.getDepositAmount()) + " VND</td></tr>" : "",
                bill.getAdditionalCost() != null && bill.getAdditionalCost().compareTo(BigDecimal.ZERO) > 0
                        ? "<tr><td>Chi phi phat sinh</td><td class=\"amount\">" + formatVnd(bill.getAdditionalCost()) + " VND</td></tr>" : "",
                isFinal ? "TONG THANH TOAN" : "TIEN DAT COC (30%)",
                formatVnd(bill.getTotalAmount())
        );
    }

    private String formatVnd(BigDecimal amount) {
        return VND.format(amount != null ? amount : BigDecimal.ZERO);
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }
}
