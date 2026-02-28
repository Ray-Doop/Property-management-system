package com.example.modules.system.file.controller;

import cn.hutool.core.io.FileUtil;
import com.example.common.Result;
import com.example.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/files")
public class FileController {

    private static final String filePath = System.getProperty("user.dir") + "/files/";
    private static final java.util.Set<String> allowedFolders = new java.util.HashSet<>(
            java.util.Arrays.asList("avatar", "comment", "notice", "post", "repair")
    );

    /**
     * 上传文件
     * 注意：MultipartFile 参数必须加 @RequestParam("file")，且前端上传 key 为 "file"
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Result upload(@RequestParam("file") MultipartFile file, @RequestParam(value = "folder", required = false) String folder, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("400", "未接收到文件");
        }
        String targetFolder = folder == null || folder.isBlank() ? "avatar" : folder.trim();
        if (!allowedFolders.contains(targetFolder)) {
            throw new CustomException("400", "文件分类不合法");
        }
        String originalFilename = file.getOriginalFilename();
        String cleanedName = originalFilename == null ? "" : originalFilename.replace("\\", "/");
        if (cleanedName.contains("/")) {
            cleanedName = cleanedName.substring(cleanedName.lastIndexOf('/') + 1);
        }
        String extension = "";
        if (cleanedName.contains(".")) {
            extension = cleanedName.substring(cleanedName.lastIndexOf('.'));
        }
        if (extension.isEmpty()) {
            String contentType = file.getContentType();
            if ("video/mp4".equalsIgnoreCase(contentType)) extension = ".mp4";
            else if ("video/quicktime".equalsIgnoreCase(contentType)) extension = ".mov";
            else if ("video/webm".equalsIgnoreCase(contentType)) extension = ".webm";
            else if ("video/ogg".equalsIgnoreCase(contentType)) extension = ".ogg";
            else if ("video/x-msvideo".equalsIgnoreCase(contentType)) extension = ".avi";
            else if ("image/jpeg".equalsIgnoreCase(contentType)) extension = ".jpg";
            else if ("image/png".equalsIgnoreCase(contentType)) extension = ".png";
            else if ("image/gif".equalsIgnoreCase(contentType)) extension = ".gif";
            else if ("image/webp".equalsIgnoreCase(contentType)) extension = ".webp";
        }
        if (cleanedName.isEmpty()) {
            cleanedName = "file" + extension;
        } else if (extension.isEmpty()) {
            cleanedName = cleanedName + extension;
        }
        String folderPath = filePath + targetFolder + "/";
        if (!FileUtil.isDirectory(folderPath)) {
            FileUtil.mkdir(folderPath);
        }
        String uniquePart = UUID.randomUUID().toString().replace("-", "");
        String fileName = System.currentTimeMillis() + "_" + uniquePart + "_" + cleanedName;
        String realPath = folderPath + fileName;
        try {
            FileUtil.writeBytes(file.getBytes(), realPath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException("500", "文件上传失败");
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String host = forwardedHost != null && !forwardedHost.isBlank() ? forwardedHost : request.getHeader("Host");
        String scheme = forwardedProto != null && !forwardedProto.isBlank() ? forwardedProto : request.getScheme();
        String baseUrl;
        if (host != null && !host.isBlank()) {
            baseUrl = scheme + "://" + host;
        } else {
            baseUrl = scheme + "://" + request.getServerName() + ":" + request.getServerPort();
        }
        String url = baseUrl + "/files/" + targetFolder + "/" + fileName;
        return Result.success(url);
    }


    /**
     * 下载文件
     */
    @GetMapping("/download/{fileName}")
    public void getFile(@PathVariable String fileName, HttpServletRequest request, HttpServletResponse response) {
        try {
            String realPath = filePath + fileName;
            serveFile(fileName, realPath, request, response);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException("500", "文件获取失败");
        }
    }

    @GetMapping("/{folder}/{fileName}")
    public void getFileByFolder(@PathVariable String folder, @PathVariable String fileName, HttpServletRequest request, HttpServletResponse response) {
        if (folder == null || fileName == null || !allowedFolders.contains(folder)) {
            throw new CustomException("404", "文件不存在");
        }
        String safeFileName = fileName.replace("\\", "/");
        if (safeFileName.contains("/") || safeFileName.contains("..")) {
            throw new CustomException("404", "文件不存在");
        }
        try {
            String realPath = filePath + folder + "/" + safeFileName;
            serveFile(safeFileName, realPath, request, response);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException("500", "文件获取失败");
        }
    }

    private void serveFile(String fileName, String realPath, HttpServletRequest request, HttpServletResponse response) throws IOException {
        File file = new File(realPath);
        if (!file.exists()) {
            throw new CustomException("404", "文件不存在");
        }

        String contentType;
        if (fileName.endsWith(".mp4")) contentType = "video/mp4";
        else if (fileName.endsWith(".mov")) contentType = "video/quicktime";
        else if (fileName.endsWith(".webm")) contentType = "video/webm";
        else if (fileName.endsWith(".ogg")) contentType = "video/ogg";
        else if (fileName.endsWith(".avi")) contentType = "video/x-msvideo";
        else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (fileName.endsWith(".png")) contentType = "image/png";
        else if (fileName.endsWith(".gif")) contentType = "image/gif";
        else if (fileName.endsWith(".webp")) contentType = "image/webp";
        else contentType = "application/octet-stream";

        response.setContentType(contentType);

        String displayFileName = fileName;
        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex > 0 && underscoreIndex < fileName.length() - 1) {
            String prefix = fileName.substring(0, underscoreIndex);
            if (prefix.matches("\\d{10,}")) {
                displayFileName = fileName.substring(underscoreIndex + 1);
            }
        }

        String disposition = (contentType.startsWith("image/") || contentType.startsWith("video/"))
                ? "inline" : "attachment";
        response.addHeader("Content-Disposition",
                disposition + ";filename=" + URLEncoder.encode(displayFileName, "UTF-8"));

        long fileLength = file.length();
        boolean isVideo = contentType.startsWith("video/");
        String range = request.getHeader("Range");

        if (isVideo && range != null && range.startsWith("bytes=")) {
            String rangeValue = range.substring("bytes=".length());
            String[] parts = rangeValue.split("-");
            long start = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
            long end = parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : fileLength - 1;
            if (end >= fileLength) {
                end = fileLength - 1;
            }
            if (start > end) {
                start = 0;
            }
            long contentLength = end - start + 1;
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.setHeader("Content-Length", String.valueOf(contentLength));
            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                 OutputStream os = response.getOutputStream()) {
                raf.seek(start);
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                while (remaining > 0) {
                    int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) break;
                    os.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        } else {
            response.setHeader("Content-Length", String.valueOf(fileLength));
            if (isVideo) {
                response.setHeader("Accept-Ranges", "bytes");
            }
            try (InputStream is = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }
        }
    }

}
