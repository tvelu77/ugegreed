package fr.uge.ugegreed.packetsProcessors;

import fr.uge.ugegreed.ApplicationContext;
import fr.uge.ugegreed.RouteTable;
import fr.uge.ugegreed.records.Packet;
import java.util.logging.Logger;

public record Connector() implements PacketsProcessor<Packet.Connection> {
    private static final Logger logger = Logger.getLogger(Connector.class.getName());

    @Override
    public State process(ApplicationContext context, Packet.Connection value, RouteTable routeTable, State currentState) {
        return switch (value) {
            case Packet.Connection.Connect connect -> {
                if(currentState == State.DISCONNECTING) {
                    var connectKo = new Packet.Connection.ConnectKo();
                    context.queueMessage(connectKo);
                    yield State.CONNEXION_REFUSED;
                }

                var connectOk = new Packet.Connection.ConnectOk(routeTable.id(), routeTable.getAllId());
                context.queueMessage(connectOk);
                var newDaughterId = connect.idDaughter();
                routeTable.add(newDaughterId, newDaughterId, context);

                var addNode = new Packet.Connection.AddNode(routeTable.id(), newDaughterId);
                // Elle va ensuite envoyer une trame BROADCAST à toutes ses voisines (excepté DAUGHTER):
                routeTable.sendToNeighbours(addNode, id -> id.equals(newDaughterId));
                logger.info("Client connected : " + newDaughterId);
                yield currentState;
            }
            case Packet.Connection.ConnectOk connectOk -> {
                var ids = connectOk.ids();
                var motherId = connectOk.idMother();
                routeTable.changeMother(motherId);
                for (var id: ids) {
                    routeTable.add(id, motherId, context);
                }

                yield State.CONNECTED;
            }
            case Packet.Connection.ConnectKo ignored -> State.CONNEXION_REFUSED;
            case Packet.Connection.AddNode addNode -> {
         /*
         Elle va rajouter dans sa table de routage l'association id_daughter -> id_src.
         elle transmet à l'identique la trame à toutes ses voisines à l'exception de la voisine qui est à l'origine de la trame.
          */

                var routeToDaughterId = addNode.id();
                var idDaughter = addNode.idDaughter();
                routeTable.add(idDaughter, routeToDaughterId, context);
                var newAddNode = new Packet.Connection.AddNode(routeTable.id(), idDaughter);
                routeTable.sendToNeighbours(newAddNode, neighbour -> neighbour.equals(routeToDaughterId));
                yield currentState;
            }
        };
    }
}
