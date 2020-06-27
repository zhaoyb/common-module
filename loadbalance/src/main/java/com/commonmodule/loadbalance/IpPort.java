package com.commonmodule.loadbalance;

public class IpPort {
    private String ip;
    private int port;
    private int weight;

    public IpPort() {
    }

    public IpPort(String ip, int port, int weight) {
        this.ip = ip;
        this.port = port;
        this.weight = weight;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String toIdentityString() {
        return ip + ":" + port + ":" + weight;
    }

    @Override
    public String toString() {
        return "IpPort{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", weight=" + weight +
                '}';
    }
}
