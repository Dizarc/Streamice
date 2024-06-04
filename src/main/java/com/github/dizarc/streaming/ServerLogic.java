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

/*
    TODO:
    Now that the client can choose from his listview the video he wants
     we need to create a new fxml file as soon as he chooses which will bring out a video player that will play the video.
 */
public class ServerLogic {

    static final String VIDEOS_DIR = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\Streaming\\src\\main\\resources\\Videos";
    static final String FFMPEG_DIR = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\ffmpeg\\bin";

    static final List<Pair<Integer, Integer>> RESOLUTIONS = Arrays.asList(
            new Pair<>(320, 240),
            new Pair<>(640, 360),
            new Pair<>(640, 480),
            new Pair<>(1280, 720),
            new Pair<>(1920, 1080)
    );

    //Since mkv's actual name is matroska we cannot use one string array...
    static final String[] FORMATS = {"avi", "mp4", "matroska"};
    static final String[] FORMAT_END = {"avi", "mp4", "mkv"};

    static class ResolutionFormat {
        int resolution;
        String format;

        public ResolutionFormat(int resolution, String format) {
            this.resolution = resolution;
            this.format = format;
        }
    }

    static final int SERVER_PORT = 8334;

    private static final Logger LOGGER = Logger.getLogger(ServerLogic.class.getName());

    public ServerLogic() {
        try {
            FileHandler fileHandler = new FileHandler("Server.log");
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.severe("Failed to setup logging: " + e.getMessage());
        }
    }

    public static void createFiles(ServerController controller) {
        try {
            File directory = new File(VIDEOS_DIR);
            File[] files = directory.listFiles();

            Map<String, ResolutionFormat> highestRes = new HashMap<>();

            //set the pattern of name-resolution.format
            String pattern = "^(\\w+)-(\\d+)p\\.(\\w+)$";
            Pattern fileNamePattern = Pattern.compile(pattern);

            //Put the highest resolution and its format for each video into a Map.
            if (files != null && files.length > 0) {
                for (File file : files) {

                    Matcher matcher = fileNamePattern.matcher(file.getName());

                    if(matcher.matches()){
                        String name = matcher.group(1);
                        int resolution = Integer.parseInt(matcher.group(2));
                        String format = matcher.group(3);

                        ResolutionFormat currentResFormat = highestRes.get(name);
                        if (currentResFormat == null || resolution > currentResFormat.resolution) {
                            highestRes.put(name, new ResolutionFormat(resolution, format));
                        }
                    } else {
                        LOGGER.info("File: " + file.getName() + " is not named correctly...");
                    }
                }
            } else {
                LOGGER.info("No Videos inside directory...");
                Platform.runLater(() -> controller.setVideosLabel("There are no videos in the directory!!", "-fx-text-fill: red"));
                return;
            }

            FFmpeg ffmpeg = new FFmpeg(FFMPEG_DIR + "\\ffmpeg.exe");
            FFprobe ffprobe = new FFprobe(FFMPEG_DIR + "\\ffprobe.exe");

            int pos = 0;
            for (Map.Entry<String, ResolutionFormat> entry : highestRes.entrySet()) {
                pos++;

                //Get position of the maximum resolution for each Video
                int resPos = RESOLUTIONS.indexOf(RESOLUTIONS.stream()
                        .filter(pair -> pair.getValue().equals(entry.getValue().resolution))
                        .findFirst().orElse(null));

                //does not seem to work with threads for some reason...
                //new Thread(() -> {

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
                                .done();

                        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
                        executor.createJob(builder).run();

                        Platform.runLater(controller::setList);
                    }
                }
                //}).start();

                float percentage = (float) pos / highestRes.size();
                Platform.runLater(() -> controller.setProgressBar(percentage));
            }

            Platform.runLater(() -> controller.setVideosLabel("Created all the missing Videos!!", "-fx-text-fill: green"));
            Platform.runLater(() -> controller.setClientLabel("Waiting for client...", "-fx-text-fill: red"));

            connectionClient(controller);

        } catch (IOException e) {
            LOGGER.severe("FFmpeg error: " + e.getMessage());
            System.exit(11);
        }
    }

    public static void connectionClient(ServerController controller) {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            while (true) {
                try {
                    Socket socket = serverSocket.accept();

                    Platform.runLater(() -> controller.setClientLabel("Client Accepted! \n", "-fx-text-fill: green"));

                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    ObjectOutputStream objectWriter = new ObjectOutputStream(socket.getOutputStream());
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                    double speedtest = 0;
                    String format = "";
                    String fileName = "";
                    String protocol= "";
                    while (true) {

                        speedtest = Double.parseDouble(reader.readLine());

                        format = reader.readLine();

                        Platform.runLater(() -> controller.setClientLabel("Client chose format. \n" + "Sending files and waiting for client...", "-fx-text-fill: green"));

                        ArrayList<String> allFiles = getFiles(controller,speedtest, format);
                        objectWriter.writeObject(allFiles);

                        fileName = reader.readLine();

                        Platform.runLater(() -> controller.setClientLabel("Client chose file. \n" + "waiting for client protocol selection...", "-fx-text-fill: green"));

                        protocol = reader.readLine();
                    }

                } catch (IOException e) {
                    LOGGER.severe("Client exited: " + e.getMessage());
                    Platform.runLater(() -> controller.setClientLabel("Client Exited... \n" + "Waiting for new client...", "-fx-text-fill: red"));
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Error creating server socket: " + e.getMessage());
            System.exit(22);
        }
    }

    //Get files depending on clients speedtest and format
    public static ArrayList<String> getFiles(ServerController controller, double speedtest, String format){

        int resolutions = 0;

        // speedtest in Mbps
        if(speedtest >= 3)
            resolutions = 1080;
        else if(speedtest >= 1.5)
            resolutions = 720;
        else if(speedtest >= 0.5)
            resolutions = 480;
        else if(speedtest >= 0.4)
            resolutions = 360;
        else if(speedtest >= 0.3)
            resolutions = 240;

        String[] list = controller.getList();
        ArrayList<String> finalList = new ArrayList<>();

        String pattern = "^(\\w+)-(\\d+)p\\."+ format+"$";
        Pattern fileNamePattern = Pattern.compile(pattern);

        for (String filename : list) {

            Matcher matcher = fileNamePattern.matcher(filename);

            if(matcher.matches()){
                int fileResolution = Integer.parseInt(matcher.group(2));

                if(fileResolution <= resolutions)
                    finalList.add(filename);
            }
        }
        return finalList;
    }
}
