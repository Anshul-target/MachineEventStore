package  com.example.MachineEventStore.exception;



import com.example.MachineEventStore.model.enums.RejectionReason;
import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {

    private final RejectionReason rejectionReason;
    private final String eventId;

    /**
     * Constructor with message and rejection reason
     *
     * @param message error message
     * @param rejectionReason why the validation failed
     */
    public ValidationException(String message, RejectionReason rejectionReason) {
        super(message);
        this.rejectionReason = rejectionReason;
        this.eventId = null;
    }

    /**
     * Constructor with message, rejection reason, and event ID
     *
     * @param message error message
     * @param rejectionReason why the validation failed
     * @param eventId the ID of the event that failed validation
     */
    public ValidationException(String message, RejectionReason rejectionReason, String eventId) {
        super(message);
        this.rejectionReason = rejectionReason;
        this.eventId = eventId;
    }

    /**
     * Constructor with rejection reason only (uses default message)
     *
     * @param rejectionReason why the validation failed
     */
    public ValidationException(RejectionReason rejectionReason) {
        super(rejectionReason.getMessage());
        this.rejectionReason = rejectionReason;
        this.eventId = null;
    }
}