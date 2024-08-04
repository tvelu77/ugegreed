package fr.uge.ugegreed.packetsProcessors;

import fr.uge.ugegreed.ApplicationContext;
import fr.uge.ugegreed.RouteTable;
import fr.uge.ugegreed.records.Packet;
import fr.uge.ugegreed.records.Task;
import fr.uge.ugegreed.works.ResponseCollector;
import fr.uge.ugegreed.works.Worker;

public final class Calculator implements PacketsProcessor<Packet.Work> {
    private final Worker worker = new Worker();
    private final ResponseCollector responseCollector;

    Calculator(ResponseCollector responseCollector) {
        this.responseCollector = responseCollector;
    }

    // return true if packet was transferred or ignored
    private boolean transfer(Packet.Work packet, RouteTable routeTable) {
        var destinationId = packet.idDest();
        if(routeTable.id().equals(destinationId)) {
            return false;
        }

        routeTable.sendTo(packet, destinationId);
        return true;
    }

    @Override
    public State process(ApplicationContext context, Packet.Work packet, RouteTable routeTable, State currentState) {
        if(transfer(packet, routeTable)) {
            return currentState;
        }

        return switch (packet) {
            case Packet.Work.WorkRequest workRequest -> {
                var nbAvailableUcs = 0L;
                if(currentState != State.DISCONNECTING) {
                    nbAvailableUcs = 42L; // TODO algo qui caclcule le nb d'ucs compris entre 0 et nbMax UC
                }

                var workAvailability = new Packet.Work.WorkAvailability(
                        workRequest.idDest(),
                        workRequest.idSrc(),
                        workRequest.requestId(),
                        nbAvailableUcs
                );
                context.queueMessage(workAvailability);

                if(nbAvailableUcs == 0) {
                    yield State.REQUEST_DENIED;
                }

                worker.submitWorkRequest(workRequest, nbAvailableUcs);
                yield State.REQUEST_ACCEPTED;
            }
            case Packet.Work.WorkAvailability availabilityResponse -> {
                var nbAvailableUcs = availabilityResponse.nbComputation();
                if(nbAvailableUcs == 0) {
                    yield State.REQUEST_DENIED;
                }

                var range = new Task.Range(10, 20); // TODO algo qui calcule lenombre de ranges en fonction du nbAvailableUcs d'une machine
                var workAssignment = new Packet.Work.WorkAssignment(
                        availabilityResponse.idDest(),
                        availabilityResponse.idSrc(),
                        availabilityResponse.requestId(),
                        range
                );
                context.queueMessage(workAssignment);
                yield State.WORK_SUBMITTED;
            }
            case Packet.Work.WorkAssignment workAssignment -> {
                worker.submitWorkAssignment(
                        workAssignment,
                        responseCollector.getResultFilePath(workAssignment.requestId()),
                        routeTable
                );
                yield State.COMPUTING;
            }
            case Packet.Work.WorkResponse workResponse -> {
                responseCollector.saveResponseIntoResultFile(workResponse);
                yield State.RECEIVED_WORK_RESPONSE;
            }
        };
    }
}
