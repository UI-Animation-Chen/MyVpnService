import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A server socket listens on a single port. All established client connections on that server
 * are associated with that same listening port on the server side of the connection.
 * An established connection is uniquely identified by the combination of client-side and
 * server-side IP/Port pairs. Multiple connections on the same server can share the same
 * server-side IP/Port pair as long as they are associated with different client-side
 * IP/Port pairs, and the server would be able to handle as many clients as available system
 * resources allow it to.
 */
public class TcpServer {

  private static final int serverPort = 12345;

  public static void main(String[] args) {
    try {
      ServerSocket serverSocket = new ServerSocket(serverPort);
      for(;;) {
        System.out.println("--------- server is ready");
        Socket clientSocket = serverSocket.accept();
        System.out.println("--------- a client is coming");
        startTCPConn(clientSocket);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void startTCPConn(Socket clientSocket) {
    try {
      final String sokectName = clientSocket.toString();
      final BufferedReader br =
          new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      final BufferedWriter bw =
          new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
      new Thread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            try {
              String recv = br.readLine();
              if (recv == null) { // closed.
                return;
              }
              bw.write(sokectName + recv);
              bw.newLine();
              bw.flush();
            } catch(IOException e) {
              e.printStackTrace();
            }
          }
        }
      }).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
