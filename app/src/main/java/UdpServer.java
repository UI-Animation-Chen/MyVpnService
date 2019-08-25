import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class UdpServer {

  private static final int serverPort = 12345;

  public static void main(String[] args) {
    try {
      DatagramSocket udpServer = new DatagramSocket(serverPort);
      System.out.println("----- udp server is ready");
      byte[] buf = new byte[1500];
      for (;;) {
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        udpServer.receive(p);
        System.out.println("reveived a packet");

        buf[0]++;
        p = new DatagramPacket(buf, p.getLength(), p.getAddress(), p.getPort());
        udpServer.send(p);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
