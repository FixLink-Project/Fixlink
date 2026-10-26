package com.fixlink.application.service;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class VietQRService {

    private static final String BANK_ID = "970436";
    private static final String ACCOUNT_NO = "1051236810";
    private static final String ACCOUNT_NAME = "PHAN TA HUU DUC";
    private static final String TEMPLATE = "compact2";
    private static final String VIETQR_BASE = "https://img.vietqr.io/image";

    public String generateQRUrl(long amount, String content) {
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8);
        return String.format("%s/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                VIETQR_BASE, BANK_ID, ACCOUNT_NO, TEMPLATE,
                amount, encodedContent, encodedName);
    }

    public String generateTransferContent(Long requestId, boolean isDeposit) {
        return String.format("FL-%d-%s", requestId, isDeposit ? "DC" : "TT");
    }

    public BankInfo getBankInfo() {
        return new BankInfo(BANK_ID, ACCOUNT_NO, ACCOUNT_NAME, "Vietcombank");
    }

    public record BankInfo(String bankId, String accountNo, String accountName, String bankName) {}
}
