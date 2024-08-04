package fr.uge.ugegreed.works;

import fr.uge.ugegreed.Checker;
import fr.uge.ugegreed.records.Response;
import fr.uge.ugegreed.records.Task;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class JarHandler {

  private static final Logger LOGGER = Logger.getLogger(JarHandler.class.getName());

  public static List<Response> handleJar(String url,
                                         String className,
                                         Task.Range range) {
    Objects.requireNonNull(url);
    Objects.requireNonNull(className);
    var listResponse = new ArrayList<Response>();
    var checker = Client.checkerFromHTTP(url, className).orElse(null);
    if (checker == null) { // Not the best way for checking if it's URL or PATH
      // But, it uses Client class instead of saying "is URL then, bla bla bla..."
      checker = Client.checkerFromDisk(Path.of(url), className).orElse(null);
      if (checker == null) {
        for (var i = range.inf(); i < range.sup(); i++) {
          listResponse.add(new Response(i, (byte) 3, ""));
        }
        return List.copyOf(listResponse);
      }
    }
    for (var i = range.inf(); i <= range.sup(); i++) {
      listResponse.add(process(checker, i));
    }
    return List.copyOf(listResponse);
  }

  private static Response process(Checker checker, long value) {
    var message = "";
    try {
      message = checker.check(value);
    } catch (InterruptedException e) {
      LOGGER.warning("interrupted while computing");
      return new Response(value, (byte) 1, message);
    }
    return new Response(value, (byte) 0, message);
  }

  public static void main(String[] args) throws InterruptedException {
    var range = new Task.Range(10, 20);
    var result = handleJar("http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar",
            "fr.uge.factors.Factorizer",
            range);
    System.out.println(result);
    result = handleJar("/Users/carayol/bb/progreseau/jars/Collatz.jar",
            "fr.uge.collatz.Collatz",
            range);
    System.out.println(result);
    result = handleJar("http://www-igm.univ-mlv.fr/~carayol/SlowChecker.jar",
            "fr.uge.slow.SlowChecker",
            range);
    System.out.println(result);
  }
}
