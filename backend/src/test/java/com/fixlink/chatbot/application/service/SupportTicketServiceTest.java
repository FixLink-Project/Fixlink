package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fixlink.chatbot.application.port.out.SupportTicketRepositoryPort;
import com.fixlink.chatbot.domain.model.SupportTicket;
import com.fixlink.chatbot.domain.model.TicketPriority;
import com.fixlink.chatbot.domain.model.TicketStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepositoryPort ticketRepository;

    private SupportTicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new SupportTicketService(ticketRepository);
    }

    @Test
    @DisplayName("Tạo mã phiếu hỗ trợ đồng thời an toàn không trùng lặp")
    void shouldGenerateUniqueTicketCodesConcurrently() throws InterruptedException {
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> generatedCodes = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    SupportTicket ticket = ticketService.createTicket(
                            1L,
                            "Khách hàng " + idx,
                            "0912345678",
                            "Sự cố " + idx,
                            "Mô tả sự cố " + idx,
                            TicketPriority.MEDIUM,
                            null
                    );
                    generatedCodes.add(ticket.getTicketCode());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(generatedCodes).hasSize(threadCount);
        for (String code : generatedCodes) {
            assertThat(code).startsWith("TCK-");
        }
    }

    @Test
    @DisplayName("Ngăn chặn IDOR: Người dùng A không thể đọc phiếu của người dùng B")
    void shouldPreventIdorWhenReadingTicket() {
        SupportTicket ticket = SupportTicket.builder()
                .id(1L)
                .ticketCode("TCK-1005")
                .userId(100L)
                .title("Lỗi đặt cọc")
                .description("Mô tả")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(ticketRepository.findByCode("TCK-1005")).thenReturn(Optional.of(ticket));

        // User 200 (not owner, not admin, no guestContact match) tries to read
        Optional<SupportTicket> result = ticketService.getTicketByCode("TCK-1005", 200L, false, null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Admin có quyền xem phiếu của bất kỳ ai")
    void shouldAllowAdminToReadAnyTicket() {
        SupportTicket ticket = SupportTicket.builder()
                .id(1L)
                .ticketCode("TCK-1005")
                .userId(100L)
                .title("Lỗi đặt cọc")
                .description("Mô tả")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(ticketRepository.findByCode("TCK-1005")).thenReturn(Optional.of(ticket));

        Optional<SupportTicket> result = ticketService.getTicketByCode("TCK-1005", 999L, true, null);
        assertThat(result).isPresent();
        assertThat(result.get().getTicketCode()).isEqualTo("TCK-1005");
    }
}
