import com.czf.myvpnservice.IPv4Utils;

import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class VpnServer {

  private static DatagramSocket server;
  private static final int serverPort = 12346;

  private static DatagramSocket outSocket;

  public static void main(String[] args) {
    try {
      server = new DatagramSocket(serverPort);
      System.out.println("----- vpn server is ready");
      byte[] buf = new byte[1500];
      for (;;) {
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        server.receive(p);

        System.out.println("----- packet: orgin ip: " + p.getAddress().getHostAddress());
        System.out.println("----- packet: orgin port: " + p.getPort());

        System.out.println("----- ip version: " + IPv4Utils.getIPVersion(buf));
        System.out.println("----- ip header Len: " + IPv4Utils.getIPHeaderLen(buf));
        System.out.println("----- ip totalLen: " + IPv4Utils.getIPTotalLen(buf));
        System.out.println("----- orgin ip: " + IPv4Utils.getOriginIpAddress(buf));
        System.out.println("----- dest ip: " + IPv4Utils.getDestIpAddress(buf));
        System.out.println("----- up proto: " + IPv4Utils.getUpProtocol(buf));

        //server.send(p);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void startOutSocket() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          outSocket = new DatagramSocket();
          byte[] buf = new byte[1500];
          for (;;) {
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            outSocket.receive(p);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

}
