package com.luckycat.mqtt.common;

/**
 * User: fafu
 * Date: 14-5-30
 * Time: 下午3:33
 * This class is
 */
public class Session {
    public String clientIdentifier;
    public String username;
    public String password;
    public Connection conn;

    public Session(String client,String username,String password,Connection conn){
        this.clientIdentifier = client;
        this.username = username;
        this.password = password;
        this.conn =  conn;
    }
}
