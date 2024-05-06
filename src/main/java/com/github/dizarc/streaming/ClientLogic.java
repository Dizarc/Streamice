package com.github.dizarc.streaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientLogic {

    static final int SERVER_PORT = 8334;
    static final String SERVER_HOST = "localhost";

    public boolean connectToServer(){
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            //connection established..

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

            //have to check speed and return it to the server
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public void testSpeed(){

    }
}
