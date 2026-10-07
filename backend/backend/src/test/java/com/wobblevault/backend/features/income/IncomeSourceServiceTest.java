package com.wobblevault.backend.features.income;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.wobblevault.backend.entity.IncomeSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncomeSourceServiceTest {

    @Mock
    private IncomeSourceRepository incomeSourceRepository;

    private IncomeSourceService incomeSourceService;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        incomeSourceService = new IncomeSourceService(incomeSourceRepository);
        today = LocalDate.of(2026, 10, 7);
        when(incomeSourceRepository.save(any(IncomeSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(incomeSourceRepository.findByIncomeDate(any())).thenReturn(new ArrayList<>());
    }

    private CreateIncomeSourceRequest liquidationRequest() {
        CreateIncomeSourceRequest request = new CreateIncomeSourceRequest();
        request.setShopType("store");
        request.setPaymentMethod("cash");
        request.setIncomeDate(today);
        request.setCustomerName("Walk-in");
        request.setJobOrderNo(null);
        request.setAmount(new BigDecimal("1500.00"));
        request.setPaymentCategory("LIQUIDATION");
        request.setRemarks("supplies");
        return request;
    }

    @Test
    void createIncomeSource_generatesLiquidationReference() {
        IncomeSourceDTO dto = incomeSourceService.createIncomeSource(liquidationRequest());
        assertEquals("LIQ-20261007-0001", dto.getReferenceNumber());
    }

    @Test
    void createIncomeSource_incrementsLiquidationSequence() {
        IncomeSource existing = new IncomeSource();
        existing.setReferenceNumber("LIQ-20261007-0007");
        when(incomeSourceRepository.findByIncomeDate(today)).thenReturn(List.of(existing));

        IncomeSourceDTO dto = incomeSourceService.createIncomeSource(liquidationRequest());
        assertEquals("LIQ-20261007-0008", dto.getReferenceNumber());
    }

    @Test
    void createIncomeSource_invalidShopType_throws() {
        CreateIncomeSourceRequest request = liquidationRequest();
        request.setShopType("bogus");
        assertThrows(IllegalArgumentException.class, () -> incomeSourceService.createIncomeSource(request));
    }

    @Test
    void createIncomeSource_invalidPaymentMethod_throws() {
        CreateIncomeSourceRequest request = liquidationRequest();
        request.setPaymentMethod("cheque");
        assertThrows(IllegalArgumentException.class, () -> incomeSourceService.createIncomeSource(request));
    }

    @Test
    void syncOrderPayment_createsEntryWhenPaymentPositive() {
        CreateIncomeSourceRequest request = new CreateIncomeSourceRequest(
                "store", "cash", today, "Juan Cruz", "071026-01", new BigDecimal("5000.00"),
                null, "PAYMENT", null);

        incomeSourceService.syncOrderPayment(request);

        ArgumentCaptor<IncomeSource> captor = ArgumentCaptor.forClass(IncomeSource.class);
        verify(incomeSourceRepository).save(captor.capture());
        assertEquals(new BigDecimal("5000.00"), captor.getValue().getAmount());
        assertEquals("071026-01", captor.getValue().getJobOrderNo());
        assertEquals("PAYMENT", captor.getValue().getPaymentCategory());
    }

    @Test
    void syncOrderPayment_zeroAmount_doesNothingWhenNoEntryExists() {
        CreateIncomeSourceRequest request = new CreateIncomeSourceRequest(
                "store", "cash", today, "Juan Cruz", "071026-01", BigDecimal.ZERO,
                null, "PAYMENT", null);

        incomeSourceService.syncOrderPayment(request);

        verify(incomeSourceRepository, never()).save(any(IncomeSource.class));
        verify(incomeSourceRepository, never()).delete(any(IncomeSource.class));
    }

    @Test
    void syncOrderPayment_zeroAmount_deletesExistingEntry() {
        IncomeSource existing = new IncomeSource();
        existing.setId(UUID.randomUUID());
        existing.setJobOrderNo("071026-01");
        existing.setPaymentCategory("PAYMENT");
        when(incomeSourceRepository.findByJobOrderNoOrderByCreatedAtAsc("071026-01"))
                .thenReturn(List.of(existing));

        CreateIncomeSourceRequest request = new CreateIncomeSourceRequest(
                "store", "cash", today, "Juan Cruz", "071026-01", BigDecimal.ZERO,
                null, "PAYMENT", null);

        incomeSourceService.syncOrderPayment(request);

        verify(incomeSourceRepository).delete(existing);
        verify(incomeSourceRepository, never()).save(any(IncomeSource.class));
    }

    @Test
    void syncOrderPayment_updatesExistingEntry() {
        IncomeSource existing = new IncomeSource();
        existing.setId(UUID.randomUUID());
        existing.setJobOrderNo("071026-01");
        existing.setPaymentCategory("PAYMENT");
        existing.setAmount(new BigDecimal("5000.00"));
        when(incomeSourceRepository.findByJobOrderNoOrderByCreatedAtAsc("071026-01"))
                .thenReturn(List.of(existing));

        CreateIncomeSourceRequest request = new CreateIncomeSourceRequest(
                "online", "gcash", today, "Juan Cruz", "071026-01", new BigDecimal("7500.00"),
                null, "PAYMENT", "updated");

        incomeSourceService.syncOrderPayment(request);

        ArgumentCaptor<IncomeSource> captor = ArgumentCaptor.forClass(IncomeSource.class);
        verify(incomeSourceRepository).save(captor.capture());
        assertEquals(new BigDecimal("7500.00"), captor.getValue().getAmount());
        assertEquals("online", captor.getValue().getShopType());
        assertEquals("gcash", captor.getValue().getPaymentMethod());
        verify(incomeSourceRepository, never()).delete(any(IncomeSource.class));
    }

    @Test
    void getTotalIncomeByJobOrderNo_sumsEntries() {
        IncomeSource first = new IncomeSource();
        first.setAmount(new BigDecimal("100.00"));
        IncomeSource second = new IncomeSource();
        second.setAmount(new BigDecimal("50.50"));
        when(incomeSourceRepository.findByJobOrderNo("071026-01")).thenReturn(List.of(first, second));

        BigDecimal total = incomeSourceService.getTotalIncomeByJobOrderNo("071026-01");
        assertEquals(new BigDecimal("150.50"), total);
    }
}
