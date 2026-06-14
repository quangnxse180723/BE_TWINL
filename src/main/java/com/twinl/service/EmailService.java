package com.twinl.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực OTP đăng ký Twinl Secondhand");
        message.setText("Xin chào,\n\nMã xác thực OTP của bạn là: " + otpCode + "\n\nMã này sẽ hết hạn trong 5 phút.\nCảm ơn bạn đã sử dụng Twinl Secondhand!");
        
        mailSender.send(message);
    }
}
