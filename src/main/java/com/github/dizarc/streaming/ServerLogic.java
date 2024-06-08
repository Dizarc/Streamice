package com.github.dizarc.streaming;

import javafx.application.Platform;
import javafx.util.Pair;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerLogic {

    static final String VIDEOS_DIR = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\Streaming\\src\\main\\resources\\Videos";
    static final String FFMPEG_DIR = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\ffmpeg\\bin";

    static final int SERVER_PORT = 8334;
    static final int FFMPEG_PORT = 8445;

    //Since mkv's actual name is matroska we cannot use one string array...
    static final String[] FORMATS = {"avi", "mp4", "matroska"};
    static final String[] FORMAT_END = {"avi", "mp4", "mkv"};


    static final Logger LOGGER = Logger.getLogger(ServerLogic.class.getName());
    private static final String LOG_FILE = "Server.log";

    static final List<Pair<Integer, Integer>> RESOLUTIONS = Arrays.asList(
            new Pair<>(320, 240),
            new Pair<>(640, 360),
            new Pair<>(640, 480),
            new Pair<>(1280, 720),
            new Pair<>(1920, 1080)
    );

    static {
        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.severe("Failed to setup logging: " + e.getMessage());
        }
    }

    static class ResolutionFormat {
        int resolution;
        String format;

        public ResolutionFormat(int resolution, String format) {
            this.resolution = resolution;
            this.format = format;
        }
    }

    /*
        Finds which file has the biggest resolution and its format and calls the function to create the missing files.
     */
    public static void fileCreation(ServerController controller) {

        LOGGER.info("Started file creation");

        try {
            File directory = new File(VIDEOS_DIR);
            File[] files = directory.listFiles();

            Map<String, ResolutionFormat> highestRes = new HashMap<>();

            //set the pattern of name-resolution.format
            String pattern = "^(\\w+)-(\\d+)p\\.(\\w+)$";
            Pattern fileNamePattern = Pattern.compile(pattern);

            //Put the highest resolution and its format for each video into a Map.
            if (files != null) {
                for (File file : files) {

                    Matcher matcher = fileNamePattern.matcher(file.getName());

                    if (matcher.matches()) {
                        String name = matcher.group(1);
                        int resolution = Integer.parseInt(matcher.group(2));
                        String format = matcher.group(3);

                        //if the current resolution and format do not exist in the highest resolution map for that name
                        // or its resolution is bigger than the one on the highest resolution add it to the highest resolution map
                        ResolutionFormat currentResFormat = highestRes.get(name);
                        if (currentResFormat == null || resolution > currentResFormat.resolution) {
                            highestRes.put(name, new ResolutionFormat(resolution, format));
                        }
                    } else {
                        LOGGER.info("File: " + file.getName() + " is not named correctly!");
                    }
                }
            }

            if (highestRes.isEmpty()) {

                LOGGER.info("No Videos inside directory!");
                Platform.runLater(() -> controller.setVideosLabel("There are no videos in the directory!", "-fx-text-fill: red"));

                return;
            }

            createFiles(controller, highestRes);

        } catch (NullPointerException e) {
            LOGGER.severe("directory error: " + e.getMessage());
            System.exit(11);
        }
    }

    /*
        Creates all the missing files using ffmpeg
     */
    private static void createFiles(ServerController controller, Map<String, ResolutionFormat> highestRes) {

        LOGGER.info("using ffmpeg for file creation");

        try {
            FFmpeg ffmpeg = new FFmpeg(FFMPEG_DIR + "\\ffmpeg.exe");
            FFprobe ffprobe = new FFprobe(FFMPEG_DIR + "\\ffprobe.exe");

            int pos = 0;
            for (Map.Entry<String, ResolutionFormat> entry : highestRes.entrySet()) {
                pos++;

                //Get position of the maximum resolution for each Video
                int resPos = RESOLUTIONS.indexOf(RESOLUTIONS.stream()
                        .filter(pair -> pair.getValue().equals(entry.getValue().resolution))
                        .findFirst().orElse(null));

                //Create all the files with resolutions equal and lower than the max and every other format.
                for (int i = 0; i <= resPos; i++) {
                    for (int j = 0; j < 3; j++) {

                        FFmpegBuilder builder = new FFmpegBuilder()
                                .setInput(VIDEOS_DIR + "\\" +
                                        entry.getKey() + "-" + entry.getValue().resolution + "p." + entry.getValue().format)
                                .overrideOutputFiles(false)
                                .addOutput(VIDEOS_DIR + "\\"
                                        + entry.getKey() + "-" + RESOLUTIONS.get(i).getValue() + "p." + FORMAT_END[j])
                                .setFormat(FORMATS[j])
                                .setVideoResolution(RESOLUTIONS.get(i).getKey(), RESOLUTIONS.get(i).getValue())
                                .addExtraArgs("-loglevel", "error")
                                .done();

                        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
                        executor.createJob(builder).run();

                        Platform.runLater(controller::setList);
                    }
                }


                float percentage = (float) pos / highestRes.size();
                Platform.runLater(() -> controller.setProgressBar(percentage));
            }

            Platform.runLater(() -> controller.setVideosLabel("Created all the missing Videos!", "-fx-text-fill: green"));
            Platform.runLater(() -> controller.setClientLabel("Waiting for client...", "-fx-text-fill: red"));

            LOGGER.info("finished file creation");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
        Handles the connection and communication with the client.
     */
    public static void connectionHandler(ServerController controller) {

        LOGGER.info("Starting connection handler");

        try {

            LOGGER.info("Opening socket");
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            while (true) {
                try {
                    LOGGER.info("Getting socket connection");

                    Socket socket = serverSocket.accept();

                    Platform.runLater(() -> controller.setClientLabel("Client Accepted! \n", "-fx-text-fill: green"));

                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    ObjectOutputStream objectWriter = new ObjectOutputStream(socket.getOutputStream());
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                    double speedtest;
                    String format;
                    String fileName;
                    String protocol;
                    while (true) {

                        LOGGER.info("getting speedtest");
                        Platform.runLater(() -> controller.setClientLabel("Waiting for speedtest result...", "-fx-text-fill: green"));
                        speedtest = Double.parseDouble(reader.readLine());

                        LOGGER.info("getting format choice");
                        double finalSpeedtest = speedtest;
                        Platform.runLater(() -> controller.setClientLabel("Speedtest result: " + finalSpeedtest + "\n" + "Waiting for format selection...", "-fx-text-fill: green"));
                        format = reader.readLine();

                        LOGGER.info("sending videos");
                        String finalFormat = format;
                        Platform.runLater(() -> controller.setClientLabel("Client chose format: " + finalFormat + "\n" + "Sending available files " + "\n" + "Waiting for video choice...", "-fx-text-fill: green"));

                        ArrayList<String> availableFiles = getFilenamesForClient(controller, speedtest, format);
                        objectWriter.writeObject(availableFiles);

                        //because .mkv has its name as matroska
                        if (format.equalsIgnoreCase("mkv"))
                            format = "matroska";

                        LOGGER.info("getting video choice");
                        fileName = reader.readLine();

                        LOGGER.info("getting protocol choice");
                        String finalFileName = fileName;
                        Platform.runLater(() -> controller.setClientLabel("Client chose file: " + finalFileName + "\n" + "waiting for protocol selection...", "-fx-text-fill: green"));
                        protocol = reader.readLine();

                        if (protocol.equalsIgnoreCase("auto")) {
                            if (fileName.contains("240p"))
                                protocol = "tcp";
                            else if (fileName.contains("360p") || fileName.contains("480p"))
                                protocol = "udp";
                            else if (fileName.contains("720p") || fileName.contains("1080p"))
                                protocol = "rtp";
                        }

                        String finalProtocol = protocol;
                        Platform.runLater(() -> controller.setClientLabel("Chosen protocol: " + finalProtocol + "\n" + "Starting streaming...", "-fx-text-fill: green"));

                        LOGGER.info("Starting streaming preparations");

                        String[] ffmpegCommand = {""};

                        if (protocol.equalsIgnoreCase("udp")) {

                            ffmpegCommand = new String[]{
                                    FFMPEG_DIR + ".\\ffmpeg",
                                    "-re", //read input in native fps
                                    "-i", fileName,
                                    "-movflags", "frag_keyframe", //fragmentation
                                    "-f", "mpegts", //transport using MPEG-TS
                                    "udp://" + socket.getInetAddress().getHostAddress() + ":" + FFMPEG_PORT,
                            };
                        } else if (protocol.equalsIgnoreCase("tcp")) {

                            ffmpegCommand = new String[]{
                                    FFMPEG_DIR + ".\\ffmpeg",
                                    "-re",
                                    "-i", fileName,
                                    "-movflags", "frag_keyframe",
                                    "-f", format,
                                    "tcp://" + socket.getInetAddress().getHostAddress() + ":" + FFMPEG_PORT + "?listen",
                            };
                        } else if (protocol.equalsIgnoreCase("rtp")) {

                            ffmpegCommand = new String[]{
                                    FFMPEG_DIR + ".\\ffmpeg",
                                    "-re", "-i", fileName,
                                    "-movflags", "frag_keyframe",
                                    "-c:v", "copy", "-c:a", "copy",
                                    "-f", "rtp_mpegts",
                                    "\"rtp://" + socket.getInetAddress().getHostAddress() + ":" + FFMPEG_PORT + "\""
                            };
                        }

                        writer.println("READY");
                        String START = reader.readLine();

                        try {
                            LOGGER.info("Creating processBuilder");
                            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);

                            processBuilder.redirectErrorStream(true);
                            processBuilder.redirectOutput(ProcessBuilder.Redirect.to(new File("FfmpegOUT" + LOG_FILE)));
                            processBuilder.redirectError(ProcessBuilder.Redirect.to(new File("FfmpegERROR" + LOG_FILE)));

                            processBuilder.directory(new File(VIDEOS_DIR));

                            Process process = processBuilder.start();

                            String FINISHED = reader.readLine();

                            process.destroy();

                            LOGGER.info("Streaming done");

                        } catch (IOException e) {
                            LOGGER.severe("FFmpeg error: " + e.getMessage());
                        }
                    }

                } catch (IOException e) {
                    LOGGER.info("Client exited: " + e.getMessage());
                    Platform.runLater(() -> controller.setClientLabel("Client Exited... \n" + "Waiting for new client...", "-fx-text-fill: red"));
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Error creating server socket: " + e.getMessage());
            System.exit(22);
        }
    }

    /*
        Get filenames depending on clients speedtest and format.
     */
    public static ArrayList<String> getFilenamesForClient(ServerController controller, double speedtest, String format) {

        int resolutions = 0;

        // speedtest in Mbps
        if (speedtest >= 3)
            resolutions = 1080;
        else if (speedtest >= 1.5)
            resolutions = 720;
        else if (speedtest >= 0.5)
            resolutions = 480;
        else if (speedtest >= 0.4)
            resolutions = 360;
        else if (speedtest >= 0.3)
            resolutions = 240;

        String[] list = controller.getList();
        ArrayList<String> finalList = new ArrayList<>();

        String pattern = "^(\\w+)-(\\d+)p\\." + format + "$";
        Pattern fileNamePattern = Pattern.compile(pattern);

        for (String filename : list) {

            Matcher matcher = fileNamePattern.matcher(filename);

            if (matcher.matches()) {
                int fileResolution = Integer.parseInt(matcher.group(2));

                if (fileResolution <= resolutions)
                    finalList.add(filename);
            }
        }
        return finalList;
    }
}
