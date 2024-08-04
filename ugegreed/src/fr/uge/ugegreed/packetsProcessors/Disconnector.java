package fr.uge.ugegreed.packetsProcessors;

import fr.uge.ugegreed.ApplicationContext;
import fr.uge.ugegreed.RouteTable;
import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public final class Disconnector implements PacketsProcessor<Packet.Disconnection> {
    private static final Logger logger = Logger.getLogger(Disconnector.class.getName());

    private final HashSet<Id> connectingDaughters = new HashSet<>(); // les filles qui doivent se reconnecter à moi après une déco.

    private Id disconnectingDaughter;

    // pour signaler que D doit être supprimé des tables de routages :
    // Mother envoie à toutes ses voisines et à D
    private void broadcastDisconnected(RouteTable routeTable) {
        var disconnected = new Packet.Disconnection.Disconnected(routeTable.id(), disconnectingDaughter); // M, D
        routeTable.sendToNeighbours(disconnected);
        routeTable.remove(disconnectingDaughter);
        disconnectingDaughter = null;
    }

    @Override
    public State process(ApplicationContext context, Packet.Disconnection disconnectionPacket, RouteTable routeTable, State currentState) {
        return switch (disconnectionPacket) {
            case Packet.Disconnection.DisconnectionRequest request -> {
                // Si une autre de ses applications filles est en train de se déconnecter,
                // elle attend que cette déconnexion soit terminée
                if(disconnectingDaughter != null) {
                    logger.info("A daughter is still disconnecting");
                    yield State.DISCONNECTING;
                }

                var daughters = request.listDaughters();
                if(daughters.isEmpty()) {
                    throw new IllegalStateException("debug : daughters list is empty");
                }

                var daughtersDeque = new ArrayDeque<>(request.listDaughters());
                disconnectingDaughter = daughtersDeque.pollFirst();
                if(currentState == State.DISCONNECTING) {
                    var denied = new Packet.Disconnection.DisconnectionDenied();
                    context.queueMessage(denied);
                    yield currentState;
                }

                var disconnectGranted = new Packet.Disconnection.DisconnectionGranted();
                context.queueMessage(disconnectGranted);

                // Elle va ensuite attendre que les filles de D se connectent sur son port d'écoute avec des trames Reconnect
                if(daughtersDeque.isEmpty()) {
                    broadcastDisconnected(routeTable);
                } else {
                    connectingDaughters.addAll(Set.copyOf(daughtersDeque));
                }

                yield currentState;
            }
            case Packet.Disconnection.Reconnect reconnect -> {
                // A la réception de cette trame,
                // si l'id correspond bien à une id de list_daughters,
                // l'application M va mettre à jour sa table de routage en associant à tous les membres de list_ancesters, l'identifiant id.

                var id = reconnect.id();
                if(!connectingDaughters.contains(id)) {
                    yield currentState;
                }

                var ancestors = reconnect.ids();
                routeTable.add(id, id, context);
                // to talk with all ancestors, M must talk to Id
                ancestors.forEach(destinationId -> routeTable.add(destinationId, id, context));

                // daughter id is now reconnected
                connectingDaughters.remove(id);

                // waiting for others to reconnect
                if(!connectingDaughters.isEmpty()) {
                    yield currentState;
                }

                // Toutes les applications filles de D se sont reconnectées,
                // il ne reste plus de mention de D dans la table routage de M.
                broadcastDisconnected(routeTable); // OwO, faut envoyer la table de routage à tout le monde !
                yield currentState;
            }
            case Packet.Disconnection.DisconnectionGranted ignored -> {
                var pleaseReconnect = new Packet.Disconnection.PleaseReconnect(routeTable.getMother());
                routeTable.sendToDaughters(pleaseReconnect);
                yield State.DISCONNECTING;
            }
            case Packet.Disconnection.Disconnected disconnected -> {
                var sourceId = disconnected.sourceId();
                var applicationId = routeTable.id();
                var disconnectingApplicationId = disconnected.id();
                routeTable.remove(disconnectingApplicationId);
                routeTable.findAndReplace(disconnectingApplicationId, sourceId);
                routeTable.sendToDaughters(disconnected); // retransmettre au filles
                if(disconnectingApplicationId.equals(applicationId)) { // ferme les connexions avec ses voisines
                    yield State.CLOSED; // close all connections with Neighbours
                }

                // TODO ne plus considérer les calculs affactés à disconnectingApplicationId
                yield currentState;
            }
            case Packet.Disconnection.DisconnectionDenied __ -> {
                logger.info("Mother is already disconnecting");
                yield State.DISCONNECTION_DENIED;
            }
            case Packet.Disconnection.PleaseReconnect pleaseReconnect -> State.RECONNECTING; // Elle attendra que D ferme sa connexion pour la fermer.
        };
    }
}
