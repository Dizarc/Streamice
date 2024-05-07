package com.github.dizarc.streaming;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ClientLogic {

    static final int SERVER_PORT = 8334;
    static final String SERVER_HOST = "localhost";
    private final static String SPEED_TEST_SERVER = "ftp://speedtest:speedtest@ftp.otenet.gr/test100Mb.db";

    private double speedtestValue;
    private static final Logger LOGGER = Logger.getLogger(ClientLogic.class.getName());

    public ClientLogic(){
        try{
            FileHandler fileHandler = new FileHandler("Client.log");
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.severe("Failed to setup logging: "+ e.getMessage());
        }
    }

    public boolean connectToServer(ClientController controller){
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);

            controller.setServerConnectLabel("Connection to Server established!");

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            testSpeed(controller);

            String received = "";
            while (true) {
                //System.out.println(speedtestValue +"H");
                if(speedtestValue != 0.0 ) {
                    writer.println("HEY");
                    System.out.println("haa");
                    received = reader.readLine();
                }
            }

            //have to check speed and return it to the server

        } catch (IOException e) {
            LOGGER.severe("Error socket: " + e.getMessage());
            System.exit(21);
        }
        return false;
    }
    public void testSpeed(ClientController controller){

        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport speedTestReport) {
                speedtestValue = speedTestReport.getTransferRateBit().divide(BigDecimal.valueOf(1000000), RoundingMode.CEILING).doubleValue();
                Platform.runLater(() -> controller.setConnectionTestLabel("Connection test: " + speedtestValue +" Mbps", "-fx-text-fill: black"));
                Platform.runLater(() -> controller.setTestProgressBar((float) 1.0));
            }
            @Override
            public void onProgress(float v, SpeedTestReport speedTestReport) {
                controller.setTestProgressBar(v / 49);
            }
            @Override
            public void onError(SpeedTestError speedTestError, String s) {
                LOGGER.severe("Speedtest error: "+ speedTestError +" : " + s);
            }
        });

        speedTestSocket.startFixedDownload(SPEED_TEST_SERVER, 5000);
    }
}
