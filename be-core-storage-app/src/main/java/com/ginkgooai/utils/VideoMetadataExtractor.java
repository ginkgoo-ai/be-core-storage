package com.ginkgooai.utils;

import com.ginkgooai.domain.CloudFile;
import com.ginkgooai.domain.VideoMetadata;
import com.ginkgooai.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class VideoMetadataExtractor {

    private StorageService storageService;

    public VideoMetadata extractMetadata(MultipartFile videoFile) throws IOException {
        Path tempFile = Files.createTempFile("video", videoFile.getOriginalFilename());
        videoFile.transferTo(tempFile.toFile());

        try {
            FFprobe ffprobe = new FFprobe("G:\\ffmpeg-7.0.2-full_build\\bin\\ffmpeg.exe");
            FFmpegProbeResult probeResult = ffprobe.probe(tempFile.toString());

            FFmpegStream videoStream = probeResult.getStreams().stream()
                    .filter(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO)
                    .findFirst()
                    .orElseThrow(() -> new IOException("No video stream found"));

            CloudFile cloudFile = generateThumbnail(videoFile);
            return VideoMetadata.builder()
                    .thumbnailFileId(cloudFile.getId())
                    .thumbnailUrl(cloudFile.getVideoThumbnailUrl())
                    .duration(Math.round(probeResult.getFormat().duration * 1000))
                    .resolution(videoStream.width + "x" + videoStream.height)
                    .size(videoFile.getSize())
                    .build();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public CloudFile generateThumbnail(MultipartFile videoFile) throws IOException {
        Path tempVideoFile = null;
        Path tempThumbnailFile = null;

        try {
            tempVideoFile = Files.createTempFile("video", videoFile.getOriginalFilename());
            videoFile.transferTo(tempVideoFile.toFile());

            FFprobe ffprobe = new FFprobe();
            FFmpegProbeResult probeResult = ffprobe.probe(tempVideoFile.toString());
            double duration = probeResult.getFormat().duration;

            tempThumbnailFile = generateThumbnailAtPosition(tempVideoFile, duration / 2);

            String thumbnailName = generateUniqueThumbnailName(Objects.requireNonNull(videoFile.getOriginalFilename()));

            return storageService.uploadThumbnailToStorage(tempThumbnailFile, thumbnailName);

        } catch (Exception e) {
            log.error("Failed to generate thumbnail.", e);
            return null;
        } finally {
            // 6. 清理临时文件
            deleteTempFile(tempVideoFile);
            deleteTempFile(tempThumbnailFile);
        }
    }

    private Path generateThumbnailAtPosition(Path videoPath, double position) throws IOException, InterruptedException {
        Path thumbnailPath = Files.createTempFile("thumbnail", ".jpg");

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", videoPath.toString(),
                "-ss", String.valueOf(position),
                "-vframes", "1",
                "-q:v", "2",
                thumbnailPath.toString()
        );

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("FFmpeg thumbnail generation failed");
        }

        return thumbnailPath;
    }

    private String generateUniqueThumbnailName(String originalFileName) {
        String extension = "jpg";
        String baseFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return String.format("thumbnails/%s_%s.%s",
                baseFileName,
                UUID.randomUUID(),
                extension
        );
    }



    private void deleteTempFile(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("delete temp file failed", e);
            }
        }
    }
}

