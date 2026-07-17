package com.twinl.service.impl;

import com.twinl.entity.Order;
import com.twinl.entity.User;
import com.twinl.entity.Wallet;
import com.twinl.entity.WalletTransaction;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.UserRepository;
import com.twinl.repository.WalletRepository;
import com.twinl.repository.WalletTransactionRepository;
import com.twinl.service.WalletService;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.twinl.dto.request.BankUpdateRequest;
import com.twinl.dto.response.SellerStatisticsResponse;
import com.twinl.dto.response.WalletResponse;
import com.twinl.dto.response.WalletTransactionResponse;

@Service
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final com.twinl.repository.ProductRepository productRepository;
    private final com.twinl.service.NotificationService notificationService;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletTransactionRepository transactionRepository,
                             UserRepository userRepository,
                             OrderRepository orderRepository,
                             com.twinl.repository.ProductRepository productRepository,
                             com.twinl.service.NotificationService notificationService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    private Wallet getAdminWallet() {
        // Giả sử lấy user đầu tiên làm admin (hoặc có role ADMIN)
        List<User> users = userRepository.findAll();
        User admin = users.stream()
            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().name().equals("ADMIN")))
            .findFirst()
            .orElse(users.isEmpty() ? null : users.get(0));
            
        if (admin == null) return null;

        return walletRepository.findByUserId(admin.getId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(admin)
                            .balance(BigDecimal.ZERO)
                            .escrowBalance(BigDecimal.ZERO)
                            .totalCommission(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    private Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .escrowBalance(BigDecimal.ZERO)
                            .totalCommission(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Override
    @Transactional
    public void holdEscrow(Order order) {
        for (var item : order.getItems()) {
            if (item.getProduct() != null && item.getProduct().getSeller() != null) {
                User seller = item.getProduct().getSeller();
                Wallet wallet = getOrCreateWallet(seller);
                
                BigDecimal platformFee = item.getLineTotal().multiply(new BigDecimal("0.10"));
                BigDecimal sellerAmount = item.getLineTotal().subtract(platformFee);
                
                wallet.setEscrowBalance(wallet.getEscrowBalance().add(sellerAmount));
                walletRepository.save(wallet);

                WalletTransaction txn = WalletTransaction.builder()
                        .wallet(wallet)
                        .amount(sellerAmount)
                        .type("ESCROW_HOLD")
                        .status("SUCCESS")
                        .description("Tạm giữ tiền chờ thanh toán cho đơn " + order.getCode())
                        .order(order)
                        .build();
                transactionRepository.save(txn);
            }
        }
    }

    @Override
    @Transactional
    public void releaseEscrow(Order order) {
        for (var item : order.getItems()) {
            if (item.getProduct() != null && item.getProduct().getSeller() != null) {
                User seller = item.getProduct().getSeller();
                Wallet wallet = getOrCreateWallet(seller);
                
                BigDecimal platformFee = item.getLineTotal().multiply(new BigDecimal("0.10"));
                BigDecimal sellerAmount = item.getLineTotal().subtract(platformFee);
                
                if (wallet.getEscrowBalance().compareTo(sellerAmount) >= 0) {
                    wallet.setEscrowBalance(wallet.getEscrowBalance().subtract(sellerAmount));
                }
                
                wallet.setBalance(wallet.getBalance().add(sellerAmount));
                walletRepository.save(wallet);

                WalletTransaction txn = WalletTransaction.builder()
                        .wallet(wallet)
                        .amount(sellerAmount)
                        .type("ESCROW_RELEASE")
                        .status("SUCCESS")
                        .description("Nhận tiền thanh toán từ đơn " + order.getCode())
                        .order(order)
                        .build();
                transactionRepository.save(txn);
            }
        }
    }

    @Override
    @Transactional
    public void refundBuyer(Order order) {
        if (Boolean.TRUE.equals(order.getEscrowReleased())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Escrow already released, cannot refund");
        }

        // 1. Deduct from seller's escrow
        for (var item : order.getItems()) {
            if (item.getProduct() != null && item.getProduct().getSeller() != null) {
                User seller = item.getProduct().getSeller();
                Wallet sellerWallet = getOrCreateWallet(seller);
                
                BigDecimal platformFee = item.getLineTotal().multiply(new BigDecimal("0.10"));
                BigDecimal sellerAmount = item.getLineTotal().subtract(platformFee);
                
                if (sellerWallet.getEscrowBalance().compareTo(sellerAmount) >= 0) {
                    sellerWallet.setEscrowBalance(sellerWallet.getEscrowBalance().subtract(sellerAmount));
                } else {
                    sellerWallet.setEscrowBalance(BigDecimal.ZERO);
                }
                walletRepository.save(sellerWallet);

                WalletTransaction txn = WalletTransaction.builder()
                        .wallet(sellerWallet)
                        .amount(sellerAmount.negate())
                        .type("ESCROW_REFUND_DEDUCT")
                        .status("SUCCESS")
                        .description("Thu hồi tiền chờ thanh toán do đơn " + order.getCode() + " hoàn trả")
                        .order(order)
                        .build();
                transactionRepository.save(txn);
            }
        }

        // 2. Add total amount to buyer's available balance
        User buyer = order.getUser();
        Wallet buyerWallet = getOrCreateWallet(buyer);
        buyerWallet.setBalance(buyerWallet.getBalance().add(order.getTotalAmount()));
        walletRepository.save(buyerWallet);

        WalletTransaction refundTxn = WalletTransaction.builder()
                .wallet(buyerWallet)
                .amount(order.getTotalAmount())
                .type("REFUND")
                .status("SUCCESS")
                .description("Hoàn tiền đơn hàng " + order.getCode())
                .order(order)
                .build();
        transactionRepository.save(refundTxn);
    }

    @Override
    @Transactional
    public void processCommission(Order order) {
        Wallet wallet = getAdminWallet();
        if (wallet == null) return;
        
        // 1.5% commission
        BigDecimal commission = order.getTotalAmount().multiply(new BigDecimal("0.015"));
        wallet.setTotalCommission(wallet.getTotalCommission().add(commission));
        
        if (wallet.getBalance().compareTo(commission) >= 0) {
            wallet.setBalance(wallet.getBalance().subtract(commission));
        }
        
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .amount(commission)
                .type("COMMISSION_FEE")
                .status("SUCCESS")
                .description("Thu phí sàn 1.5% cho đơn " + order.getCode())
                .order(order)
                .build();
        transactionRepository.save(txn);
    }

    @Override
    @Transactional
    public WalletResponse getMyWallet(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));
        
        Wallet wallet = getOrCreateWallet(user);
        
        List<WalletTransactionResponse> txns = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream().map(t -> WalletTransactionResponse.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .type(t.getType())
                        .status(t.getStatus())
                        .description(t.getDescription())
                        .createdAt(t.getCreatedAt())
                        .orderCode(t.getOrder() != null ? t.getOrder().getCode() : null)
                        .build())
                .collect(Collectors.toList());

        return WalletResponse.builder()
                .balance(wallet.getBalance())
                .escrowBalance(wallet.getEscrowBalance())
                .totalCommission(wallet.getTotalCommission())
                .bankName(wallet.getBankName())
                .bankAccountNumber(wallet.getBankAccountNumber())
                .bankAccountName(wallet.getBankAccountName())
                .transactions(txns)
                .build();
    }

    @Override
    @Transactional
    public void updateBankAccount(String username, BankUpdateRequest request) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));
        
        Wallet wallet = getOrCreateWallet(user);
        wallet.setBankName(request.getBankName());
        wallet.setBankAccountNumber(request.getBankAccountNumber());
        wallet.setBankAccountName(request.getBankAccountName());
        
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public SellerStatisticsResponse getSellerStatistics(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));

        Wallet wallet = getOrCreateWallet(user);
        
        long totalProducts = productRepository.findBySellerId(user.getId(), org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long totalOrders = orderRepository.findOrdersBySellerId(user.getId(), org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        
        return SellerStatisticsResponse.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(wallet.getBalance().add(wallet.getEscrowBalance()))
                .pendingEscrow(wallet.getEscrowBalance())
                .build();
    }

    @Override
    @Transactional
    public void requestWithdrawal(String username, BigDecimal amount) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));
        
        Wallet wallet = getOrCreateWallet(user);
        
        if (wallet.getBankName() == null || wallet.getBankAccountNumber() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng cập nhật thông tin ngân hàng trước khi rút tiền");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền rút không hợp lệ");
        }
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số dư không đủ để rút");
        }
        
        // Trừ tiền ngay khi tạo yêu cầu
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type("WITHDRAWAL")
                .status("PENDING")
                .description("Yêu cầu rút tiền về " + wallet.getBankName() + " (" + wallet.getBankAccountNumber() + ")")
                .build();
        transactionRepository.save(txn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.twinl.dto.response.WithdrawalRequestResponse> getAllWithdrawals() {
        return transactionRepository.findAll().stream()
                .filter(t -> "WITHDRAWAL".equals(t.getType()))
                .sorted(java.util.Comparator.comparing(WalletTransaction::getCreatedAt).reversed())
                .map(t -> com.twinl.dto.response.WithdrawalRequestResponse.builder()
                        .id(t.getId())
                        .sellerName(t.getWallet().getUser().getDisplayName())
                        .sellerEmail(t.getWallet().getUser().getEmail())
                        .amount(t.getAmount())
                        .bankName(t.getWallet().getBankName())
                        .bankAccountNumber(t.getWallet().getBankAccountNumber())
                        .bankAccountName(t.getWallet().getBankAccountName())
                        .createdAt(t.getCreatedAt())
                        .status(t.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveWithdrawal(Long transactionId) {
        WalletTransaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch"));
                
        if (!"WITHDRAWAL".equals(txn.getType()) || !"PENDING".equals(txn.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giao dịch không hợp lệ để duyệt");
        }
        
        txn.setStatus("SUCCESS");
        transactionRepository.save(txn);
        
        notificationService.sendNotification(
            txn.getWallet().getUser(),
            "Rút tiền thành công",
            "Yêu cầu rút " + txn.getAmount() + " VNĐ của bạn đã được duyệt và chuyển khoản thành công.",
            "WITHDRAWAL_SUCCESS"
        );
    }

    @Override
    @Transactional
    public void rejectWithdrawal(Long transactionId, String reason) {
        WalletTransaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch"));
                
        if (!"WITHDRAWAL".equals(txn.getType()) || !"PENDING".equals(txn.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giao dịch không hợp lệ để từ chối");
        }
        
        txn.setStatus("FAILED");
        txn.setDescription("Yêu cầu rút tiền bị từ chối: " + reason);
        transactionRepository.save(txn);
        
        notificationService.sendNotification(
            txn.getWallet().getUser(),
            "Rút tiền bị từ chối",
            "Yêu cầu rút " + txn.getAmount() + " VNĐ của bạn đã bị từ chối với lý do: " + reason,
            "WITHDRAWAL_FAILED"
        );
        
        // Hoàn tiền lại cho ví
        Wallet wallet = txn.getWallet();
        wallet.setBalance(wallet.getBalance().add(txn.getAmount()));
        walletRepository.save(wallet);
    }
}
