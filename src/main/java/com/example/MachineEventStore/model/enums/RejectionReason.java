package com.example.MachineEventStore.model.enums;

import lombok.Data;
import lombok.RequiredArgsConstructor;


public enum RejectionReason {
    INVALID_DURATION("Duration must be between 0 and 6 hours"),
    FUTURE_EVENT_TIME("Event time cannot be more than 15 minutes in the future"),
    DUPLICATE_EVENT("Duplicate event ignored"),
    INVALID_PAYLOAD("Invalid or incomplete event data");
  private final String message;
    RejectionReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
