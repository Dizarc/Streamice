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
        1. For some reason the threads in clientController dont work. Find a way to make them work and also do the same coding in ServerController.
        (I THINK IT WORKS BUT I HAVE NOT IMPLEMENTED IT IN THE SERVER SIDE)
        2. Create a reset button which resets the client to right after the speed test so he can choose other videos.
        3. Make it so when the video finishes the client does 2.
        4. RTP playing issues....
 */
public class ClientLogic {

    static final int SERVER_PORT = 8334;
    static final String SERVER_HOST = "localhost";
    private final static String SPEED_TEST_SERVER = "ftp://speedtest:speedtest@ftp.otenet.gr/test100Mb.db";

    static final String FFMPEG_DIR = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\ffmpeg\\bin";
    static final int FFMPEG_PORT = 8445;

    private static final Logger LOGGER = Logger.getLogger(ClientLogic.class.getName());
    private static final String LOG_FILE = "Client.log";

    static {
        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE);
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

        LOGGER.info("Starting speed test");

        final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport speedTestReport) {

                LOGGER.info("Finished speed test");

                //speedTestValue is in Mbps here
                double speedtestValue = speedTestReport.getTransferRateBit().divide(BigDecimal.valueOf(1000000), RoundingMode.CEILING).doubleValue();

                Platform.runLater(() -> controller.setConnectionTestLabel(String.valueOf(speedtestValue), "-fx-text-fill: white"));
                Platform.runLater(() -> controller.setTestProgressBar((float) 1.0));
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
        speedTestSocket.startFixedDownload(SPEED_TEST_SERVER, 1000);
    }

    /*
        Handles connection and communication with the server.
     */
    public static void connectionHandler(ClientController controller){

        LOGGER.info("Starting connection handler");

        try {

            LOGGER.info("Connecting to server socket");
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);

            Platform.runLater(() -> controller.setServerConnectLabel("Connection to Server established!"));

            ObjectInputStream objectReader = new ObjectInputStream(socket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            //When user selects a format
            controller.getFormatBox().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) ->  {

                if(newValue != null) {
                    LOGGER.info("Sending format");

                    writer.println(newValue);

                    controller.setFormatDisable(true);
                    controller.setVideoDisable(false);
                }
            });

            //When user selects video
            controller.getVideoList().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) ->  {

                if(newValue != null) {
                    LOGGER.info("Sending video name");

                    writer.println(newValue);

                    controller.setVideoDisable(true);
                    controller.setProtocolDisable(false);
                }
            });

            //when user selects a protocol
            controller.getProtocolBox().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {

                if(newValue != null) {
                    LOGGER.info("Sending protocol");

                    writer.println(newValue);

                    controller.setProtocolDisable(true);
                }
            });

            while (true) {

                LOGGER.info("Sending speed test");
                writer.println(Double.parseDouble(controller.getSpeedtestLabel().getText()));

                Platform.runLater(() -> controller.setFormatDisable(false));

                LOGGER.info("Reading files");
                ArrayList<String> fileNames = (ArrayList<String>) objectReader.readObject();
                Platform.runLater(() -> controller.setVideoList(fileNames));

                //Wait until client has sent everything to the server
                LOGGER.info("Reading ready message");
                String READY = reader.readLine();

                    String protocol = controller.getProtocolBox().getValue();
                    String fileName = controller.getVideoList().getSelectionModel().getSelectedItem();
                    if (protocol.equalsIgnoreCase("auto")) {
                        if (fileName.contains("240p"))
                            protocol = "tcp";
                        else if (fileName.contains("360p") || fileName.contains("480p"))
                            protocol = "udp";
                        else if (fileName.contains("720p") || fileName.contains("1080p"))
                            protocol = "rtp";
                    }

                    LOGGER.info("Starting streaming");

                    String[] ffmpegCommand = {""};

                    if (protocol.equalsIgnoreCase("udp")) {
                        ffmpegCommand = new String[]{
                                FFMPEG_DIR + "\\" + ".\\ffplay",
                                "udp://" + socket.getInetAddress().getHostAddress() + ":" + FFMPEG_PORT,
                        };

                    } else if (protocol.equalsIgnoreCase("tcp")) {
                        ffmpegCommand = new String[]{
                                FFMPEG_DIR + "\\" + ".\\ffplay",
                                "tcp://" + socket.getInetAddress().getHostAddress() + ":" + FFMPEG_PORT,
                        };

                    } else if (protocol.equalsIgnoreCase("rtp")) {
                        ffmpegCommand = new String[]{
                                FFMPEG_DIR + "\\" + ".\\ffplay",
                                "-protocol_whitelist", "file,rtp,udp",
                                "-i", "\"rtp://" + socket.getInetAddress().getHostAddress() + ":" + FFMPEG_PORT
                        };
                    }

                    try {

                        LOGGER.info("Creating processBuilder");
                        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);

                        processBuilder.redirectErrorStream(true);
                        processBuilder.redirectOutput(ProcessBuilder.Redirect.to(new File("FfmpegOUT" + LOG_FILE)));
                        processBuilder.redirectError(ProcessBuilder.Redirect.to(new File("FfmpegERROR" + LOG_FILE)));

                        Process process = processBuilder.start();

                        writer.println("START");

                        int exitCode = process.waitFor();

                        LOGGER.info("FFmpeg exited with code: " + exitCode);

                        writer.println("FINISHED");

                        Platform.runLater(controller::reset);
                    } catch (IOException e) {
                        LOGGER.severe("FFmpeg error: " + e.getMessage());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
            }
        } catch (IOException e) {
            LOGGER.severe("Error socket: " + e.getMessage());
            System.exit(21);
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Error getting object through socket: " + e.getMessage());
        }
    }
}
