package com.github.dizarc.streaming;

import javafx.application.Platform;
import javafx.util.Pair;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;

/*
    TO - DO:
    2. For some reason 1080p mkv does not work?
    3. PLATFORM-> stuff dont work correctly...
 */
public class ServerLogic {

    static final String VIDEOS_DIR = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\Streaming\\src\\main\\resources\\Videos";
    static final String FFMPEG_DIR = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\ffmpeg\\bin";

    List<Pair<Integer, Integer>> resolutions = Arrays.asList(
            new Pair<>(320, 240),
            new Pair<>(640, 360),
            new Pair<>(640, 480),
            new Pair<>(1280, 720),
            new Pair<>(1920, 1080)
    );

    String[] formats = {"avi", "mp4", "matroska"};
    String[] formatEnd = {"avi", "mp4", "mkv"};

    public void createFiles(ServerController controller) {
        try {
            File directory = new File(VIDEOS_DIR);
            File[] files = directory.listFiles();

            Map<String, ResolutionFormat> highestRes = new HashMap<>();

            //Get the highest resolution of each Video and its format and start
            if (files != null) {
                for (File file : files) {
                    String filename = file.getName();

                    String[] parts = filename.split("-");

                    if (parts.length == 2) {
                        String name = parts[0];
                        String[] resolutionFormat = parts[1].split("p\\.");

                        if (resolutionFormat.length == 2) {
                            int resolution = Integer.parseInt(resolutionFormat[0]);
                            String format = resolutionFormat[1];

                            ResolutionFormat currentResFormat = highestRes.get(name);
                            if (currentResFormat == null || resolution > currentResFormat.resolution) {
                                highestRes.put(name, new ResolutionFormat(resolution, format));
                            }
                        }
                    }
                }
            }

            FFmpeg ffmpeg = new FFmpeg(FFMPEG_DIR + "\\ffmpeg.exe");
            FFprobe ffprobe = new FFprobe(FFMPEG_DIR + "\\ffprobe.exe");

            int pos = 0;
            for (Map.Entry<String, ResolutionFormat> entry : highestRes.entrySet()) {

                pos++;
                //Get position of the maximum resolution for each Video
                int resPos = resolutions.indexOf(resolutions.stream()
                        .filter(pair -> pair.getValue().equals(entry.getValue().resolution))
                        .findFirst().orElse(null));


                //threaded does not seem to work for some reason..
                //new Thread(() -> {
                        //Create all the files with resolutions equal and lower than the max and every other format.
                        for (int i = 0; i <= resPos; i++) {
                            for (int j = 0; j < 3; j++) {

                                FFmpegBuilder builder = new FFmpegBuilder()

                                        .setInput(VIDEOS_DIR + "\\" + entry.getKey() + "-" + entry.getValue().resolution + "p." + entry.getValue().format)
                                        .overrideOutputFiles(false)
                                        .addOutput(VIDEOS_DIR + "\\" + entry.getKey() + "-" + resolutions.get(i).getValue() + "p." + formatEnd[j])
                                        .setFormat(formats[j])
                                        .setVideoResolution(resolutions.get(i).getKey(), resolutions.get(i).getValue())
                                        .done();

                                FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
                                executor.createJob(builder).run();

                                Platform.runLater(controller::setList);
                            }
                        }


                //}).start();
                int finalPos = pos;
                Platform.runLater(() -> controller.setProgressBar((double) finalPos / highestRes.size()));
            }
            Platform.runLater(() -> controller.setVideosCreatedLabel("Created all the missing Videos!!"));

        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public boolean openConnection() {
        return false;
    }

}
