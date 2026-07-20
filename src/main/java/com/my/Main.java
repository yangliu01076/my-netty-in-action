package com.my;

/**
 *
 * @author duoyian
 * @date 2026/7/20
 */
public class Main {
    public static void main(String[] args) {
        try {
            new GatewayServer(8080).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}