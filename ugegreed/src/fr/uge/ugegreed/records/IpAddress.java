package fr.uge.ugegreed.records;

import java.net.InetAddress;
import java.util.Objects;

/**
 * Defines what is an IP address in our protocol.<br>
 * It is defined by the IP type (v4 or v6) and the IP.
 *
 * @author Axel BELIN and Thomas VELU.
 * @param type byte, IP type, 4 or 6.
 * @param address InetAddress, the IP address.
 */
public record IpAddress(byte type, InetAddress address) {

  /**
   * The constructor.<br>
   * Of course, we verify the type of the IP and als, if the address is correct.
   *
   * @param type byte, IP type, 4 or 6.
   * @param address InetAddress, the IP address.
   * @throws IllegalArgumentException If the type is incorrect.
   */
  public IpAddress {
    if (type != 4 && type != 6) {
      throw new IllegalArgumentException("type should be 4 or 6 !");
    }
    Objects.requireNonNull(address, "an IP address cannot be null !");
  }

}
