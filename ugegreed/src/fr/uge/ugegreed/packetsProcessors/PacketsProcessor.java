package fr.uge.ugegreed.packetsProcessors;

import fr.uge.ugegreed.ApplicationContext;
import fr.uge.ugegreed.RouteTable;
import fr.uge.ugegreed.records.Packet;
import fr.uge.ugegreed.works.ResponseCollector;

public sealed interface PacketsProcessor<T extends Packet> permits Connector, Disconnector, Calculator {
    enum State {
        CONNECTING,
        CONNECTED,
        CONNEXION_REFUSED,
        DISCONNECTING,
        DISCONNECTION_DENIED,
        RECONNECTING,
        CLOSED,
        REQUEST_ACCEPTED,
        REQUEST_DENIED,
        WORK_SUBMITTED,
        COMPUTING,
        RECEIVED_WORK_RESPONSE
    }

    record PacketsHandler(Connector connector, Disconnector disconnector, Calculator calculator) {
        public PacketsHandler(ResponseCollector responseCollector) {
            this(new Connector(), new Disconnector(), new Calculator(responseCollector));
        }
    }

    State process(ApplicationContext context, T packet, RouteTable routeTable, State currentState);
}
