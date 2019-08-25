import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class UdpClient {

  private static final String serverIP = "192.168.8.146";
  private static final int serverPort = 12345;

  public static void main(String[] args) {
    try {
      DatagramSocket udpServer = new DatagramSocket();

      byte[] buf = new byte[]{77};
      DatagramPacket p = new DatagramPacket(buf, 1, InetAddress.getByName(serverIP), serverPort);
      udpServer.send(p);
      System.out.println("send a udp packet");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
