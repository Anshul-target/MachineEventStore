package com.example.MachineEventStore.model.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchIngestionResponse {

    private int accepted;
    private int deduped;
    private int updated;
    private int rejected;

    @Builder.Default
    private List<RejectionDetail> rejections = new ArrayList<>();
}