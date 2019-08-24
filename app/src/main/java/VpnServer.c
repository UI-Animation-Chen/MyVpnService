#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <errno.h>

static const short port = 12346;

static int get_tunnel() {
	// 3 params: domain, type, protocol
	int udp_sockid = socket(AF_INET, SOCK_DGRAM, 0);
	if (udp_sockid == -1) {
		printf("socket SOCK_DGRAM err\n");
		return -1;
	}
	// bind port 12346
	struct sockaddr_in addr;	
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(port);
	while (bind(udp_sockid, (struct sockaddr *)&addr, sizeof(addr))) {
		if (errno != EADDRINUSE) {
			printf("socket SOCK_DGRAM bind err\n");
			close(udp_sockid);
			return -1;
		} else {
			printf("port in use, retry\n");
		}
		usleep(100 * 1000);
	}
	
	return udp_sockid;
}

static int get_raw_sock() {
  // recv tcp and udp packet
	int raw_sockid = socket(AF_INET, SOCK_RAW, IPPROTO_UDP | IPPROTO_TCP);
	if (raw_sockid == -1) {
		printf("socket SOCK_RAW err: %d, %s\n", errno, strerror(errno));
		return -1;
	}
	const int on = 1;
	if (setsockopt(raw_sockid, IPPROTO_IP, IP_HDRINCL, &on, sizeof(on)) < 0) {
		close(raw_sockid);
		printf("setsockopt SOCK_RAW errno: %d, %s\n", errno, strerror(errno));
		return -1;
	}
	
	return raw_sockid;
}

int main(int argc, char *argv[]) {
	int udp_sockid = get_tunnel();
	if (udp_sockid == -1) {
		exit(-1);
	}

	int raw_sockid = get_raw_sock();
	if (raw_sockid == -1) {
	  exit(-1);
	}

	char buf[1500];
	struct sockaddr_in addr;
	memset(&addr, 0, sizeof(addr));
	socklen_t addrlen;
	while (1) {
	  memset(buf, 0, sizeof(buf));
		addrlen = sizeof(addr);
		int n = recvfrom(udp_sockid, buf, sizeof(buf), 0,
						         (struct sockaddr *)&addr, &addrlen);
		if (n <= 0) {
			printf("udp recv err: %d, %s\n", errno, strerror(errno));
			exit(-1);
		}

    if (buf[0] == 0 && buf[1] == 0 && buf[2] == 0 && buf[3] == 0) {
      char *ipbuf = buf + 4;

      ipbuf[4] = ipbuf[5] = 0; // ip标识，设为0，让内核再设置。

      // 记录原来的源地址
      char beforeip[4];
      memset(beforeip, 0, sizeof(beforeip));
      beforeip[0] = ipbuf[12];
      beforeip[1] = ipbuf[13];
      beforeip[2] = ipbuf[14];
      beforeip[3] = ipbuf[15];
      printf("ip before: %d.%d.%d.%d\n",
             (unsigned char)ipbuf[12], (unsigned char)ipbuf[13],
             (unsigned char)ipbuf[14], (unsigned char)ipbuf[15]);

      // 修改ip包的源地址为本机地址
      ipbuf[12] = 192;
      ipbuf[13] = 168;
      ipbuf[14] = 4;
      ipbuf[15] = 141;
      printf("ip after: %d.%d.%d.%d\n",
             (unsigned char)ipbuf[12], (unsigned char)ipbuf[13],
             (unsigned char)ipbuf[14], (unsigned char)ipbuf[15]);

      char destip[16];
      memset(destip, 0, sizeof(destip));
      sprintf(destip, "%d.%d.%d.%d",
              (unsigned char)ipbuf[16], (unsigned char)ipbuf[17],
              (unsigned char)ipbuf[18], (unsigned char)ipbuf[19]);
      printf("dest ip: %s\n", destip);
      struct sockaddr_in addrto;
      memset(&addrto, 0, sizeof(addrto));
      addrto.sin_family = AF_INET;
      addrto.sin_addr.s_addr = inet_addr(destip);
      int len = sendto(raw_sockid, ipbuf, n - 4, 0,
                       (struct sockaddr *)&addrto, sizeof(addrto));
      if (len == -1) {
        printf("raw sockid sendto err: %d, %s\n", errno, strerror(errno));
        continue;
      }
      printf("raw sendto success, len: %d\n", len);

      int recvlen = recv(raw_sockid, ipbuf, len, 0);
      if (recvlen == -1) {
        printf("raw sockid recv err: %d, %s\n", errno, strerror(errno));
        continue;
      }
      printf("raw recv success, len: %d\n", recvlen);

      ipbuf[16] = beforeip[0];
      ipbuf[17] = beforeip[1];
      ipbuf[18] = beforeip[2];
      ipbuf[19] = beforeip[3];
      int sendlen = sendto(udp_sockid, ipbuf, recvlen, 0,
                           (struct sockaddr *)&addr, addrlen);
      if (sendlen == -1) {
        printf("udp sockid sendto err: %d, %s\n", errno, strerror(errno));
        continue;
      }
      printf("udp sendto success, len: %d\n", sendlen);

    } else {
      printf("not match packet\n");
    }
	}

}

