package com.wobblevault.backend.features.income;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wobblevault.backend.entity.IncomeSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class IncomeSourceService {

    public static final String CATEGORY_PAYMENT = "PAYMENT";
    public static final String CATEGORY_LIQUIDATION = "LIQUIDATION";

    private static final List<String> ALLOWED_SHOP_TYPES = List.of("store", "online");
    private static final List<String> ALLOWED_PAYMENT_METHODS = List.of("cash", "gcash");

    private final IncomeSourceRepository incomeSourceRepository;

    public IncomeSourceService(IncomeSourceRepository incomeSourceRepository) {
        this.incomeSourceRepository = incomeSourceRepository;
    }

    public IncomeSourceDTO createIncomeSource(CreateIncomeSourceRequest request) {
        String shopType = normalizeShopType(request.getShopType());
        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        String paymentCategory = normalizePaymentCategory(request.getPaymentCategory());

        IncomeSource incomeSource = new IncomeSource();
        incomeSource.setShopType(shopType);
        incomeSource.setPaymentMethod(paymentMethod);
        incomeSource.setIncomeDate(request.getIncomeDate());
        incomeSource.setCustomerName(blankToNull(request.getCustomerName()));
        incomeSource.setJobOrderNo(blankToNull(request.getJobOrderNo()));
        incomeSource.setAmount(request.getAmount());
        incomeSource.setPaymentCategory(paymentCategory);
        incomeSource.setRemarks(blankToNull(request.getRemarks()));

        String referenceNumber = resolveReferenceNumber(request, paymentCategory);
        incomeSource.setReferenceNumber(referenceNumber);

        IncomeSource savedIncomeSource = incomeSourceRepository.save(incomeSource);
        return new IncomeSourceDTO(savedIncomeSource);
    }

    private String resolveReferenceNumber(CreateIncomeSourceRequest request, String paymentCategory) {
        if (request.getReferenceNumber() != null && !request.getReferenceNumber().isBlank()) {
            return request.getReferenceNumber().trim();
        }

        if (!CATEGORY_LIQUIDATION.equals(paymentCategory)) {
            return null;
        }

        LocalDate incomeDate = request.getIncomeDate() != null ? request.getIncomeDate() : LocalDate.now();
        String prefix = "LIQ-" + incomeDate.toString().replace("-", "");

        int nextSequence = incomeSourceRepository.findByIncomeDate(incomeDate).stream()
                .map(IncomeSource::getReferenceNumber)
                .filter(reference -> reference != null && reference.startsWith(prefix))
                .map(IncomeSourceService::extractLiquidationSequence)
                .filter(sequence -> sequence != null && sequence > 0)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;

        return String.format("%s-%04d", prefix, nextSequence);
    }

    private static Integer extractLiquidationSequence(String referenceNumber) {
        if (referenceNumber == null || referenceNumber.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("-(\\d{4})$").matcher(referenceNumber.trim());
        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public BigDecimal getTotalIncomeByJobOrderNo(String jobOrderNo) {
        if (jobOrderNo == null || jobOrderNo.isBlank()) {
            return BigDecimal.ZERO;
        }

        return incomeSourceRepository.findByJobOrderNo(jobOrderNo).stream()
                .map(income -> income.getAmount() != null ? income.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void syncOrderPayment(CreateIncomeSourceRequest request) {
        if (request.getJobOrderNo() == null || request.getJobOrderNo().isBlank()) {
            return;
        }

        String paymentCategory = normalizePaymentCategory(request.getPaymentCategory());
        IncomeSource existingPayment = findOrderPaymentEntry(request.getJobOrderNo(), paymentCategory);
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            if (existingPayment != null) {
                incomeSourceRepository.delete(existingPayment);
            }
            return;
        }

        if (existingPayment == null) {
            createIncomeSource(request);
            return;
        }

        existingPayment.setShopType(normalizeShopType(request.getShopType()));
        existingPayment.setPaymentMethod(normalizePaymentMethod(request.getPaymentMethod()));
        existingPayment.setIncomeDate(request.getIncomeDate());
        existingPayment.setCustomerName(blankToNull(request.getCustomerName()));
        existingPayment.setAmount(amount);
        existingPayment.setReferenceNumber(blankToNull(request.getReferenceNumber()));
        existingPayment.setPaymentCategory(paymentCategory);
        existingPayment.setRemarks(blankToNull(request.getRemarks()));

        incomeSourceRepository.save(existingPayment);
    }

    public void deletePaymentsForJobOrderNo(String jobOrderNo, String paymentCategory) {
        if (jobOrderNo == null || jobOrderNo.isBlank()) {
            return;
        }
        for (IncomeSource entry : incomeSourceRepository.findByJobOrderNo(jobOrderNo)) {
            if (paymentCategory == null
                    || paymentCategory.equalsIgnoreCase(entry.getPaymentCategory())) {
                incomeSourceRepository.delete(entry);
            }
        }
    }

    private IncomeSource findOrderPaymentEntry(String jobOrderNo, String paymentCategory) {
        List<IncomeSource> entries = incomeSourceRepository.findByJobOrderNoOrderByCreatedAtAsc(jobOrderNo);
        if (entries.isEmpty()) {
            return null;
        }

        if (paymentCategory != null && !paymentCategory.isBlank()) {
            Optional<IncomeSource> categorizedEntry = entries.stream()
                    .filter(entry -> paymentCategory.equalsIgnoreCase(entry.getPaymentCategory()))
                    .findFirst();
            if (categorizedEntry.isPresent()) {
                return categorizedEntry.get();
            }
        }

        return entries.get(0);
    }

    public IncomeSourceDTO getIncomeSourceById(UUID id) {
        IncomeSource incomeSource = incomeSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Income source not found"));
        return new IncomeSourceDTO(incomeSource);
    }

    public Page<IncomeSourceDTO> getAllIncomeSources(Pageable pageable) {
        return incomeSourceRepository.findAll(pageable)
                .map(IncomeSourceDTO::new);
    }

    public List<IncomeSourceDTO> getIncomeSourceByDate(LocalDate date) {
        return incomeSourceRepository.findByIncomeDate(date).stream()
                .map(IncomeSourceDTO::new)
                .collect(Collectors.toList());
    }

    public List<IncomeSourceDTO> getIncomeSourcesByDateRange(LocalDate startDate, LocalDate endDate) {
        return incomeSourceRepository.findByIncomeDateBetween(startDate, endDate).stream()
                .map(IncomeSourceDTO::new)
                .collect(Collectors.toList());
    }

    public void deleteIncomeSource(UUID id) {
        IncomeSource incomeSource = incomeSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Income source not found"));
        incomeSourceRepository.delete(incomeSource);
    }

    private String normalizeShopType(String shopType) {
        String normalized = shopType != null ? shopType.trim().toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_SHOP_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Shop type must be one of: store, online");
        }
        return normalized;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod != null ? paymentMethod.trim().toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_PAYMENT_METHODS.contains(normalized)) {
            throw new IllegalArgumentException("Payment method must be one of: cash, gcash");
        }
        return normalized;
    }

    private String normalizePaymentCategory(String paymentCategory) {
        if (paymentCategory == null || paymentCategory.isBlank()) {
            return CATEGORY_PAYMENT;
        }
        String normalized = paymentCategory.trim().toUpperCase(Locale.ROOT);
        if (!CATEGORY_PAYMENT.equals(normalized) && !CATEGORY_LIQUIDATION.equals(normalized)) {
            throw new IllegalArgumentException("Payment category must be one of: PAYMENT, LIQUIDATION");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
