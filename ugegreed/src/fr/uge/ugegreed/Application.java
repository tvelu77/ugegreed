package fr.uge.ugegreed;

import fr.uge.ugegreed.packetsProcessors.PacketsProcessor;
import fr.uge.ugegreed.records.Id;
import fr.uge.ugegreed.records.Packet;
import fr.uge.ugegreed.records.Task;
import fr.uge.ugegreed.works.ResponseCollector;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Represents the application.<br>
 * The application is used to connect two machines (or more)
 * and to send a workload to multiple machine.
 *
 * @author Axel BELIN and Thomas VELU.
 */
public final class Application {
  private static final Logger logger = Logger.getLogger(Application.class.getName());

  private final ResponseCollector responseCollector = new ResponseCollector();
  private final PacketsProcessor.PacketsHandler packetsHandler;
  private final SocketChannel sc;
  private final Selector selector;
  private final InetSocketAddress motherAddress;
  private final Id applicationId;
  private final Thread consoleThread;
  private final ServerSocketChannel serverSocketChannel;
  private final RouteTable routeTable;
  private final Object lock = new Object();
  private final int totalWorkRange = 1000;
  private final boolean rootMode;
  private ApplicationContext motherContext;
  private int currentWorkLoad = 0;
  private long workToSend = 0;
  private long requestId = -1;
  private List<Id> contactForWork;

  public Application(int port) throws IOException {
    rootMode = true;
    packetsHandler = new PacketsProcessor.PacketsHandler(responseCollector);
    applicationId = new Id(new InetSocketAddress("0.0.0.0", port));
    routeTable = new RouteTable(applicationId);
    motherAddress = null;
    selector = Selector.open();
    sc = null;
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(applicationId.socketAddress());
    var console = new Console(
            this::onStartTask,
            () -> System.out.println("You are in root mode, you cannot disconnect !"),
            () -> System.out.println(routeTable)
    );
    consoleThread = Thread.ofPlatform().daemon().unstarted(console::run);
  }

  /**
   * Application constructor.<br>
   * This constructor is for the node mode.
   *
   * @param port Int, the node's port.
   * @param serverAddress InetSocketAddress, father's address.
   * @throws IOException If the application address or socket channel couldn't be opened
   */
  public Application(int port, InetSocketAddress serverAddress) throws IOException {
    rootMode = false;
    packetsHandler = new PacketsProcessor.PacketsHandler(responseCollector);
    applicationId = new Id(new InetSocketAddress("0.0.0.0", port));
    routeTable = new RouteTable(applicationId);
    this.motherAddress = serverAddress;
    this.selector = Selector.open();
    sc = SocketChannel.open();

    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(applicationId.socketAddress());
    var console = new Console(
            this::onStartTask,
            this::onDisconnectApplication,
            () -> System.out.println(routeTable)
    );
    consoleThread = Thread.ofPlatform().daemon().unstarted(console::run);
  }

  private void onDisconnectApplication() {
    var disconnectPacket = new Packet.Disconnection.DisconnectionRequest(
            routeTable.daughtersForDisconnection()
    );
    motherContext.enqueue(disconnectPacket);
    selector.wakeup();
  }

  private void onStartTask(Task task) {
    requestId = System.currentTimeMillis();
    responseCollector.addResultFilename(requestId, task.resultsFilename());
    contactForWork = routeTable.getNeighbours();
    workToSend = task.range().sup();
    var workLoadForOneMachine = workToSend / contactForWork.size(); // TODO attention au diviser par 0 exception quand juste ROOT est démarré
    var firstMachine = contactForWork.get(0); // Just horrible...
    contactForWork.remove(firstMachine); // Holy macaroni, a remove ?!?!
    var workRequest = new Packet.Work.WorkRequest(
            applicationId,
            firstMachine,
            requestId,
            task.checker(),
            task.range(),
            workLoadForOneMachine
    );
    routeTable.sendTo(workRequest, firstMachine);
    selector.wakeup();
  }

  private void processCommandsForMother() {
    for (;;) {
      var msg = motherContext.peek();
      if (msg == null) {
        break;
      }

      motherContext.nextPacket();
      motherContext.queueMessage(msg);
    }
  }

  private void processCommands() {
    synchronized (lock) {
      if(!rootMode && motherContext.currentState() == PacketsProcessor.State.DISCONNECTING) {
        processCommandsForMother();
        return;
      }

      if(!rootMode) {
        processCommandsForMother();
      }
    }
  }

  private void pingServer() {
    synchronized (lock) {
      var connectPacket = new Packet.Connection.Connect(applicationId);
      motherContext.enqueue(connectPacket);
      selector.wakeup();
    }
  }

  public void launch() throws IOException {
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    if(!rootMode) {
      sc.configureBlocking(false);
      var key = sc.register(selector, SelectionKey.OP_CONNECT);
      motherContext = new ApplicationContext(this, key);
      key.attach(motherContext);
      routeTable.changeMother(new Id(motherAddress));
      sc.connect(motherAddress);
      pingServer();
      logger.info("Application started, connecting to : " + motherAddress);
    } else {
      logger.info("Application started in ROOT mode");
    }
    consoleThread.start();
    while (!Thread.interrupted()) {
      try {
        selector.select(this::treatKey);
        processCommands();
      } catch (UncheckedIOException tunneled) {
        throw tunneled.getCause();
      }
    }
  }

  private void treatKey(SelectionKey key) {
    try {
      if (key.isValid() && key.isAcceptable()) {
        doAccept();
      }
    } catch (IOException ioe) {
      // lambda call in select requires to tunnel IOException
      throw new UncheckedIOException(ioe);
    }

    try {
      if (key.isValid() && key.isConnectable()) {
        motherContext.doConnect();
      }
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }

    var context = (ApplicationContext) key.attachment();

    try {
      if (key.isValid() && key.isWritable()) {
        context.doWrite();
      }
      if (key.isValid() && key.isReadable()) {
        context.doRead(routeTable, packetsHandler);
      }
    } catch(IOException e) {
      logger.info("connection closed with client due to IO exception");
      silentlyClose(key);
    }
  }

  private void doAccept() throws IOException {
    var sc = serverSocketChannel.accept();
    if (sc == null) {
      logger.warning("selector has lied");
      return;
    }
    sc.configureBlocking(false);
    var clientKey = sc.register(selector, SelectionKey.OP_READ);
    var daughterContext = new ApplicationContext(this, clientKey);
    clientKey.attach(daughterContext);
    logger.info("connection accepted : " + sc.getRemoteAddress());
  }

  private void silentlyClose(SelectionKey key) {
    var sc = (Channel) key.channel();
    try {
      sc.close();
    } catch (IOException e) {
      // ignore exception
    }

    logger.info("Connexion closed with  : " + sc);
  }

  void connectToNewMother(Id newMother, Set<Id> daughters) throws IOException {
    var sc = SocketChannel.open();
    sc.configureBlocking(false);
    var motherKey = sc.register(selector, SelectionKey.OP_CONNECT);
    motherContext = new ApplicationContext(this, motherKey);
    motherKey.attach(motherContext);
    sc.connect(newMother.socketAddress());
    routeTable.add(newMother, newMother, motherContext);
    motherContext.doConnect();
    var reconnect = new Packet.Disconnection.Reconnect(
            routeTable.id(), List.copyOf(daughters));
    motherContext.queueMessage(reconnect);
  }
}
