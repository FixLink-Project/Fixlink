package com.fixlink.application.service;

import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataQuotationRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataWorkProgressRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceCategoryRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestReviewRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceAreaRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.entity.WorkProgressJpaEntity;
import com.fixlink.adapter.out.persistence.entity.ServiceAreaJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.adapter.out.persistence.entity.AppointmentJpaEntity;
import com.fixlink.application.port.in.AdminStatisticsUseCase;
import com.fixlink.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService implements AdminStatisticsUseCase {
    private final SpringDataUserRepository userRepository;
    private final SpringDataRepairRequestRepository repairRequestRepository;
    private final SpringDataQuotationRepository quotationRepository;
    private final SpringDataAppointmentRepository appointmentRepository;
    private final SpringDataWorkProgressRepository workProgressRepository;
    private final SpringDataServiceCategoryRepository categoryRepository;
    private final SpringDataRepairRequestReviewRepository reviewRepository;
    private final SpringDataServiceAreaRepository areaRepository;
    private final SpringDataTechnicianProfileRepository technicianProfileRepository;

    @Override
    public List<Map<String, Object>> getVerifiedOfflineTechnicians(Long areaId, Long categoryId) {
        List<Map<String, Object>> technicians = technicianProfileRepository
                .findVerifiedOfflineByAreaAndCategory(areaId, categoryId).stream()
                .map(profile -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("technicianId", profile.getUserId());
                    item.put("name", profile.getFullName());
                    item.put("phone", profile.getPhone());
                    item.put("specialization", profile.getSpecialization());
                    item.put("rating", profile.getAvgRating());
                    item.put("completedJobs", profile.getCompletedJobs());
                    item.put("areaIds", profile.getAreaIds());
                    item.put("categoryIds", profile.getCategoryIds());
                    item.put("online", Boolean.TRUE.equals(profile.getIsOnline()));
                    return item;
                }).toList();
        return technicians;
    }

    @Override
    public Map<String, Object> getMonthlyStatistics(LocalDate month, Long areaId) {
        LocalDate selectedMonth = (month == null ? LocalDate.now() : month).withDayOfMonth(1);
        LocalDateTime from = selectedMonth.atStartOfDay();
        LocalDateTime to = selectedMonth.plusMonths(1).atStartOfDay();

        long customerRegistrations = userRepository.countByRoleAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                Role.CUSTOMER, from, to);
        long technicianRegistrations = userRepository.countByRoleAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                Role.TECHNICIAN, from, to);
        long jobsCreated = repairRequestRepository.countCreatedBetween(from, to);
        long activeCustomers = repairRequestRepository.countActiveCustomersBetween(from, to);
        long activeTechnicians = repairRequestRepository.countActiveTechniciansBetween(from, to);
        long jobsAccepted = workProgressRepository.countByToStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                com.fixlink.domain.model.RequestStatus.ASSIGNED, from, to);
        long cancelledJobs = workProgressRepository.countByToStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                com.fixlink.domain.model.RequestStatus.CANCELLED, from, to);
        long quotesCreated = quotationRepository.countCreatedBetween(from, to);
        long techniciansQuoting = quotationRepository.countActiveTechniciansBetween(from, to);
        long customersReceivingQuotes = quotationRepository.countCustomersReceivingQuotesBetween(from, to);
        long appointmentsCreated = appointmentRepository.countCreatedBetween(from, to);
        long customersBooking = appointmentRepository.countActiveCustomersBetween(from, to);
        long techniciansBooked = appointmentRepository.countActiveTechniciansBetween(from, to);
        long progressUpdates = workProgressRepository.countCreatedBetween(from, to);
        long techniciansUpdatingProgress = workProgressRepository.countActiveTechniciansBetween(from, to);
        Double averageRating = reviewRepository.averageRatingBetween(from, to, areaId);
        double monthlyAverageRating = averageRating == null ? 0 : averageRating;
        long monthlyReviewCount = reviewRepository.countBetween(from, to, areaId);
        List<Map<String, Object>> ratingBreakdown = new ArrayList<>();
        for (int rating = 5; rating >= 1; rating--) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("rating", rating);
            point.put("count", reviewRepository.countRatingBetween(from, to, areaId, rating));
            ratingBreakdown.add(point);
        }

        List<RepairRequestJpaEntity> monthlyRequests = repairRequestRepository
                .findByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to);
        List<RepairRequestJpaEntity> allMonthlyRequests = monthlyRequests;
        List<Long> allMonthlyRequestIds = allMonthlyRequests.stream().map(RepairRequestJpaEntity::getId).toList();
        Set<Long> allAcceptedRequestIds = allMonthlyRequestIds.isEmpty() ? Set.of() : workProgressRepository
                .findByRequestIdInAndToStatus(allMonthlyRequestIds, com.fixlink.domain.model.RequestStatus.ASSIGNED).stream()
                .map(WorkProgressJpaEntity::getRequestId).collect(Collectors.toSet());
        if (areaId != null) monthlyRequests = monthlyRequests.stream().filter(r -> areaId.equals(r.getAreaId())).toList();
        List<Long> requestIds = monthlyRequests.stream().map(RepairRequestJpaEntity::getId).toList();
        Set<Long> acceptedRequestIds = requestIds.isEmpty() ? Set.of() : workProgressRepository
                .findByRequestIdInAndToStatus(requestIds, com.fixlink.domain.model.RequestStatus.ASSIGNED).stream()
                .map(WorkProgressJpaEntity::getRequestId).collect(Collectors.toSet());
        List<QuotationJpaEntity> monthlyQuotes = requestIds.isEmpty() ? List.of()
                : quotationRepository.findByRequestIdInAndDeletedAtIsNull(requestIds);
        List<AppointmentJpaEntity> monthlyAppointments = requestIds.isEmpty() ? List.of()
                : appointmentRepository.findByRepairRequestIdInAndDeletedAtIsNull(requestIds);
        List<WorkProgressJpaEntity> scopedProgress = areaId == null || requestIds.isEmpty() ? List.of()
                : workProgressRepository.findByRequestIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(requestIds, from, to);
        if (areaId != null) {
            jobsCreated = monthlyRequests.size();
            activeCustomers = monthlyRequests.stream().map(RepairRequestJpaEntity::getCustomerId).distinct().count();
            activeTechnicians = monthlyRequests.stream().filter(r -> r.getTechnicianId() != null).map(RepairRequestJpaEntity::getTechnicianId).distinct().count();
            jobsAccepted = scopedProgress.stream().filter(w -> w.getToStatus() == com.fixlink.domain.model.RequestStatus.ASSIGNED).count();
            cancelledJobs = scopedProgress.stream().filter(w -> w.getToStatus() == com.fixlink.domain.model.RequestStatus.CANCELLED).count();
            List<QuotationJpaEntity> quotesThisMonth = monthlyQuotes.stream().filter(q -> !q.getCreatedAt().isBefore(from) && q.getCreatedAt().isBefore(to)).toList();
            List<AppointmentJpaEntity> appointmentsThisMonth = monthlyAppointments.stream().filter(a -> !a.getCreatedAt().isBefore(from) && a.getCreatedAt().isBefore(to)).toList();
            quotesCreated = quotesThisMonth.size();
            techniciansQuoting = quotesThisMonth.stream().map(QuotationJpaEntity::getTechnicianId).distinct().count();
            Map<Long, Long> customerByRequest = monthlyRequests.stream().collect(Collectors.toMap(
                    RepairRequestJpaEntity::getId, RepairRequestJpaEntity::getCustomerId));
            customersReceivingQuotes = quotesThisMonth.stream().map(QuotationJpaEntity::getRequestId)
                    .map(customerByRequest::get).filter(java.util.Objects::nonNull).distinct().count();
            appointmentsCreated = appointmentsThisMonth.size();
            customersBooking = appointmentsThisMonth.stream().map(AppointmentJpaEntity::getCustomerId).distinct().count();
            techniciansBooked = appointmentsThisMonth.stream().map(AppointmentJpaEntity::getTechnicianId).distinct().count();
            progressUpdates = scopedProgress.size();
            techniciansUpdatingProgress = scopedProgress.stream().map(WorkProgressJpaEntity::getCreatedBy).filter(java.util.Objects::nonNull).distinct().count();
        }

        Set<Long> quotedRequestIds = monthlyQuotes.stream().map(QuotationJpaEntity::getRequestId).collect(Collectors.toSet());
        Set<Long> appointmentRequestIds = monthlyAppointments.stream().map(AppointmentJpaEntity::getRepairRequestId).collect(Collectors.toSet());
        Set<Long> appointmentAfterQuoteIds = appointmentRequestIds.stream().filter(quotedRequestIds::contains).collect(Collectors.toSet());
        Set<Long> completedRequestIds = monthlyRequests.stream()
                .filter(r -> r.getStatus() == com.fixlink.domain.model.RequestStatus.COMPLETED)
                .map(RepairRequestJpaEntity::getId).collect(Collectors.toSet());
        long completedAfterAppointment = completedRequestIds.stream().filter(appointmentRequestIds::contains).count();
        long completedJobs = monthlyRequests.stream().filter(r -> r.getStatus() == com.fixlink.domain.model.RequestStatus.COMPLETED).count();
        long acceptedJobsInCohort = acceptedRequestIds.size();
        BigDecimal completedValue = monthlyRequests.stream()
                .filter(r -> r.getStatus() == com.fixlink.domain.model.RequestStatus.COMPLETED)
                .map(r -> r.getAgreedPrice() == null ? BigDecimal.ZERO : r.getAgreedPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageCompletedOrder = completedJobs == 0 ? BigDecimal.ZERO
                : completedValue.divide(BigDecimal.valueOf(completedJobs), 0, java.math.RoundingMode.HALF_UP);
        BigDecimal requestedGmv = monthlyRequests.stream()
                .map(r -> r.getAgreedPrice() != null && r.getAgreedPrice().signum() > 0 ? r.getAgreedPrice()
                        : r.getBudgetRef() == null ? BigDecimal.ZERO : r.getBudgetRef())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal acceptedGmv = monthlyRequests.stream().filter(r -> acceptedRequestIds.contains(r.getId()))
                .map(r -> r.getAgreedPrice() != null && r.getAgreedPrice().signum() > 0 ? r.getAgreedPrice()
                        : r.getBudgetRef() == null ? BigDecimal.ZERO : r.getBudgetRef())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, LocalDateTime> requestCreatedAt = monthlyRequests.stream().collect(Collectors.toMap(
                RepairRequestJpaEntity::getId, RepairRequestJpaEntity::getCreatedAt));
        Map<Long, Long> firstResponseMinutes = new HashMap<>();
        monthlyQuotes.forEach(q -> recordResponse(requestCreatedAt, firstResponseMinutes, q.getRequestId(), q.getCreatedAt()));
        if (!requestIds.isEmpty()) workProgressRepository
                .findByRequestIdInAndToStatus(requestIds, com.fixlink.domain.model.RequestStatus.ASSIGNED)
                .forEach(w -> recordResponse(requestCreatedAt, firstResponseMinutes, w.getRequestId(), w.getCreatedAt()));
        long averageResponseMinutes = firstResponseMinutes.isEmpty() ? -1
                : Math.round(firstResponseMinutes.values().stream().mapToLong(Long::longValue).average().orElse(0));

        Map<Long, Long> categoryCounts = monthlyRequests.stream().collect(Collectors.groupingBy(
                RepairRequestJpaEntity::getCategoryId, HashMap::new, Collectors.counting()));
        Map<Long, Long> availableByCategory = technicianProfileRepository.countAvailableByCategoryGrouped(areaId).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> ((Number) row[1]).longValue()));
        long jobsCreatedForShare = jobsCreated;
        List<Map<String, Object>> categoryBreakdown = categoryRepository.findAllByDeletedAtIsNull().stream()
                .filter(c -> categoryCounts.containsKey(c.getId()))
                .map(c -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    long count = categoryCounts.get(c.getId());
                    item.put("categoryId", c.getId());
                    item.put("name", c.getName());
                    item.put("requests", count);
                    item.put("percentage", jobsCreatedForShare == 0 ? 0 : Math.round(count * 100.0 / jobsCreatedForShare));
                    item.put("availableTechnicians", availableByCategory.getOrDefault(c.getId(), 0L));
                    return item;
                }).sorted((a, b) -> Long.compare((Long) b.get("requests"), (Long) a.get("requests"))).toList();

        Map<Long, Long> areaRequestCounts = allMonthlyRequests.stream().filter(r -> r.getAreaId() != null)
                .collect(Collectors.groupingBy(RepairRequestJpaEntity::getAreaId, HashMap::new, Collectors.counting()));
        Map<Long, List<RepairRequestJpaEntity>> requestsByArea = allMonthlyRequests.stream()
                .filter(r -> r.getAreaId() != null)
                .collect(Collectors.groupingBy(RepairRequestJpaEntity::getAreaId));
        Map<Long, Long> availableByArea = technicianProfileRepository.countAvailableByAreaGrouped().stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> ((Number) row[1]).longValue()));
        List<Map<String, Object>> areaBreakdown = areaRepository.findAllByIsActiveTrue().stream()
                .map(area -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    List<RepairRequestJpaEntity> areaRequests = requestsByArea.getOrDefault(area.getId(), List.of());
                    long count = areaRequestCounts.getOrDefault(area.getId(), 0L);
                    long assigned = areaRequests.stream().filter(r -> allAcceptedRequestIds.contains(r.getId())).count();
                    long completed = areaRequests.stream().filter(r -> r.getStatus() == com.fixlink.domain.model.RequestStatus.COMPLETED).count();
                    long cancelled = areaRequests.stream().filter(r -> r.getStatus() == com.fixlink.domain.model.RequestStatus.CANCELLED).count();
                    BigDecimal serviceValue = areaRequests.stream()
                            .filter(r -> r.getStatus() == com.fixlink.domain.model.RequestStatus.COMPLETED)
                            .map(r -> r.getAgreedPrice() == null ? BigDecimal.ZERO : r.getAgreedPrice())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    long available = availableByArea.getOrDefault(area.getId(), 0L);
                    item.put("areaId", area.getId());
                    item.put("name", area.getName());
                    item.put("city", area.getCity());
                    item.put("requests", count);
                    item.put("assignedJobs", assigned);
                    item.put("completedJobs", completed);
                    item.put("cancelledJobs", cancelled);
                    item.put("acceptanceRate", count == 0 ? 0 : Math.round(assigned * 100.0 / count));
                    item.put("completionRate", assigned == 0 ? 0 : Math.round(completed * 100.0 / assigned));
                    item.put("completedJobValueVnd", serviceValue);
                    item.put("availableTechnicians", available);
                    item.put("gap", Math.max(0, count - available));
                    return item;
                }).sorted((a, b) -> Long.compare((Long) b.get("gap"), (Long) a.get("gap"))).toList();

        Map<LocalDate, Long> dailyJobs = monthlyRequests.stream().collect(Collectors.groupingBy(
                r -> r.getCreatedAt().toLocalDate(), Collectors.counting()));
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (LocalDate day = selectedMonth; day.isBefore(selectedMonth.plusMonths(1)); day = day.plusDays(1)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.toString());
            point.put("jobs", dailyJobs.getOrDefault(day, 0L));
            dailyTrend.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", selectedMonth.toString());
        result.put("areaId", areaId);
        result.put("areaName", areaId == null ? "Tất cả khu vực" : areaRepository.findByIdAndDeletedAtIsNull(areaId).map(ServiceAreaJpaEntity::getName).orElse("Khu vực đã chọn"));
        result.put("areas", areaRepository.findAllByIsActiveTrue().stream().map(a -> Map.of("id", a.getId(), "name", a.getName(), "city", a.getCity())).toList());
        result.put("customerRegistrations", customerRegistrations);
        result.put("activeCustomers", activeCustomers);
        result.put("technicianRegistrations", technicianRegistrations);
        result.put("activeTechnicians", activeTechnicians);
        result.put("jobsCreated", jobsCreated);
        result.put("jobsAccepted", jobsAccepted);
        result.put("cancelledJobs", cancelledJobs);
        result.put("quotesCreated", quotesCreated);
        result.put("techniciansQuoting", techniciansQuoting);
        result.put("customersReceivingQuotes", customersReceivingQuotes);
        result.put("appointmentsCreated", appointmentsCreated);
        result.put("customersBooking", customersBooking);
        result.put("techniciansBooked", techniciansBooked);
        result.put("progressUpdates", progressUpdates);
        result.put("techniciansUpdatingProgress", techniciansUpdatingProgress);
        result.put("completedJobs", completedJobs);
        result.put("completedJobValueVnd", completedValue);
        result.put("averageCompletedOrderVnd", averageCompletedOrder);
        result.put("requestedGmvVnd", requestedGmv);
        result.put("acceptedGmvVnd", acceptedGmv);
        result.put("acceptedJobsInCohort", acceptedJobsInCohort);
        result.put("completionRate", acceptedJobsInCohort == 0 ? 0 : Math.round(completedJobs * 100.0 / acceptedJobsInCohort));
        result.put("averageResponseMinutes", averageResponseMinutes);
        result.put("respondedJobs", firstResponseMinutes.size());
        result.put("jobsWithQuotes", quotedRequestIds.size());
        result.put("jobsWithAppointments", appointmentRequestIds.size());
        result.put("quoteConversionRate", jobsCreated == 0 ? 0 : Math.round(quotedRequestIds.size() * 100.0 / jobsCreated));
        result.put("appointmentConversionRate", quotedRequestIds.isEmpty() ? 0 : Math.round(appointmentAfterQuoteIds.size() * 100.0 / quotedRequestIds.size()));
        result.put("completionConversionRate", appointmentRequestIds.isEmpty() ? 0 : Math.round(completedAfterAppointment * 100.0 / appointmentRequestIds.size()));
        result.put("categoryBreakdown", categoryBreakdown);
        result.put("areaBreakdown", areaBreakdown);
        result.put("dailyTrend", dailyTrend);
        result.put("financialMetricNote", "Giá trị hoàn thành dùng giá đã thỏa thuận; hệ thống chưa lưu giao dịch thanh toán hoặc phí hoa hồng.");
        result.put("qualityMetricNote", "Đã ghi nhận đánh giá theo job; hệ thống chưa có hồ sơ khiếu nại hoặc hoàn tiền gắn với yêu cầu.");
        result.put("averageRating", monthlyAverageRating);
        result.put("reviewCount", monthlyReviewCount);
        result.put("ratingBreakdown", ratingBreakdown);
        return result;
    }

    private static void recordResponse(Map<Long, LocalDateTime> createdAtByRequest,
                                       Map<Long, Long> responseMinutesByRequest,
                                       Long requestId, LocalDateTime responseAt) {
        LocalDateTime createdAt = createdAtByRequest.get(requestId);
        if (createdAt == null || responseAt == null || responseAt.isBefore(createdAt)) return;
        long minutes = Duration.between(createdAt, responseAt).toMinutes();
        responseMinutesByRequest.merge(requestId, minutes, Math::min);
    }
}

