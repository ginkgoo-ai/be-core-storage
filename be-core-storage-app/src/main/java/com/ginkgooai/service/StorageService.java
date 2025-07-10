package com.ginkgooai.service;

import com.ginkgooai.domain.CloudFile;
import com.ginkgooai.dto.CloudFileResponse;
import com.ginkgooai.dto.CloudFilesResponse;
import com.ginkgooai.dto.PDFHighlightRequest;
import com.ginkgooai.dto.SaveSeparatelyRequest;
import com.ginkgooai.model.request.PresignedUrlRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author: david
 * @date: 20:49 2025/2/9
 */


public interface StorageService {

    Long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    Set<String> ALLOWED_FILE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");

    static String generateUniqueFileName(String originalFileName) {
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex >= 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }

        return UUID.randomUUID().toString().replace("-", "") + fileExtension;
    }


    CloudFileResponse uploadFile(MultipartFile file);

    URL generatePresignedUrl(String fileName);

    String generateSignedUrl(String fileId) throws FileNotFoundException, URISyntaxException;

    URL generatePresignedUrlByOrigninalUrl(PresignedUrlRequest request);

	/**
	 * Generate presigned URL by file ID
	 * @param fileId The file ID to generate presigned URL for
	 * @return Presigned URL for the file
	 * @throws FileNotFoundException if the file is not found
	 */
	URL generatePresignedUrlByFileId(String fileId) throws FileNotFoundException;

    void downloadFile(String originUrl, OutputStream out) throws FileNotFoundException;

    CloudFile uploadThumbnailToStorage(Path thumbnailFile, String thumbnailName);

    void downloadBlob(HttpServletRequest request, HttpServletResponse response);

    String getPrivateUrl(String fileId) throws FileNotFoundException;

    CloudFilesResponse uploadFiles(MultipartFile[] files);

    List<CloudFileResponse> getFileDetails(List<String> fileId);

    CloudFileResponse saveSeparately(SaveSeparatelyRequest saveSeparatelyRequest);

    /**
     * Process PDF highlighting and return the highlighted PDF as a blob stream
     * 
     * @param request PDF highlight request containing fileId and highlightData
     * @param response HTTP response to write the highlighted PDF blob to
     * @throws FileNotFoundException if the PDF file is not found
     * @throws IOException if there's an error processing the PDF
     */
    void processPDFHighlight(PDFHighlightRequest request, HttpServletResponse response) throws FileNotFoundException, IOException;
}
