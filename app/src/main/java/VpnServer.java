import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class VpnServer {

  private static final int serverPort = 12346;

  public static void main(String[] args) {
    try {
      Socket out = new Socket();

      DatagramSocket server = new DatagramSocket(serverPort);
      byte[] buf = new byte[1500];
      for(;;) {
        System.out.println("---------vpn server is ready");
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        server.receive(p);
        System.out.println("-----a client is coming len");
        server.send(p);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
