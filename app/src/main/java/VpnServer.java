import com.czf.myvpnservice.IPv4Utils;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

public class VpnServer {

  private static final int serverPort = 12346;

  public static void main(String[] args) {
    try {
      DatagramSocket vpnServer = new DatagramSocket(serverPort);
      vpnServer.setSoTimeout(10 * 1000);
      DatagramSocket outSocket = new DatagramSocket();
      outSocket.setSoTimeout(10 * 1000);
      System.out.println("----- vpn server is ready");
      byte[] buf = new byte[1500];
      for (;;) {
        // 1，从vpn client接收
        DatagramPacket p;
        try {
          p = new DatagramPacket(buf, buf.length);
          vpnServer.receive(p);

          System.out.println("----- packet: orgin ip: " + p.getAddress().getHostAddress());
          System.out.println("----- packet: orgin port: " + p.getPort());

          System.out.println("----- ip version: " + IPv4Utils.getIPVersion(buf));
          System.out.println("----- ip header Len: " + IPv4Utils.getIPHeaderLen(buf));
          System.out.println("----- ip totalLen: " + IPv4Utils.getIPTotalLen(buf));
          System.out.println("----- orgin ip: " + IPv4Utils.getOriginIpAddress(buf));
          System.out.println("----- dest ip: " + IPv4Utils.getDestIpAddress(buf));
          System.out.println("----- up proto: " + IPv4Utils.getUpProtocol(buf));
        } catch (SocketTimeoutException e) {
          e.printStackTrace();
          continue;
        }

        // 2，处理后转发到实际服务
        p = new DatagramPacket(buf, p.getLength());
        outSocket.send(p);

        // 3，从实际服务接收后，处理
        try {
          p = new DatagramPacket(buf, buf.length);
          outSocket.receive(p);
        } catch (SocketTimeoutException e) {
          e.printStackTrace();
          continue;
        }

        // 4，转发回vpn client
        vpnServer.send(p);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
