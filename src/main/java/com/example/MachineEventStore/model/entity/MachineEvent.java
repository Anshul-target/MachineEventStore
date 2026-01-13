package com.example.MachineEventStore.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.Instant;

@Document(collection = "machine_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
        @CompoundIndex(name = "machine_time_idx", def = "{'machineId': 1, 'eventTime': 1}"),
        @CompoundIndex(name = "line_time_idx", def = "{'lineId': 1, 'eventTime': 1}"),
        @CompoundIndex(name = "factory_time_idx", def = "{'factoryId': 1, 'eventTime': 1}")
})
public class MachineEvent {


    @Id
    private String eventId; // Using eventId as MongoDB _id for automatic uniqueness

    @Indexed
    private Instant eventTime;

    private Instant receivedTime;

    @Indexed
    private String machineId;

    private Long durationMs;

    private Integer defectCount;

    @Indexed
    private String lineId;

    @Indexed
    private String factoryId;

    @Version
    private Long version; // For optimistic locking
}
