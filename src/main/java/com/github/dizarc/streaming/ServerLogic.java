package com.github.dizarc.streaming;

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
    1. Make multithreading work.
    2. For some reason 1080p mkv does not work?
    3. Find a way to track progress so you can Check it and re-initialize the listview.
    4. Make the progress bar work.
    5. Application does not stop when x is clicked(Might be an issue with multithreading)

 */
public class ServerLogic {

    public void createFiles(){
        try {
            String videosDir = "C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\Streaming\\src\\main\\resources\\Videos";
            File directory = new File(videosDir);
            File[] files = directory.listFiles();

            Map<String, ResolutionFormat> highestRes = new HashMap<>();

            if (files != null) {
                for (File file : files) {
                    String filename = file.getName();

                    String[] parts = filename.split("-");

                    if (parts.length == 2) {
                        String name = parts[0];
                        String[] resolutionFormat = parts[1].split("\\.");

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

            FFmpeg ffmpeg = new FFmpeg("C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\ffmpeg\\bin\\ffmpeg.exe");
            FFprobe ffprobe = new FFprobe("C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\ffmpeg\\bin\\ffprobe.exe");

            String[] formats = {"avi", "mp4", "matroska"};

            List<Pair<Integer, Integer>> resolutions = Arrays.asList(
                    new Pair<>(320, 240),
                    new Pair<>(640, 360),
                    new Pair<>(640, 480),
                    new Pair<>(1280, 720),
                    new Pair<>(1920, 1080)
            );


                    for (Map.Entry<String, ResolutionFormat> entry : highestRes.entrySet()) {
                        //Get position of the maximum resolution for each Video
                        int resPos = resolutions.indexOf(resolutions.stream()
                                .filter(pair -> pair.getValue().equals(entry.getValue().resolution))
                                .findFirst().orElse(null));

                        //Create all the files with resolutions equal and lower than the max and every other format.
                        for (int i = 0; i <= resPos; i++) {
                            System.out.println(resolutions.get(i).getKey() + " " + resolutions.get(i).getValue());
                            FFmpegBuilder builder = new FFmpegBuilder()

                                    .addInput(videosDir + "\\" + entry.getKey() + "-" + entry.getValue().resolution + "." + entry.getValue().format)
                                    .overrideOutputFiles(false)
                                    .addOutput(videosDir + "\\" + entry.getKey() + "-" + resolutions.get(i).getValue() + "." + formats[0])
                                    .setFormat(formats[0])
                                    .setVideoResolution(resolutions.get(i).getKey(), resolutions.get(i).getValue())
                                    .done()

                                    .addInput(videosDir + "\\" + entry.getKey() + "-" + entry.getValue().resolution + "." + entry.getValue().format)
                                    .overrideOutputFiles(false)
                                    .addOutput(videosDir + "\\" + entry.getKey() + "-" + resolutions.get(i).getValue() + "." + formats[1])
                                    .setFormat(formats[1])
                                    .setVideoResolution(resolutions.get(i).getKey(), resolutions.get(i).getValue())
                                    .done()

                                    .addInput(videosDir + "\\" + entry.getKey() + "-" + entry.getValue().resolution + "." + entry.getValue().format)
                                    .overrideOutputFiles(false)
                                    .addOutput(videosDir + "\\" + entry.getKey() + "-" + resolutions.get(i).getValue() + ".mkv") //FFmpeg does not have the format name as mkv but as matroska...
                                    .setFormat(formats[2])
                                    .setVideoResolution(resolutions.get(i).getKey(), resolutions.get(i).getValue())
                                    .done();

                            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

                            executor.createJob(builder).run();
                        }
                    }
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

    public boolean openConnection(){
        return false;
    }

}
