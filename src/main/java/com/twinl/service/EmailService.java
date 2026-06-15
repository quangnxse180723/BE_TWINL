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

    public void sendForgotPasswordOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực Khôi phục mật khẩu Twinl Secondhand");
        message.setText("Xin chào,\n\nMã xác thực OTP để khôi phục mật khẩu của bạn là: " + otpCode + "\n\nMã này sẽ hết hạn trong 5 phút. KHÔNG CHIA SẺ MÃ NÀY CHO BẤT KỲ AI.\nCảm ơn bạn đã sử dụng Twinl Secondhand!");
        
        mailSender.send(message);
    }
    public void sendContactConfirmation(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Xác nhận gửi thông tin liên hệ - Twinl Secondhand");
        message.setText("Xin chào " + name + ",\n\nCảm ơn bạn đã liên hệ với Twinl Secondhand. Chúng tôi đã nhận được tin nhắn của bạn và sẽ phản hồi trong thời gian sớm nhất.\n\nTrân trọng,\nĐội ngũ Twinl Secondhand");
        
        mailSender.send(message);
    }

    public void sendContactNotificationToAdmin(String name, String email, String phone, String userMessage) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("twinl2hand@gmail.com");
        message.setSubject("Có liên hệ mới từ khách hàng: " + name);
        message.setText("Hệ thống vừa nhận được một liên hệ mới:\n\n- Họ tên: " + name + "\n- Email: " + email + "\n- Số điện thoại: " + (phone != null ? phone : "Không có") + "\n- Lời nhắn:\n" + userMessage + "\n\nVui lòng kiểm tra và phản hồi khách hàng.");
        
        mailSender.send(message);
    }
}
