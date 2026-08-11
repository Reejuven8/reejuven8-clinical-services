package com.reejuven8.ninemo.clinical.service.pediatric;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.document.DevelopmentalMilestone;
import com.reejuven8.ninemo.clinical.publisher.MilestoneReminderPublisher;
import com.reejuven8.ninemo.clinical.repository.DevelopmentalMilestoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevelopmentalMilestoneService {

    private final DevelopmentalMilestoneRepository milestoneRepository;
    private final ChildAccessGuard childAccessGuard;
    private final MilestoneReminderPublisher milestoneReminderPublisher;

    // WHO developmental milestone checklist: month → list of milestone names
    private static final Map<Integer, List<String>> WHO_MILESTONES = new LinkedHashMap<>();

    static {
        WHO_MILESTONES.put(2, List.of(
            "Lifts head when on tummy",
            "Moves both arms and legs equally",
            "Smiles at people",
            "Makes cooing sounds"
        ));
        WHO_MILESTONES.put(4, List.of(
            "Holds head steady",
            "Pushes down on legs when feet on flat surface",
            "Reaches for toys",
            "Brings hands to mouth",
            "Smiles spontaneously at people",
            "Likes to play with people"
        ));
        WHO_MILESTONES.put(6, List.of(
            "Rolls over both ways",
            "Begins to sit without support",
            "Passes toy from hand to hand",
            "Responds to sounds by making sounds",
            "Recognises familiar faces"
        ));
        WHO_MILESTONES.put(9, List.of(
            "Stands holding on to furniture",
            "Pulls to stand",
            "Picks up small objects with finger and thumb",
            "Says mama/dada non-specifically",
            "Copies sounds and gestures"
        ));
        WHO_MILESTONES.put(12, List.of(
            "Walks holding onto furniture",
            "May stand alone briefly",
            "Says 1–2 words besides mama/dada",
            "Cries when parent leaves",
            "Plays simple games like peek-a-boo"
        ));
        WHO_MILESTONES.put(18, List.of(
            "Walks independently",
            "Climbs stairs with support",
            "Says several single words",
            "Points to show things to others",
            "Engages in parallel play"
        ));
        WHO_MILESTONES.put(24, List.of(
            "Runs",
            "Kicks a ball",
            "Uses 2-word phrases",
            "Vocabulary of 50+ words",
            "Copies others' behaviours"
        ));
        WHO_MILESTONES.put(36, List.of(
            "Climbs well",
            "Runs easily",
            "Uses 3-word sentences",
            "Follows 2-step instructions",
            "Engages in make-believe play"
        ));
        WHO_MILESTONES.put(48, List.of(
            "Hops on one foot",
            "Draws a person with 2–4 body parts",
            "Speaks clearly",
            "Counts to 10",
            "Plays cooperatively with other children"
        ));
        WHO_MILESTONES.put(60, List.of(
            "Stands on one foot for 10 seconds",
            "Uses future tense in speech",
            "Tells a simple story",
            "Recognises own name in writing",
            "Can use toilet independently"
        ));
    }

    public DevelopmentalMilestone getOrCreateCheckIn(UUID childId, UUID userId, int month) {
        childAccessGuard.requireOwnedChild(childId, userId); // IS-027

        return milestoneRepository
            .findByChildIdAndMonth(childId.toString(), month)
            .orElseGet(() -> createCheckIn(childId.toString(), month));
    }

    public DevelopmentalMilestone markMilestone(String documentId, UUID userId,
                                                String milestoneName, boolean achieved) {
        DevelopmentalMilestone doc = milestoneRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("DevelopmentalMilestone", documentId));

        // IS-027: the document carries the childId — resolve it back to the owning parent.
        childAccessGuard.requireOwnedChild(UUID.fromString(doc.getChildId()), userId);

        List<Map<String, Object>> milestones = new ArrayList<>(doc.getMilestones());
        for (Map<String, Object> m : milestones) {
            if (milestoneName.equals(m.get("name"))) {
                m.put("achieved", achieved);
                break;
            }
        }
        doc.setMilestones(milestones);
        doc.setUpdatedAt(Instant.now());

        long achievedCount = milestones.stream()
            .filter(m -> Boolean.TRUE.equals(m.get("achieved")))
            .count();
        long total = milestones.size();

        if (total > 0 && (double) achievedCount / total < 0.5) {
            doc.setAlertFlags(List.of("DEVELOPMENTAL_DELAY_RISK"));
            milestoneReminderPublisher.publishMilestoneReminder(
                doc.getChildId(), "DEVELOPMENTAL_DELAY_RISK_" + doc.getMonth() + "M", 0);
            log.warn("Developmental delay risk flagged for child={} month={}", doc.getChildId(), doc.getMonth());
        } else {
            doc.setAlertFlags(Collections.emptyList());
        }

        return milestoneRepository.save(doc);
    }

    public List<DevelopmentalMilestone> getHistory(UUID childId, UUID userId) {
        childAccessGuard.requireOwnedChild(childId, userId); // IS-027
        return milestoneRepository.findByChildIdOrderByMonthAsc(childId.toString());
    }

    private DevelopmentalMilestone createCheckIn(String childId, int month) {
        int nearestMonth = WHO_MILESTONES.keySet().stream()
            .min(Comparator.comparingInt(k -> Math.abs(k - month)))
            .orElse(12);

        List<Map<String, Object>> milestones = WHO_MILESTONES
            .getOrDefault(nearestMonth, List.of())
            .stream()
            .map(name -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", name);
                entry.put("achieved", false);
                return entry;
            })
            .toList();

        DevelopmentalMilestone doc = DevelopmentalMilestone.builder()
            .childId(childId)
            .month(month)
            .category("WHO_MILESTONE")
            .milestones(milestones)
            .alertFlags(Collections.emptyList())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        return milestoneRepository.save(doc);
    }
}
