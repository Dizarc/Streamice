package com.github.dizarc.streaming;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import javafx.application.Platform;

import java.io.*;

import java.net.Socket;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
/*
    TODO:
        1. The application now works only with avi(issues with ffmpeg) make the other formats work aswell.
        2. Create a reset button which resets the client to right after the speed test so he can choose other videos.
        3. Make it so when the video finishes the client does 2.
        4. Create a variable for the ffmpeg streaming port.
 */
public class ClientLogic {

    static final int SERVER_PORT = 8334;
    static final String SERVER_HOST = "localhost";
    private final static String SPEED_TEST_SERVER = "ftp://speedtest:speedtest@ftp.otenet.gr/test100Mb.db";

    static final String FFMPEG_DIR = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\ffmpeg\\bin";

    private static final Logger LOGGER = Logger.getLogger(ClientLogic.class.getName());
    private static final String LOG_FILE_NAME = "Client.log";

    public ClientLogic(){
        try{
            FileHandler fileHandler = new FileHandler(LOG_FILE_NAME);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.severe("Failed to setup logging: "+ e.getMessage());
        }
    }

    /*
        Tests speed and calls the connection handler when completed.
     */
    public static void testSpeed(ClientController controller){

        LOGGER.info("Starting speed test...");

        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport speedTestReport) {

                LOGGER.info("Finished speed test...");

                //speedTestValue is in Mbps here
                double speedtestValue = speedTestReport.getTransferRateBit().divide(BigDecimal.valueOf(1000000), RoundingMode.CEILING).doubleValue();

                Platform.runLater(() -> controller.setConnectionTestLabel(String.valueOf(speedtestValue), "-fx-text-fill: white"));
                Platform.runLater(() -> controller.setTestProgressBar((float) 1.0));

                connectionHandler(controller, speedtestValue);
            }

            @Override
            public void onProgress(float v, SpeedTestReport speedTestReport) {
                controller.setTestProgressBar((float) (v / 47.5));
            }

            @Override
            public void onError(SpeedTestError speedTestError, String s) {
                LOGGER.severe("Speedtest error: "+ speedTestError +" : " + s);
            }
        });

        speedTestSocket.startFixedDownload(SPEED_TEST_SERVER, 4700);
    }

    /*
        Handles connection and communication with the server.
     */
    public static boolean connectionHandler(ClientController controller, double speedTest){

        LOGGER.info("Starting connection...");

        try {

            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);

            Platform.runLater(() -> controller.setServerConnectLabel("Connection to Server established!"));

            ObjectInputStream objectReader = new ObjectInputStream(socket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            //When user selects a format
            controller.getFormatBox().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) ->  {

                writer.println(newValue);

                controller.setFormatDisable(true);
                controller.setVideoDisable(false);
            });

            //When user selects video
            controller.getVideoList().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) ->  {

                writer.println(newValue);

                controller.setVideoDisable(true);
                controller.setProtocolDisable(false);
            });

            //when user selects a protocol
            controller.getProtocolBox().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {

                writer.println(newValue);

                controller.setProtocolDisable(true);
            });

            String received;
            while (true) {

                writer.println(speedTest);

                //Enable format label + box
                Platform.runLater(() -> controller.setFormatDisable(false));

                ArrayList<String> fileNames = (ArrayList<String>) objectReader.readObject();

                controller.setVideoList(fileNames);

                received = reader.readLine();

                if (received.equalsIgnoreCase("READY")) {

                    String protocol = controller.getProtocolBox().getValue();
                    String fileName = controller.getVideoList().getSelectionModel().getSelectedItem();
                    System.out.println(fileName);
                    if (protocol.equalsIgnoreCase("auto")) {
                        if (fileName.contains("240p"))
                            protocol = "tcp";
                        else if (fileName.contains("360p") || fileName.contains("480p"))
                            protocol = "udp";
                        else if (fileName.contains("720p") || fileName.contains("1080p"))
                            protocol = "rtp";
                    }

                    LOGGER.info("Starting streaming...");

                    String[] ffmpegCommand = {""};

                    if (protocol.equalsIgnoreCase("udp")) {

                        ffmpegCommand = new String[]{
                                FFMPEG_DIR + "\\" + ".\\ffplay",
                                "udp://" + socket.getInetAddress().getHostAddress() + ":1234" //+ socket.getPort()
                        };

                    } else if (protocol.equalsIgnoreCase("tcp")) {

                        ffmpegCommand = new String[]{
                                FFMPEG_DIR + "\\" + ".\\ffplay",
                                "tcp://" + socket.getInetAddress().getHostAddress() + ":1234" //+ socket.getPort()
                        };

                    } else if (protocol.equalsIgnoreCase("rtp")) {

                    }

                    try {
                        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);

                        processBuilder.redirectErrorStream(true);
                        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(LOG_FILE_NAME)));
                        processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(new File(LOG_FILE_NAME)));

                        processBuilder.start();

                    } catch (IOException e) {
                        LOGGER.severe("FFmpeg error: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.severe("Error socket: " + e.getMessage());
            System.exit(21);
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Error getting object through socket: " + e.getMessage());
        }
        return false;
    }

}
