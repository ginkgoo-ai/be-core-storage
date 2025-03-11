package com.ginkgooai.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class VideoMetadataExtractor {

    public static Path generateThumbnailAtPosition(File videoFile, double position) throws IOException, InterruptedException {
        Path thumbnailPath = Files.createTempFile("thumbnail", ".jpg");

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-ss", String.valueOf(position),
                "-i", videoFile.getAbsolutePath(),
                "-vframes", "1",
                "-q:v", "2",
                thumbnailPath.toString()
        );

        Process process = processBuilder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            StringBuilder errorOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroy();
            throw new IOException("FFmpeg执行超时");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            Files.deleteIfExists(thumbnailPath);
            throw new IOException("FFmpeg错误码：" + exitCode);
        }

        return thumbnailPath;
    }


    public static String generateUniqueThumbnailName(String originalFileName) {
        String extension = "jpg";
        String baseFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return String.format("thumbnails-%s_%s.%s",
                baseFileName,
                UUID.randomUUID(),
                extension
        );
    }

    public static double getVideoDuration(File videoFile) throws IOException {
        String cmd = String.format("ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 %s",
                videoFile.getAbsolutePath());
        Process process = Runtime.getRuntime().exec(cmd);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return Double.parseDouble(reader.readLine());
        }
    }

    public static String getVideoResolution(File videoFile) throws IOException {
        String cmd = String.format("ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 %s",
                videoFile.getAbsolutePath());
        Process process = Runtime.getRuntime().exec(cmd);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.readLine().trim();
        }
    }

    public static void deleteTempFile(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("delete temp file failed", e);
            }
        }
    }
}

