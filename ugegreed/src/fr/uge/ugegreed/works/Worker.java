package fr.uge.ugegreed.works;

import fr.uge.ugegreed.ApplicationContext;
import fr.uge.ugegreed.RouteTable;
import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;
import fr.uge.ugegreed.records.Response;
import fr.uge.ugegreed.records.Task;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.LongStream;

public final class Worker {
    private static final short NB_THREADS = 5;
    private static final int TIMEOUT_MS = 10_000; // 10 seconds
    private static final Logger logger = Logger.getLogger(Worker.class.getName());

    private final HashMap<Id, Map<Long, Packet.Work.WorkRequest>> pendingRequests = new HashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);

    // 1 response = 1 result for a range of the ranges' list
    private static List<Response> run(Task task) {
        var jar = task.checker();
        return JarHandler.handleJar(jar.javaUrl(), jar.classPath(), task.range());
    }

    private static ApplicationContext resolveDestinationContext(Id destinationId, RouteTable routeTable) {
        var destinationContextOpt = routeTable.getContext(destinationId);
        return destinationContextOpt.orElseGet(
                // destination application is now disconnected : we take the mother
                () -> routeTable.getContext(routeTable.getMother()).orElseThrow()
        );
    }

    private static void sendWorkResponse(Packet.Work.WorkAssignment workAssignment, List<Response> responses, RouteTable routeTable) {
        var destinationId = workAssignment.idSrc();
        var destinationContext = resolveDestinationContext(destinationId, routeTable);
        responses.forEach(response -> {
            var workResponse = new Packet.Work.WorkResponse(
                    destinationId,
                    destinationId,
                    workAssignment.requestId(),
                    response
            );

            destinationContext.queueMessage(workResponse);
        });
    }

    private static void sendWorkResponseTimeout(Packet.Work.WorkAssignment workAssignment, RouteTable routeTable) {
        var range = workAssignment.range();
        var responses = LongStream.range(range.inf(), range.sup())
                .mapToObj(i -> new Response(i, Response.JOB_TIMEOUT, ""))
                .toList();
        sendWorkResponse(workAssignment, responses, routeTable);
    }

    private void execute(Packet.Work.WorkAssignment workAssignment, Callable<List<Response>> callable, RouteTable routeTable) {
        var future = executor.submit(callable);
        try {
            var results = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            sendWorkResponse(workAssignment, results, routeTable);
        } catch(TimeoutException e) {
            sendWorkResponseTimeout(workAssignment, routeTable);
        } catch(ExecutionException e) {
            executor.shutdown();
            throw new AssertionError(e);
        } catch(InterruptedException e) {
            logger.warning("Thread interrupted during computation");
            executor.shutdown();
        }
    }

    public void submitWorkRequest(Packet.Work.WorkRequest workRequest, long nbComputation) {
        var newWorkRequest = new Packet.Work.WorkRequest(
                workRequest.idSrc(),
                workRequest.idDest(),
                workRequest.requestId(),
                workRequest.checker(),
                workRequest.range(),
                nbComputation
        );
        var sourceId = newWorkRequest.idSrc();
        var submittedTasks = pendingRequests.getOrDefault(sourceId, new HashMap<>());
        submittedTasks.putIfAbsent(newWorkRequest.requestId(), newWorkRequest);
        pendingRequests.putIfAbsent(sourceId, submittedTasks);
    }

    public void submitWorkAssignment(Packet.Work.WorkAssignment workAssignment, Path resultFile, RouteTable routeTable) {
        var submittedTasks = pendingRequests.get(workAssignment.idSrc());
        if(submittedTasks == null) {
            throw new IllegalStateException("no work request was previously submitted for this assignment");
        }

        var requestId = workAssignment.requestId();
        var workRequest = submittedTasks.get(requestId);
        if(workRequest == null) {
            throw new IllegalStateException("no work request was previously submitted for this assignment");
        }

        var task = new Task(workRequest.checker(), workRequest.range(), resultFile);
        execute(workAssignment, () -> run(task), routeTable);
        submittedTasks.remove(requestId);
    }
}
