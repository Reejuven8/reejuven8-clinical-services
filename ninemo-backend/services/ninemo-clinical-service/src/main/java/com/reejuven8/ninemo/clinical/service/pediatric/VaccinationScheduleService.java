package com.reejuven8.ninemo.clinical.service.pediatric;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import com.reejuven8.ninemo.clinical.model.entity.VaccinationRecord;
import com.reejuven8.ninemo.clinical.model.enums.VaccinationStatus;
import com.reejuven8.ninemo.clinical.repository.ChildProfileRepository;
import com.reejuven8.ninemo.clinical.repository.VaccinationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VaccinationScheduleService {

    private final ChildProfileRepository childProfileRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final ChildAccessGuard childAccessGuard;

    // IAP 2023 schedule: {vaccineName, vaccineCode, doseNumber, scheduledDaysFromBirth}
    private static final Object[][] IAP_SCHEDULE = {
        // Birth
        {"BCG",          "BCG",       1,  0},
        {"Hepatitis B",  "HEP_B",     1,  0},
        {"OPV",          "OPV",       0,  0},   // OPV0 (birth dose)
        // 6 weeks
        {"DPT",          "DPT",       1,  42},
        {"IPV",          "IPV",       1,  42},
        {"Hepatitis B",  "HEP_B",     2,  42},
        {"Hib",          "HIB",       1,  42},
        {"Rotavirus",    "ROTAVIRUS", 1,  42},
        {"PCV",          "PCV",       1,  42},
        // 10 weeks
        {"DPT",          "DPT",       2,  70},
        {"IPV",          "IPV",       2,  70},
        {"Hib",          "HIB",       2,  70},
        {"Rotavirus",    "ROTAVIRUS", 2,  70},
        {"PCV",          "PCV",       2,  70},
        // 14 weeks
        {"DPT",          "DPT",       3,  98},
        {"IPV",          "IPV",       3,  98},
        {"Hib",          "HIB",       3,  98},
        {"Rotavirus",    "ROTAVIRUS", 3,  98},
        {"PCV",          "PCV",       3,  98},
        // 6 months
        {"Hepatitis B",  "HEP_B",     3,  180},
        // 9 months
        {"MMR",          "MMR",       1,  270},
        // 12 months
        {"Varicella",    "VARICELLA", 1,  365},
        {"Hepatitis A",  "HEP_A",     1,  365},
        {"Typhoid (TCV)","TCV",       1,  365},
        // 15 months
        {"MMR",          "MMR",       2,  456},
        {"Varicella",    "VARICELLA", 2,  456},
        {"PCV",          "PCV",       4,  456},  // PCV booster
        // 16–18 months
        {"DPT",          "DPT",       4,  511},  // DPT booster 1
        {"IPV",          "IPV",       4,  511},
        {"Hib",          "HIB",       4,  511},
        // 18 months
        {"Hepatitis A",  "HEP_A",     2,  547},
        // 4–6 years (scheduled at 5 years)
        {"DPT",          "DPT",       5,  1825},
        {"OPV",          "OPV",       1,  1825},
        {"MMR",          "MMR",       3,  1825},
        {"Varicella",    "VARICELLA", 3,  1825},
        // 10–12 years (scheduled at 10 years)
        {"Tdap",         "TDAP",      1,  3650},
        {"HPV",          "HPV",       1,  3650},
        {"HPV",          "HPV",       2,  3832},  // HPV dose 2: 6 months after dose 1
    };

    /**
     * Caller-facing entry point — ownership checked (IS-027). The unchecked overload below
     * stays internal for the mode-transition flow, which has already verified the parent.
     */
    @Transactional
    public List<VaccinationRecord> getSchedule(UUID childId, UUID userId) {
        childAccessGuard.requireOwnedChild(childId, userId);
        return generateSchedule(childId);
    }

    @Transactional
    public List<VaccinationRecord> generateSchedule(UUID childId) {
        ChildProfile child = childProfileRepository.findById(childId)
            .orElseThrow(() -> new ResourceNotFoundException("ChildProfile", childId.toString()));

        // Idempotent: return existing schedule if already generated
        List<VaccinationRecord> existing = vaccinationRecordRepository.findByChildIdOrderByScheduledDateAsc(childId);
        if (!existing.isEmpty()) return existing;

        LocalDate dob = child.getDateOfBirth();
        List<VaccinationRecord> records = new ArrayList<>();

        for (Object[] row : IAP_SCHEDULE) {
            String name       = (String) row[0];
            String code       = (String) row[1];
            int    dose       = (int)    row[2];
            int    offsetDays = (int)    row[3];

            LocalDate scheduled = dob.plusDays(offsetDays);
            VaccinationStatus status = scheduled.isAfter(LocalDate.now())
                ? VaccinationStatus.PENDING
                : VaccinationStatus.OVERDUE;

            records.add(VaccinationRecord.builder()
                .childId(childId)
                .vaccineName(name)
                .vaccineCode(code)
                .doseNumber(dose)
                .scheduledDate(scheduled)
                .status(status)
                .build());
        }

        return vaccinationRecordRepository.saveAll(records);
    }

    @Transactional
    public VaccinationRecord markCompleted(UUID vaccinationId, UUID userId,
                                           LocalDate administeredDate, String administeredBy) {
        VaccinationRecord record = vaccinationRecordRepository.findById(vaccinationId)
            .orElseThrow(() -> new ResourceNotFoundException("VaccinationRecord", vaccinationId.toString()));

        // IS-027: the record carries the childId — resolve it back to the owning parent.
        childAccessGuard.requireOwnedChild(record.getChildId(), userId);

        record.setStatus(VaccinationStatus.COMPLETED);
        record.setAdministeredDate(administeredDate != null ? administeredDate : LocalDate.now());
        if (administeredBy != null) record.setAdministeredBy(administeredBy);

        return vaccinationRecordRepository.save(record);
    }
}
