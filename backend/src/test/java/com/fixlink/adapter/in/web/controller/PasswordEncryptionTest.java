package com.fixlink.adapter.in.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncryptionTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("Kiểm tra mã hóa mật khẩu bằng BCrypt: Hash một chiều, có Salt ngẫu nhiên và so khớp thành công")
    void testPasswordBCryptHashing() {
        String rawPassword = "Password@123";

        // 1. Mã hóa lần 1
        String hash1 = passwordEncoder.encode(rawPassword);
        System.out.println(">>> Mật khẩu gốc: " + rawPassword);
        System.out.println(">>> Chuỗi băm BCrypt lần 1: " + hash1);

        // 2. Mã hóa lần 2 (cùng mật khẩu)
        String hash2 = passwordEncoder.encode(rawPassword);
        System.out.println(">>> Chuỗi băm BCrypt lần 2: " + hash2);

        // Kiểm tra tiền tố chuẩn của BCrypt
        assertTrue(hash1.startsWith("$2a$") || hash1.startsWith("$2b$"), "Chuỗi băm phải có tiền tố BCrypt $2a$");
        assertEquals(60, hash1.length(), "Độ dài chuỗi băm BCrypt phải luôn là 60 ký tự");

        // Nhờ Salt ngẫu nhiên, cùng 1 mật khẩu băm 2 lần sẽ ra 2 chuỗi khác nhau hoàn toàn
        assertNotEquals(hash1, hash2, "Hai lần băm cùng một mật khẩu phải sinh ra hai chuỗi khác nhau nhờ Salt ngẫu nhiên");

        // Kiểm tra hàm matches() xác thực đúng mật khẩu
        assertTrue(passwordEncoder.matches(rawPassword, hash1), "Mật khẩu đúng phải so khớp thành công với hash1");
        assertTrue(passwordEncoder.matches(rawPassword, hash2), "Mật khẩu đúng phải so khớp thành công với hash2");

        // Kiểm tra mật khẩu sai không thể khớp
        assertFalse(passwordEncoder.matches("WrongPassword!999", hash1), "Mật khẩu sai không được phép khớp");
        System.out.println(">>> KẾT QUẢ KIỂM THỬ: Mã hóa BCrypt hoạt động chính xác 100%!");
    }
}
