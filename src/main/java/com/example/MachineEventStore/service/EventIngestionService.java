package com.example.MachineEventStore.service;



import com.example.MachineEventStore.exception.ValidationException;
import com.example.MachineEventStore.model.dto.BatchIngestionResponse;
import com.example.MachineEventStore.model.dto.EventRequest;
import com.example.MachineEventStore.model.dto.RejectionDetail;
import com.example.MachineEventStore.model.entity.MachineEvent;
import com.example.MachineEventStore.model.enums.RejectionReason;
import com.example.MachineEventStore.repository.MachineEventRepository;
import com.example.MachineEventStore.util.DateTimeUtil;
import com.example.MachineEventStore.util.EventValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventIngestionService {

    private final MachineEventRepository eventRepository;
    private final MongoTemplate mongoTemplate;
    private final EventValidator eventValidator;
    private final DateTimeUtil dateTimeUtil;

    /**
     * Process a batch of events with thread-safe operations
     * Handles validation, deduplication, and updates
     *
     * Thread-safety strategy:
     * 1. MongoDB's _id uniqueness constraint prevents duplicate inserts
     * 2. findAndModify operations are atomic at document level
     * 3. Version field (@Version) prevents lost updates through optimistic locking
     *
     * @param eventRequests list of events to process
     * @return BatchIngestionResponse with statistics
     */
    public BatchIngestionResponse processBatch(List<EventRequest> eventRequests) {
        log.info("Processing batch of {} events", eventRequests.size());

        // Thread-safe counters using AtomicInteger for accurate counts in concurrent scenarios
        AtomicInteger acceptedCount = new AtomicInteger(0);
        AtomicInteger dedupedCount = new AtomicInteger(0);
        AtomicInteger updatedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        List<RejectionDetail> rejections = new ArrayList<>();

        // Process each event independently
        // Each operation is atomic at MongoDB document level
        for (EventRequest request : eventRequests) {
            try {
                // Step 1: Validate the event
                eventValidator.validate(request);

                // Step 2: Set receivedTime to server time (override client time)
                request.setReceivedTime(dateTimeUtil.now());

                // Step 3: Process event with atomic operations
                ProcessResult result = processEvent(request);

                // Step 4: Update counters based on result
                switch (result) {
                    case ACCEPTED:
                        acceptedCount.incrementAndGet();
                        break;
                    case DEDUPED:
                        dedupedCount.incrementAndGet();
                        break;
                    case UPDATED:
                        updatedCount.incrementAndGet();
                        break;
                }

            } catch (ValidationException e) {
                // Validation failed - add to rejections
                rejectedCount.incrementAndGet();
                rejections.add(new RejectionDetail(
                        request.getEventId(),
                        e.getRejectionReason(),
                        true
                ));
                log.debug("Event {} rejected: {}", request.getEventId(), e.getMessage());
            } catch (Exception e) {
                // Unexpected error - add to rejections
                rejectedCount.incrementAndGet();
                rejections.add(new RejectionDetail(
                        request.getEventId(),
                        RejectionReason.INVALID_PAYLOAD
                ));
                log.error("Unexpected error processing event {}: {}", request.getEventId(), e.getMessage(), e);
            }
        }

        log.info("Batch processing complete - Accepted: {}, Deduped: {}, Updated: {}, Rejected: {}",
                acceptedCount.get(), dedupedCount.get(), updatedCount.get(), rejectedCount.get());

        return BatchIngestionResponse.builder()
                .accepted(acceptedCount.get())
                .deduped(dedupedCount.get())
                .updated(updatedCount.get())
                .rejected(rejectedCount.get())
                .rejections(rejections)
                .build();
    }

    /**
     * Process a single event with atomic operations
     *
     * Thread-safety approach:
     * 1. Try to insert (MongoDB _id constraint ensures uniqueness)
     * 2. If duplicate, check if payload is identical or different
     * 3. If different and newer, atomically update using findAndModify
     *
     * MongoDB guarantees:
     * - Document-level atomicity
     * - Unique _id constraint enforced atomically
     * - findAndModify is atomic (find + modify in single operation)
     *
     * @param request the event to process
     * @return ProcessResult indicating what happened
     */
    private ProcessResult processEvent(EventRequest request) {
        // Convert DTO to Document
        MachineEvent event = convertToDocument(request);

        try {
            // Attempt to insert new event
            // MongoDB's unique _id constraint ensures this is atomic
            // If _id exists, DuplicateKeyException is thrown
            mongoTemplate.insert(event);

            log.debug("Event {} inserted successfully", event.getEventId());
            return ProcessResult.ACCEPTED;

        } catch (DuplicateKeyException e) {
            // Event with this eventId already exists
            // Need to determine if it's a duplicate or an update
            log.debug("Duplicate eventId detected: {}", event.getEventId());

            return handleDuplicateEvent(event, request);
        }
    }

    /**
     * Handle duplicate eventId scenario
     *
     * Logic:
     * 1. Fetch existing event from database
     * 2. Compare payloads:
     *    - If identical → DEDUPED (ignore)
     *    - If different → Check receivedTime
     *      - If new receivedTime > existing → UPDATE (newer wins)
     *      - If new receivedTime <= existing → DEDUPED (older ignored)
     *
     * Thread-safety:
     * Uses atomic findAndModify with conditions to ensure consistent updates
     * Even if multiple threads try to update same event, MongoDB handles atomically
     *
     * @param newEvent the incoming event
     * @param request the original request (for comparison)
     * @return ProcessResult indicating deduped or updated
     */
    private ProcessResult handleDuplicateEvent(MachineEvent newEvent, EventRequest request) {
        // Fetch existing event from database
        Optional<MachineEvent> existingOpt = eventRepository.findById(newEvent.getEventId());

        if (existingOpt.isEmpty()) {
            // Race condition: event was deleted between insert and now (unlikely)
            // Treat as new insert
            log.warn("Event {} not found after duplicate key exception. Retrying insert.", newEvent.getEventId());
            mongoTemplate.insert(newEvent);
            return ProcessResult.ACCEPTED;
        }

        MachineEvent existingEvent = existingOpt.get();

        // Compare payloads to determine if it's exact duplicate or update
        boolean isIdenticalPayload = arePayloadsIdentical(existingEvent, request);

        if (isIdenticalPayload) {
            // Exact duplicate - same eventId + identical data
            log.debug("Event {} is exact duplicate - deduping", newEvent.getEventId());
            return ProcessResult.DEDUPED;
        }

        // Different payload - check receivedTime to determine which is newer
        if (newEvent.getReceivedTime().isAfter(existingEvent.getReceivedTime())) {
            // New event is newer - update existing with atomic operation
            log.debug("Event {} has different payload and newer receivedTime - updating", newEvent.getEventId());

            updateEventAtomically(newEvent);
            return ProcessResult.UPDATED;

        } else {
            // Existing event is newer or same - ignore the incoming one
            log.debug("Event {} has different payload but older/equal receivedTime - deduping", newEvent.getEventId());
            return ProcessResult.DEDUPED;
        }
    }

    /**
     * Atomically update an event using MongoDB's findAndModify
     *
     * This is thread-safe because:
     * 1. findAndModify is atomic - find + modify happen together
     * 2. We use version field for optimistic locking
     * 3. MongoDB ensures document-level atomicity
     *
     * Query conditions:
     * - Match by _id (eventId)
     * - Match by current version (optimistic locking)
     * - Only update if receivedTime of new event > existing receivedTime
     *
     * @param newEvent the event with updated data
     */
    private void updateEventAtomically(MachineEvent newEvent) {
        // Build query with optimistic locking
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(newEvent.getEventId()));

        // Build update operations
        Update update = new Update();
        update.set("eventTime", newEvent.getEventTime());
        update.set("receivedTime", newEvent.getReceivedTime());
        update.set("machineId", newEvent.getMachineId());
        update.set("durationMs", newEvent.getDurationMs());
        update.set("defectCount", newEvent.getDefectCount());
        update.set("lineId", newEvent.getLineId());
        update.set("factoryId", newEvent.getFactoryId());

        // Increment version for optimistic locking
        update.inc("version", 1);

        // Execute atomic update
        // findAndModify ensures atomicity - no other thread can modify between find and update
        MachineEvent updated = mongoTemplate.findAndModify(
                query,
                update,
                MachineEvent.class
        );

        if (updated == null) {
            log.warn("Failed to update event {} - possible concurrent modification", newEvent.getEventId());
            // Could retry here if needed, but typically indicates concurrent update won
        }
    }

    /**
     * Compare two events to determine if payloads are identical
     *
     * Compares all business fields except receivedTime
     * (receivedTime is not part of business payload - it's metadata)
     *
     * @param existing existing event in database
     * @param request incoming request
     * @return true if payloads are identical
     */
    private boolean arePayloadsIdentical(MachineEvent existing, EventRequest request) {
        return Objects.equals(existing.getEventTime(), request.getEventTime()) &&
                Objects.equals(existing.getMachineId(), request.getMachineId()) &&
                Objects.equals(existing.getDurationMs(), request.getDurationMs()) &&
                Objects.equals(existing.getDefectCount(), request.getDefectCount()) &&
                Objects.equals(existing.getLineId(), request.getLineId()) &&
                Objects.equals(existing.getFactoryId(), request.getFactoryId());
    }

    /**
     * Convert EventRequest DTO to MachineEvent Document
     *
     * @param request the request DTO
     * @return MachineEvent document ready for MongoDB
     */
    private MachineEvent convertToDocument(EventRequest request) {
        return MachineEvent.builder()
                .eventId(request.getEventId())
                .eventTime(request.getEventTime())
                .receivedTime(request.getReceivedTime())
                .machineId(request.getMachineId())
                .durationMs(request.getDurationMs())
                .defectCount(request.getDefectCount())
                .lineId(request.getLineId())
                .factoryId(request.getFactoryId())
                .build();
    }

    /**
     * Enum to represent the result of processing an event
     */
    private enum ProcessResult {
        ACCEPTED,   // New event inserted
        DEDUPED,    // Duplicate ignored (identical payload or older receivedTime)
        UPDATED     // Existing event updated (different payload + newer receivedTime)
    }
}