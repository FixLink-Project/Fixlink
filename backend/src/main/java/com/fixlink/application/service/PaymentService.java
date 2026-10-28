package com.fixlink.application.service;

import com.fixlink.adapter.out.persistence.entity.BillJpaEntity;
import com.fixlink.adapter.out.persistence.entity.PaymentJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.repository.*;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SpringDataPaymentRepository paymentRepo;
    private final SpringDataBillRepository billRepo;
    private final SpringDataRepairRequestRepository requestRepo;
    private final SpringDataCustomerProfileRepository customerProfileRepo;
    private final SpringDataTechnicianProfileRepository techProfileRepo;
    private final SpringDataServiceCategoryRepository categoryRepo;
    private final AdditionalCostService additionalCostService;
    private final VietQRService vietQRService;
    private final com.fixlink.application.port.in.NotificationUseCase notificationUseCase;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResult createDepositPayment(Long requestId, Long userId) {
        RepairRequestJpaEntity request = findRequest(requestId);
        verifyCustomerOwnership(request, userId);

        Optional<PaymentJpaEntity> existingDeposit = paymentRepo
                .findByRequestIdAndPaymentTypeAndDeletedAtIsNull(requestId, PaymentType.DEPOSIT);
        if (existingDeposit.isPresent()) {
            PaymentJpaEntity payment = existingDeposit.get();
            return new PaymentResult(payment.getId(), payment.getQrUrl(), payment.getAmount(),
                    payment.getTransferContent(), vietQRService.getBankInfo());
        }

        if (request.getStatus() != RequestStatus.ASSIGNED) {
            throw new DomainException("PAYMENT_INVALID_STATUS",
                    "Yêu cầu phải ở trạng thái đã giao thợ để tạo đặt cọc.", 409);
        }

        BigDecimal agreedPrice = request.getAgreedPrice();
        if (agreedPrice == null || agreedPrice.signum() <= 0) {
            throw new DomainException("PAYMENT_NO_PRICE",
                    "Chưa có giá thỏa thuận để tính tiền cọc.", 409);
        }

        BigDecimal depositAmount = agreedPrice.multiply(BigDecimal.valueOf(0.3))
                .setScale(0, RoundingMode.HALF_UP);

        String transferContent = vietQRService.generateTransferContent(requestId, true);
        String qrUrl = vietQRService.generateQRUrl(depositAmount.longValue(), transferContent);

        PaymentJpaEntity payment = PaymentJpaEntity.builder()
                .requestId(requestId)
                .paymentType(PaymentType.DEPOSIT)
                .amount(depositAmount)
                .transferContent(transferContent)
                .qrUrl(qrUrl)
                .build();
        payment.setCreatedBy(userId);
        paymentRepo.save(payment);

        return new PaymentResult(payment.getId(), qrUrl, depositAmount,
                transferContent, vietQRService.getBankInfo());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResult createFinalPayment(Long requestId, Long userId) {
        RepairRequestJpaEntity request = findRequest(requestId);
        verifyCustomerOwnership(request, userId);

        if (request.getStatus() != RequestStatus.AWAITING_ACCEPTANCE) {
            throw new DomainException("PAYMENT_INVALID_STATUS",
                    "Yêu cầu phải ở trạng thái chờ nghiệm thu để tạo thanh toán tổng.", 409);
        }

        BigDecimal depositAmount = paymentRepo
                .findByRequestIdAndPaymentTypeAndDeletedAtIsNull(requestId, PaymentType.DEPOSIT)
                .filter(d -> d.getStatus() == PaymentStatus.CONFIRMED)
                .map(PaymentJpaEntity::getAmount)
                .orElse(request.getDepositAmount() != null ? request.getDepositAmount() : BigDecimal.ZERO);

        Optional<PaymentJpaEntity> existingFinal = paymentRepo
                .findByRequestIdAndPaymentTypeAndDeletedAtIsNull(requestId, PaymentType.FINAL);
        if (existingFinal.isPresent()) {
            PaymentJpaEntity payment = existingFinal.get();
            return new PaymentResult(payment.getId(), payment.getQrUrl(), payment.getAmount(),
                    payment.getTransferContent(), vietQRService.getBankInfo());
        }

        BigDecimal additionalTotal = additionalCostService.getApprovedTotalByRequest(requestId);
        BigDecimal finalAmount = request.getAgreedPrice()
                .add(additionalTotal)
                .subtract(depositAmount);

        if (finalAmount.signum() < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        String transferContent = vietQRService.generateTransferContent(requestId, false);
        String qrUrl = vietQRService.generateQRUrl(finalAmount.longValue(), transferContent);

        PaymentJpaEntity payment = PaymentJpaEntity.builder()
                .requestId(requestId)
                .paymentType(PaymentType.FINAL)
                .amount(finalAmount)
                .transferContent(transferContent)
                .qrUrl(qrUrl)
                .build();
        payment.setCreatedBy(userId);
        paymentRepo.save(payment);

        return new PaymentResult(payment.getId(), qrUrl, finalAmount,
                transferContent, vietQRService.getBankInfo());
    }

    @Transactional
    public BillJpaEntity confirmPayment(Long paymentId, Long userId) {
        PaymentJpaEntity payment = paymentRepo.findById(paymentId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new DomainException("PAYMENT_NOT_FOUND",
                        "Không tìm thấy khoản thanh toán.", 404));

        RepairRequestJpaEntity request = findRequest(payment.getRequestId());
        verifyCustomerOwnership(request, userId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new DomainException("PAYMENT_NOT_PENDING",
                    "Khoản thanh toán không ở trạng thái chờ xác nhận.", 409);
        }

        payment.confirm();
        payment.setUpdatedBy(userId);
        paymentRepo.save(payment);

        BillJpaEntity bill = generateBill(payment, userId);
        updateRequestStatusAfterPayment(payment, userId);

        String formattedAmount = payment.getAmount() != null ? String.format("%,dđ", payment.getAmount().longValue()) : "0đ";
        if (payment.getPaymentType() == PaymentType.DEPOSIT) {
            notificationUseCase.sendNotification(
                    request.getCustomerId(),
                    "Đặt cọc thành công!",
                    "Bạn đã thanh toán cọc bảo chứng " + formattedAmount + " cho yêu cầu #" + request.getId() + ". Thợ sẽ đến sửa chữa theo lịch hẹn.",
                    "DEPOSIT_PAID",
                    String.valueOf(request.getId()),
                    "/yeu-cau/" + request.getId()
            );
            if (request.getTechnicianId() != null) {
                notificationUseCase.sendNotification(
                        request.getTechnicianId(),
                        "Khách hàng đã đặt cọc tiền bảo chứng!",
                        "Khách hàng đã nộp cọc " + formattedAmount + " cho yêu cầu #" + request.getId() + ". Hãy chuẩn bị dụng cụ đến sửa chữa đúng hẹn!",
                        "DEPOSIT_PAID",
                        String.valueOf(request.getId()),
                        "/tho/yeu-cau/" + request.getId()
                );
            }
        } else if (payment.getPaymentType() == PaymentType.FINAL) {
            notificationUseCase.sendNotification(
                    request.getCustomerId(),
                    "Thanh toán tổng thành công!",
                    "Bạn đã thanh toán thành công " + formattedAmount + " cho yêu cầu #" + request.getId() + ". Hóa đơn điện tử " + bill.getBillNumber() + " đã được xuất.",
                    "FINAL_PAYMENT_PAID",
                    String.valueOf(request.getId()),
                    "/yeu-cau/" + request.getId()
            );
            if (request.getTechnicianId() != null) {
                notificationUseCase.sendNotification(
                        request.getTechnicianId(),
                        "Khách hàng đã thanh toán tổng!",
                        "Khách hàng đã thanh toán " + formattedAmount + " cho yêu cầu #" + request.getId() + ". Tiền công đã được ghi nhận vào ví của bạn!",
                        "FINAL_PAYMENT_PAID",
                        String.valueOf(request.getId()),
                        "/tho/yeu-cau/" + request.getId()
                );
            }
        }

        return bill;
    }

    @Transactional(readOnly = true)
    public List<PaymentJpaEntity> getPaymentsByRequest(Long requestId) {
        return paymentRepo.findByRequestIdAndDeletedAtIsNull(requestId);
    }

    @Transactional(readOnly = true)
    public List<BillJpaEntity> getBillsByRequest(Long requestId) {
        return billRepo.findByRequestIdAndDeletedAtIsNull(requestId);
    }

    @Transactional(readOnly = true)
    public BillJpaEntity getBillById(Long billId) {
        return billRepo.findById(billId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new DomainException("BILL_NOT_FOUND",
                        "Không tìm thấy hóa đơn.", 404));
    }

    private BillJpaEntity generateBill(PaymentJpaEntity payment, Long userId) {
        RepairRequestJpaEntity request = findRequest(payment.getRequestId());
        BigDecimal additionalTotal = additionalCostService.getApprovedTotalByRequest(payment.getRequestId());

        BigDecimal depositAmount = BigDecimal.ZERO;
        if (payment.getPaymentType() == PaymentType.FINAL) {
            depositAmount = paymentRepo
                    .findByRequestIdAndPaymentTypeAndDeletedAtIsNull(payment.getRequestId(), PaymentType.DEPOSIT)
                    .map(PaymentJpaEntity::getAmount)
                    .orElse(BigDecimal.ZERO);
        }

        String customerName = customerProfileRepo.findByUserIdAndDeletedAtIsNull(request.getCustomerId())
                .map(p -> p.getFullName()).orElse("—");
        String customerPhone = customerProfileRepo.findByUserIdAndDeletedAtIsNull(request.getCustomerId())
                .map(p -> p.getPhone()).orElse("—");
        String techName = request.getTechnicianId() != null
                ? techProfileRepo.findByUserIdAndDeletedAtIsNull(request.getTechnicianId())
                    .map(p -> p.getFullName()).orElse("—") : "—";
        String techPhone = request.getTechnicianId() != null
                ? techProfileRepo.findByUserIdAndDeletedAtIsNull(request.getTechnicianId())
                    .map(p -> p.getPhone()).orElse("—") : "—";
        String categoryName = categoryRepo.findById(request.getCategoryId())
                .map(c -> c.getName()).orElse("—");

        BillJpaEntity bill = BillJpaEntity.builder()
                .billNumber(generateBillNumber())
                .paymentId(payment.getId())
                .billType(payment.getPaymentType() == PaymentType.DEPOSIT
                        ? BillType.DEPOSIT : BillType.FINAL)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerAddress(request.getAddress())
                .technicianName(techName)
                .technicianPhone(techPhone)
                .serviceCategory(categoryName)
                .serviceDescription(request.getDescription())
                .requestId(payment.getRequestId())
                .quotedPrice(request.getAgreedPrice() != null ? request.getAgreedPrice() : BigDecimal.ZERO)
                .depositAmount(depositAmount)
                .additionalCost(additionalTotal)
                .totalAmount(payment.getAmount())
                .build();
        bill.setCreatedBy(userId);
        return billRepo.save(bill);
    }

    private String generateBillNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = billRepo.nextDailySequence();
        return String.format("FL-%s-%04d", date, seq);
    }

    private void updateRequestStatusAfterPayment(PaymentJpaEntity payment, Long userId) {
        RepairRequestJpaEntity request = findRequest(payment.getRequestId());
        if (payment.getPaymentType() == PaymentType.DEPOSIT) {
            request.setStatus(RequestStatus.DEPOSIT_PAID);
            request.setDepositAmount(payment.getAmount());
        }
        // Final payment không tự chuyển COMPLETED — khách phải nghiệm thu trước
        request.setUpdatedBy(userId);
        requestRepo.save(request);
    }

    private void verifyCustomerOwnership(RepairRequestJpaEntity request, Long userId) {
        if (!request.getCustomerId().equals(userId)) {
            throw new DomainException("PAYMENT_UNAUTHORIZED",
                    "Bạn không có quyền thao tác thanh toán cho yêu cầu này.", 403);
        }
    }

    private RepairRequestJpaEntity findRequest(Long requestId) {
        return requestRepo.findById(requestId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new DomainException("REQUEST_NOT_FOUND",
                        "Không tìm thấy yêu cầu sửa chữa.", 404));
    }

    public record PaymentResult(
            Long paymentId,
            String qrUrl,
            BigDecimal amount,
            String transferContent,
            VietQRService.BankInfo bankInfo
    ) {}
}
