package com.vocaldocs.controllers;

import com.vocaldocs.models.History;
import com.vocaldocs.models.User;
import com.vocaldocs.repositories.HistoryRepository;
import com.vocaldocs.repositories.UserRepository;
import com.vocaldocs.services.AudioConversionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Controller
public class AudioController {

    private final AudioConversionService audioConversionService;
    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;

    @Autowired
    public AudioController(AudioConversionService audioConversionService, UserRepository userRepository, HistoryRepository historyRepository) {
        this.audioConversionService = audioConversionService;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> handleFileUploadApi(@RequestParam("documentFile") MultipartFile file, HttpSession session) {
        java.util.Map<String, String> response = new java.util.HashMap<>();
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            response.put("error", "Error: Please upload a valid document.");
            return ResponseEntity.badRequest().body(response);
        }

        String originalName = file.getOriginalFilename().toLowerCase();
        if (!originalName.endsWith(".pdf") && !originalName.endsWith(".txt") && !originalName.endsWith(".docx")) {
            response.put("error", "Error: Please upload a PDF, TXT, or DOCX file.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String generatedAudioPath = audioConversionService.processFileToAudio(file);
            File audioFile = new File(generatedAudioPath);
            String downloadUrl = "/download/" + audioFile.getName();
            
            // Save to history if user is logged in
            Long userId = (Long) session.getAttribute("userId");
            if (userId != null) {
                Optional<User> optUser = userRepository.findById(userId);
                if (optUser.isPresent()) {
                    String fileType = originalName.substring(originalName.lastIndexOf('.') + 1).toUpperCase();
                    History history = new History(optUser.get(), file.getOriginalFilename(), fileType, downloadUrl);
                    historyRepository.save(history);
                }
            }

            response.put("success", "Audio generated successfully!");
            response.put("downloadLink", downloadUrl);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Processing Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadAudioFile(@PathVariable("fileName") String fileName) {
        try {
            Path audioDir = Paths.get(System.getProperty("user.dir"), "audio").toAbsolutePath().normalize();
            Path filePath = audioDir.resolve(fileName).normalize();

            if (!filePath.startsWith(audioDir)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = fileName.endsWith(".mp3") ? "audio/mpeg" : "audio/x-wav";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/api/history")
    @ResponseBody
    public ResponseEntity<List<History>> getUserHistory(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(historyRepository.findByUserIdOrderByTimestampDesc(userId));
    }

    @DeleteMapping("/api/history/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteUserHistory(@PathVariable("id") Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<History> historyOpt = historyRepository.findById(id);
        if (historyOpt.isPresent() && historyOpt.get().getUser().getId().equals(userId)) {
            historyRepository.delete(historyOpt.get());
            return ResponseEntity.ok("Deleted successfully");
        }
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
