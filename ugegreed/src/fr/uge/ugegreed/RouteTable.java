package fr.uge.ugegreed;

import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class represents a route table of this type.<br>
 * To contact machine A, the route is going through machine B.<br>
 * In reality, it is a simple hashmap for each application.
 *
 * @author Axel BELIN and Thomas VELU.
 */
public class RouteTable {
  private final Map<Id, Id> routeTable;
  private final Map<Id, ApplicationContext> contextTable;
  private final Id selfId;
  private Id motherId; // mutable because you can change your mother.

  /**
   * RouteTable constructor.<br>
   * It takes an id as a parameter.<br>
   * This id represents the current machine.
   *
   * @param firstId Id, the current machine.
   */
  public RouteTable(Id firstId) {
    Objects.requireNonNull(firstId);
    routeTable = new HashMap<>();
    contextTable = new HashMap<>();
    routeTable.put(firstId, firstId);
    selfId = firstId;
    // We use put because we are sure that the routeTable was freshly created before
    // And doesn't contain any keys right now.
    motherId = firstId; // By default, you are your own mother
  }

  // Holy guacamole, what is this ???
  // Are we in Python or what ?
  // It's disgusting but necessary, unfortunately
  public Id getMother() {
    return motherId;
  }

  /**
   * Adds an id to the route table.<br>
   * If the id is already in the route table, it replaces the current value.
   *
   * @param newId Id, the machine's id.
   * @param route Id, through which machine should we talk to, to be able to contact the correct machine.
   */
  public void add(Id newId, Id route, ApplicationContext context) {
    Objects.requireNonNull(newId);
    Objects.requireNonNull(route);
    Objects.requireNonNull(context);
    routeTable.merge(newId, route, (oldValue, newValue) -> newValue);
    contextTable.merge(newId, context, (oldValue, newValue) -> newValue);
  }

  public void remove(Id sourceId) {
    Objects.requireNonNull(sourceId);
    routeTable.remove(sourceId);
    contextTable.remove(sourceId);
  }

  public Set<Id> getDaughters() {
    return routeTable.values().stream()
            .filter(id -> !id.equals(selfId) && !id.equals(motherId))
            .collect(Collectors.toUnmodifiableSet());
  }

  public List<Id> getNeighbours() {
    return routeTable.values().stream()
            .filter(id -> !id.equals(selfId))
            .collect(Collectors.toList());
  }

  // for disconnection, we include selfId in the list because it has to appear in the query
  public List<Id> daughtersForDisconnection() {
    return routeTable.values().stream().filter(id -> !id.equals(motherId)).toList();
  }

  public Optional<ApplicationContext> getContext(Id id) {
    return Optional.ofNullable(contextTable.get(id));
  }

  public void sendTo(Packet packet, Id destination) {
    var context = contextTable.get(destination);
    if(context == null) {
      return;
    }
    context.queueMessage(packet);
  }

  public void sendToNeighbours(Packet packet, Predicate<Id> neighboursToExclude) {
    routeTable.values().stream()
            .filter(id -> !id.equals(selfId) && !neighboursToExclude.test(id))
            .distinct()
            .map(contextTable::get)
            .filter(Objects::nonNull)
            .forEach(context -> context.queueMessage(packet));
  }

  public void sendToNeighbours(Packet packet) {
    sendToNeighbours(packet, id -> false); // exclude nobody
  }

  public void sendToDaughters(Packet packet) {
    getDaughters().stream()
            .map(contextTable::get)
            .filter(Objects::nonNull)
            .forEach(context -> context.queueMessage(packet));
  }

  public void findAndReplace(Id toFindId, Id replacementId) {
    routeTable.replaceAll((k, v) -> {
      if(v.equals(toFindId)) {
        return replacementId;
      }

      return v;
    });
    var oldContext = contextTable.get(toFindId);
    var replacementContext = contextTable.get(replacementId);
    if(oldContext == null || replacementContext == null) {
      return;
    }

    contextTable.replaceAll((k, v) -> {
      if(v.equals(oldContext)) {
        return replacementContext;
      }

      return v;
    });
  }

  /**
   * Returns every IDs.
   *
   * @return List of ID, every machine connected to this route table.
   */
  public List<Id> getAllId() {
    return routeTable.keySet().stream().toList();
  }

  // beurk
  public void changeMother(Id motherId) {
    Objects.requireNonNull(motherId);
    this.motherId = motherId;
  }

  public Id id() {
    return selfId;
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();
    for(var entry: routeTable.entrySet()) {
      builder
              .append("source : ")
              .append(entry.getKey())
              .append(" ==> destination : ")
              .append(entry.getValue())
              .append('\n');
    }
    return builder.toString();
  }
}
