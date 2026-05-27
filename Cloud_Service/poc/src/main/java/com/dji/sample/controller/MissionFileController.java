package com.dji.sample.controller;

import com.dji.sample.dto.response.MissionFileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionFileController {

    @Value("${app.upload.mission-dir:uploads/missions}")
    private String missionUploadDir;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MissionFileUploadResponse uploadMissionFile(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        Path uploadPath = Paths.get(missionUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalName = file.getOriginalFilename() == null
                ? "mission-file"
                : file.getOriginalFilename();

        String safeOriginalName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedFileName = UUID.randomUUID() + "_" + safeOriginalName;

        Path targetPath = uploadPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String encodedFileName = URLEncoder.encode(storedFileName, StandardCharsets.UTF_8);

        return MissionFileUploadResponse.builder()
                .file(storedFileName)
                .fileName(storedFileName)
                .downloadUrl("/api/v1/missions/files/download/" + encodedFileName)
                .build();
    }

    @GetMapping("/files/download/{fileName}")
    public ResponseEntity<Resource> downloadMissionFile(
            @PathVariable String fileName
    ) throws Exception {

        Path filePath = Paths.get(missionUploadDir)
                .toAbsolutePath()
                .normalize()
                .resolve(fileName)
                .normalize();

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File not found");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\""
                )
                .body(resource);
    }

    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<Void> deleteMissionFile(
            @PathVariable String fileName
    ) throws Exception {

        Path filePath = Paths.get(missionUploadDir)
                .toAbsolutePath()
                .normalize()
                .resolve(fileName)
                .normalize();

        Files.deleteIfExists(filePath);

        return ResponseEntity.noContent().build();
    }
}