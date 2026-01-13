package com.example.MachineEventStore.repository;


import com.example.MachineEventStore.model.entity.MachineEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MachineEventRepository extends MongoRepository<MachineEvent, String> {



       List<MachineEvent> findByMachineIdAndEventTimeBetween(
            String machineId,
            Instant start,
            Instant end
    );


    @Query("{ 'machineId': ?0, 'eventTime': { $gte: ?1, $lt: ?2 }, 'defectCount': { $ne: -1 } }")
    List<MachineEvent> findByMachineIdAndEventTimeRangeExcludingUnknownDefects(
            String machineId,
            Instant start,
            Instant end
    );


    List<MachineEvent> findByFactoryIdAndEventTimeBetween(
            String factoryId,
            Instant from,
            Instant to
    );


    @Query("{ 'factoryId': ?0, 'eventTime': { $gte: ?1, $lt: ?2 }, 'defectCount': { $ne: -1 } }")
    List<MachineEvent> findByFactoryIdAndEventTimeRangeExcludingUnknownDefects(
            String factoryId,
            Instant from,
            Instant to
    );

        long countByMachineIdAndEventTimeBetween(
            String machineId,
            Instant start,
            Instant end
    );


    List<MachineEvent> findByLineIdAndEventTimeBetween(
            String lineId,
            Instant from,
            Instant to
    );
}
