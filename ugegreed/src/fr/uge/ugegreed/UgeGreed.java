package fr.uge.ugegreed;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * UgeGreed class.<br>
 * The UgeGreed class is used to launch the UGEGreed application.<br>
 * The user launch the program by launching the UgeGreed class and providing arguments.
 *
 * @author Axel BELIN and Thomas VELU.
 */
public class UgeGreed {

  /**
   * Prints on the console, the correct ways to use the application.
   */
  private static void usage() {
    var rootString = """
            Usage (for creating a root) : UgeGreed <port>""";
    var nodeString = """
            Usage (for connection to a root) : UgeGreed <port> <host address> <host port>""";
    System.out.println(rootString);
    System.out.println(nodeString);
  }

  /**
   * The main method.<br>
   * It is called by the program when the user launch the application.<br>
   * We actively wait for 2 arguments (root mode) or 4 arguments (node mode).<br>
   * If the user doesn't give the exact amount, we print an error message.
   *
   * @param args Array of strings, the user's given parameters when launching the application.
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1 && args.length != 3) {
      usage();
      return;
    }
    if (args.length == 1) {
      new Application(Integer.parseInt(args[0])).launch();
    } else {
      new Application(Integer.parseInt(args[0]),
              new InetSocketAddress(args[1], Integer.parseInt(args[2]))).launch();
    }
  }

}
