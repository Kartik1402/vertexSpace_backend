package com.example.vertexSpace.util;

import com.example.vertexSpace.entity.ResourceTimeBlock;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConflictChecker {
    public boolean hasOverlap(Instant start1, Instant conflictEnd1,
                              Instant start2, Instant conflictEnd2) {
        // Two ranges overlap if:
        // - First starts before second ends, AND
        // - Second starts before first ends
        return start1.isBefore(conflictEnd2) && start2.isBefore(conflictEnd1);
    }
    public boolean hasConflict(Instant proposedStart, Instant proposedConflictEnd,
                               List<ResourceTimeBlock> existingBlocks) {
        if (existingBlocks == null || existingBlocks.isEmpty()) {
            return false;
        }

        return existingBlocks.stream()
                .anyMatch(block -> hasOverlap(
                        proposedStart, proposedConflictEnd,
                        block.getStartTimeUtc(), block.getConflictEndUtc()
                ));
    }

    /**
     * Find all conflicting blocks
     */
    public List<ResourceTimeBlock> findConflicts(Instant proposedStart, Instant proposedConflictEnd,
                                                 List<ResourceTimeBlock> existingBlocks) {
        if (existingBlocks == null || existingBlocks.isEmpty()) {
            return List.of();
        }

        return existingBlocks.stream()
                .filter(block -> hasOverlap(
                        proposedStart, proposedConflictEnd,
                        block.getStartTimeUtc(), block.getConflictEndUtc()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get earliest available slot after conflicts
     */
    public Instant findNextAvailableSlot(Instant desiredStart,
                                         List<ResourceTimeBlock> conflictingBlocks) {
        if (conflictingBlocks == null || conflictingBlocks.isEmpty()) {
            return desiredStart;
        }

        // Find latest conflict end time
        Instant latestConflictEnd = conflictingBlocks.stream()
                .map(ResourceTimeBlock::getConflictEndUtc)
                .max(Instant::compareTo)
                .orElse(desiredStart);

        return latestConflictEnd;
    }
    public String formatConflictMessage(List<ResourceTimeBlock> conflicts) {
        if (conflicts.isEmpty()) {
            return "No conflicts";
        }

        StringBuilder message = new StringBuilder("Resource is booked during:\n");
        conflicts.forEach(conflict -> {
            message.append(String.format("- %s to %s (by %s)\n",
                    conflict.getStartTimeUtc(),
                    conflict.getEndTimeUtc(),
                    conflict.getUser().getDisplayName()
            ));
        });

        return message.toString();
    }
}
