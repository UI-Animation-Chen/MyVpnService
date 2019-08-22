package com.czf.myvpnservice;

public class IPv4Utils {

  public static int getIPVersion(byte[] ipBuf) {
    if (ipBuf == null || ipBuf.length < 1) return 0;
    return (ipBuf[0] & 0xf0) >> 4;
  }

  public static int getIPHeaderLen(byte[] ipBuf) {
    if (ipBuf == null || ipBuf.length < 1) return 0;
    return (ipBuf[0] & 0x0f) * 4;
  }

  public static int getIPTotalLen(byte[] ipBuf) {
    if (ipBuf == null || ipBuf.length < 4) return 0;
    return ((ipBuf[2] & 0xff) << 8) + (ipBuf[3] & 0xff);
  }

  public static String getOriginIpAddress(byte[] ipBuf) {
    if (ipBuf == null || ipBuf.length < 20) return null;
    return (ipBuf[12] & 0xff) + "." + (ipBuf[13] & 0xff) + "." + (ipBuf[14] & 0xff) + "." + (ipBuf[15] & 0xff);
  }

  public static String getDestIpAddress(byte[] ipBuf) {
    if (ipBuf == null || ipBuf.length < 20) return null;
    return (ipBuf[16] & 0xff) + "." + (ipBuf[17] & 0xff) + "." + (ipBuf[18] & 0xff) + "." + (ipBuf[19] & 0xff);
  }

  public static String getUpProtocol(byte[] ipBuf) {
    return upProtocolNum2Str(ipBuf[9] & 0xff);
  }

  private static String upProtocolNum2Str(int protocol) {
    switch (protocol) {
      case 1:
        return "ICMP";
      case 2:
        return "IGMP";
      case 4:
        return "IP";
      case 6:
        return "TCP";
      case 8:
        return "EGP";
      case 9:
        return "IGP";
      case 17:
        return "UDP";
      case 41:
        return "IPV6";
      case 50:
        return "ESP";
      case 89:
        return "OSPF";
      default:
        return "NOT IMPL";
    }
  }

}
