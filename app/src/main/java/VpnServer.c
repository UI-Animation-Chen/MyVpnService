#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <errno.h>

#define CZF_DEBUG 1

#ifdef CZF_DEBUG
  #define D_LOG printf
#else
  #define D_LOG(...)
#endif

static const char udp_data[] = {
  143, 54,  // origin port
  48, 57,   // dest port
  0, 9,     // udp data len
  0, 0,     // 16位sheck sum， 设置为0表示不检查校验和
  77        // data
};

static const char ip_hdr[] = {
  0x45,     // version 4, ip header len 5 * 4
  0,        // service type
  0, 29,    // ip data len
  248, 56,  // ip标识
  64, 0,    // 3位标志， 15位偏移
  64,       // ttl
  17,       // up service proto
  175, 207,         // ip header check sum
  192, 168, 8, 234, // origin ip
  192, 168, 8, 146, // dest ip
};

static const short port = 12346;

// only handle one client at a time.
static int get_tunnel() {
	// 3 params: domain, type, protocol
	int udp_sockfd = socket(AF_INET, SOCK_DGRAM, 0);
	if (udp_sockfd == -1) {
		printf("socket SOCK_DGRAM err: %d, %s\n", errno, strerror(errno));
		return -1;
	}
	// bind port 12346
	struct sockaddr_in addr;	
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(port);
	while (bind(udp_sockfd, (struct sockaddr *)&addr, sizeof(addr))) {
		if (errno != EADDRINUSE) {
			printf("socket SOCK_DGRAM bind err: %d, %s\n", errno, strerror(errno));
			close(udp_sockfd);
			return -1;
		} else {
			D_LOG("port in use, retry\n");
		}
		usleep(100 * 1000);
	}
	// waiting for a client
	char buf[4];
	socklen_t addrlen;
	do {
    addrlen = sizeof(addr);
    int n = recvfrom(udp_sockfd, buf, sizeof(buf), 0,
                     (struct sockaddr *)&addr, &addrlen);
    if (n <= 0) {
      printf("socket SOCK_DGRAM recvfrom err: %d, %s\n", errno, strerror(errno));
      return -1;
    }
	} while (!(buf[0] == 0 && buf[1] == 0 && buf[2] == 0 && buf[3] == 0));

	// connect the client and then only receive packets from the one.
  int res = connect(udp_sockfd, (struct sockaddr *)&addr, addrlen);
  if (res == -1) {
    printf("socket SOCK_DGRAM connect err: %d, %s\n", errno, strerror(errno));
    return -1;
  }

  // send back several times in case of packet loss.
  for (int i = 0; i < 3; i++) {
    send(udp_sockfd, buf, sizeof(buf), MSG_NOSIGNAL);
  }

	return udp_sockfd;
}

static int get_raw_sock() {
  // recv tcp and udp packet
	int raw_sockfd = socket(AF_INET, SOCK_RAW, IPPROTO_UDP); // 只能写一个。写多个收不到ip包，但是可以发送。
	if (raw_sockfd == -1) {
		printf("socket SOCK_RAW err: %d, %s\n", errno, strerror(errno));
		return -1;
	}
	const int on = 1;
	if (setsockopt(raw_sockfd, IPPROTO_IP, IP_HDRINCL, &on, sizeof(on)) < 0) {
		close(raw_sockfd);
		printf("setsockopt SOCK_RAW errno: %d, %s\n", errno, strerror(errno));
		return -1;
	}
	D_LOG("raw socket created\n");
	return raw_sockfd;
}

void send_udp_data(int raw_sockfd) {
  char buf[sizeof(ip_hdr) + sizeof(udp_data)];
  memcpy(buf, ip_hdr, sizeof(ip_hdr));
  memcpy(buf + sizeof(ip_hdr), udp_data, sizeof(udp_data));

  // ip标识，设为0，让内核再设置。
  buf[4] = buf[5] = 0;

  // 修改ip包的源地址为0，让内核设置为本机地址。
  buf[12] = buf[13] = buf[14] = buf[15] = 0;

  // 准备目的ip地址
  char destip[16];
  memset(destip, 0, sizeof(destip));
  sprintf(destip, "%d.%d.%d.%d",
          (unsigned char)buf[16], (unsigned char)buf[17],
          (unsigned char)buf[18], (unsigned char)buf[19]);
  D_LOG("dest ip: %s\n", destip);

  struct sockaddr_in addrto;
  memset(&addrto, 0, sizeof(addrto));
  addrto.sin_family = AF_INET;
  int res = inet_aton(destip, &addrto.sin_addr);
  if (res == 0) {
    printf("inet_aton err, ipstr is invalid");
    close(raw_sockfd);
    exit(-1);
  }
  memcpy(buf + 16, &addrto.sin_addr.s_addr, sizeof(addrto.sin_addr.s_addr));

  int len = sendto(raw_sockfd, buf, sizeof(buf), 0,
                   (struct sockaddr *)&addrto, sizeof(addrto));
  if (len == -1) {
    D_LOG("raw sockid sendto err: %d, %s\n", errno, strerror(errno));
  }
  D_LOG("raw sendto success, len: %d\n", len);

  close(raw_sockfd);
}

void recv_from_raw(int raw_sockfd) {
  char buf[1500];
  while (1) {
    memset(buf, 0, sizeof(buf));
    int n = recv(raw_sockfd, buf, sizeof(buf), 0);
    if (n == -1) {
      D_LOG("raw sockfd recv err: %d, %s\n", errno, strerror(errno));
      break;
    }
    D_LOG("raw sock recv a packet, len: %d\n", n);
    if (((unsigned char)buf[15]) != 146) {
      D_LOG("other packet coming, len: %d\n", n);
      continue;
    }
    for (int i = 0; i < n; i++) {
      D_LOG("buf[%d]: %d\n", i, (unsigned char)buf[i]);
    }
  }
}

int main(int argc, char *argv[]) {
	int raw_sockfd = get_raw_sock();
	if (raw_sockfd == -1) {
	  exit(-1);
	}

  send_udp_data(raw_sockfd);
  //recv_from_raw(raw_sockfd);

	/*int udp_sockfd;
	while ((udp_sockfd = get_tunnel()) != -1) { // connected a client
	  D_LOG("here comes a new tunnel\n");
    char buf[1500];

	  while (1) {
	    memset(buf, 0, sizeof(buf));

      int n = recv(udp_sockfd, buf, sizeof(buf), 0);
      if (n <= 0) {
        printf("udp recv err: %d, %s\n", errno, strerror(errno));
        exit(-1);
      }

      // ip标识，设为0，让内核再设置。
      buf[4] = buf[5] = 0;

      // 原来的源地址
      D_LOG("ip before: %d.%d.%d.%d\n",
            (unsigned char)buf[12], (unsigned char)buf[13],
            (unsigned char)buf[14], (unsigned char)buf[15]);

      // 修改ip包的源地址为0，让内核设置为本机地址。
      buf[12] = buf[13] = buf[14] = buf[15] = 0;

      // 准备目的ip地址
      buf[16] = 192;
      buf[17] = 168;
      buf[18] = 8;
      buf[19] = 146;
      char destip[16];
      memset(destip, 0, sizeof(destip));
      sprintf(destip, "%d.%d.%d.%d",
              (unsigned char)buf[16], (unsigned char)buf[17],
              (unsigned char)buf[18], (unsigned char)buf[19]);
      D_LOG("dest ip: %s\n", destip);
      struct sockaddr_in addrto;
      memset(&addrto, 0, sizeof(addrto));
      addrto.sin_family = AF_INET;
      addrto.sin_addr.s_addr = inet_addr(destip);
      int len = sendto(raw_sockfd, buf, n, 0,
                       (struct sockaddr *)&addrto, sizeof(addrto));
      if (len == -1) {
        D_LOG("raw sockfd sendto err: %d, %s\n", errno, strerror(errno));
        continue;
      }
      D_LOG("raw sendto success, len: %d\n", len);

      int recvlen = recv(raw_sockfd, buf, len, 0);
      if (recvlen == -1) {
        D_LOG("raw sockfd recv err: %d, %s\n", errno, strerror(errno));
        continue;
      }
      D_LOG("raw recv success, len: %d\n", recvlen);

      int sendlen = send(udp_sockfd, buf, recvlen, MSG_NOSIGNAL);
      if (sendlen == -1) {
        D_LOG("udp sockfd sendto err: %d, %s\n", errno, strerror(errno));
        continue;
      }
      D_LOG("udp sendto success, len: %d\n", sendlen);
    }
	}*/
	return 0;
}

