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

public class ClientLogic {

    static final String SERVER_HOST = "localhost";
    static final int SERVER_PORT = 8334;

    static final String SPEED_TEST_SERVER = "ftp://speedtest:speedtest@ftp.otenet.gr/test100Mb.db";

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
        Tests speed of the internet connection.
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
                Platform.runLater(() -> controller.setServerLabel("Speed test ERROR", "-fx-text-fill: red"));
            }
        });
        speedTestSocket.startFixedDownload(SPEED_TEST_SERVER, 5000);
    }

    /*
        Handles connection and communication with the server.
     */
    public static void connectionHandler(ClientController controller){

        LOGGER.info("Starting connection handler");

        try {

            LOGGER.info("Connecting to server socket");
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);

            ObjectInputStream objectReader = new ObjectInputStream(socket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            //When user selects a format
            controller.getFormatBox().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) ->  {

                if(newValue != null) {
                    LOGGER.info("Sending format");

                    writer.println(newValue);

                    controller.setServerLabel("Choose a video", "-fx-text-fill: green");

                    controller.setFormatDisable(true);
                    controller.setVideoDisable(false);
                }
            });

            //When user selects video
            controller.getVideoList().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) ->  {

                if(newValue != null) {
                    LOGGER.info("Sending video name");

                    writer.println(newValue);

                    controller.setServerLabel("Choose a protocol", "-fx-text-fill: green");

                    controller.setVideoDisable(true);
                    controller.setProtocolDisable(false);
                }
            });

            //when user selects a protocol
            controller.getProtocolBox().getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {

                if(newValue != null) {
                    LOGGER.info("Sending protocol");

                    writer.println(newValue);

                    controller.setServerLabel("Playing video...", "-fx-text-fill: green");

                    controller.setProtocolDisable(true);
                }
            });

            while (true) {

                LOGGER.info("Sending speed test");
                writer.println(Double.parseDouble(controller.getSpeedtestLabel().getText()));

                Platform.runLater(() -> controller.setServerLabel("Connection to Server established!" + "\n" +"Choose a format", "-fx-text-fill: green"));

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

                    LOGGER.info("Starting streaming preparations");

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
                        LOGGER.severe("FFmpeg process error: " + e.getMessage());
                    }
            }
        } catch (IOException e) {
            LOGGER.severe("Error connecting to the server: " + e.getMessage());
            Platform.runLater(() -> controller.setServerLabel("Connection to the server could not be established!", "-fx-text-fill: red"));
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Error getting object through socket: " + e.getMessage());
        }
    }
}
