package com.example.MachineEventStore.model.dto;

import com.example.MachineEventStore.model.enums.RejectionReason;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectionDetail {

    private String eventId;
    private String reason;

    // Constructor that accepts RejectionReason enum
    public RejectionDetail(String eventId,  RejectionReason rejectionReason ) {
        this.eventId = eventId;
        this.reason = rejectionReason.name(); // Store enum name as string
    }

    // Optional: Include the message as well
    private String message;

    public RejectionDetail(String eventId, RejectionReason rejectionReason, boolean includeMessage) {
        this.eventId = eventId;
        this.reason = rejectionReason.name();
        if (includeMessage) {
            this.message = rejectionReason.getMessage();
        }
    }
}