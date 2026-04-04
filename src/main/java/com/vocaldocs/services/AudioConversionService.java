package com.vocaldocs.services;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import javax.sound.sampled.AudioFileFormat;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AudioConversionService {

    // Use absolute paths based on the current working directory to prevent Tomcat Temp directory issues
    private final Path UPLOADS_DIR = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
    private final Path AUDIO_DIR = Paths.get(System.getProperty("user.dir"), "audio").toAbsolutePath().normalize();

    public AudioConversionService() {
        // Ensure directories exist immediately upon service instantiation
        try {
            Files.createDirectories(UPLOADS_DIR);
            Files.createDirectories(AUDIO_DIR);
        } catch (IOException e) {
            System.err.println("Could not create necessary storage directories: " + e.getMessage());
        }
    }

    /**
     * Extracts text, converts to WAV via FreeTTS, optionally to MP3, and returns path to final audio.
     */
    public String processFileToAudio(MultipartFile file) throws Exception {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new Exception("Uploaded file is empty.");
        }

        // 1. Save uploaded PDF temporarily
        String originalName = file.getOriginalFilename();
        String baseName = UUID.randomUUID().toString() + "_" + originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
        
        Path pdfPath = UPLOADS_DIR.resolve(baseName);
        File savedPdf = pdfPath.toFile();
        
        // This transferTo explicitly uses the absolute path we constructed above
        file.transferTo(savedPdf);

        // 2. Extract Text based on file extension
        String text = "";
        String origLower = originalName.toLowerCase();
        
        if (origLower.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(savedPdf)) {
                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(document);
            }
        } else if (origLower.endsWith(".txt")) {
            text = new String(Files.readAllBytes(pdfPath));
        } else if (origLower.endsWith(".docx")) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(savedPdf);
                 org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(fis);
                 org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc)) {
                text = extractor.getText();
            }
        } else if (origLower.endsWith(".doc")) {
            throw new Exception("Legacy .doc format not supported. Please use .docx or PDF.");
        } else {
            throw new Exception("Unsupported file format.");
        }

        if (text == null || text.trim().isEmpty()) {
            throw new Exception("The uploaded file could not be parsed or contains no extractable text.");
        }

        // 3. Convert to WAV via FreeTTS
        String baseAudioName = baseName.replace(".pdf", "");
        
        // SingleFileAudioPlayer automatically appends .wav to the path we provide
        String rawWavPathStr = AUDIO_DIR.resolve(baseAudioName).toString(); 
        String fullWavPath = rawWavPathStr + ".wav";
        
        VoiceManager voiceManager = VoiceManager.getInstance();
        Voice voice = voiceManager.getVoice("kevin16");
        
        if (voice == null) {
            throw new Exception("Cannot locate Voice 'kevin16'. Voice loading failed.");
        }

        voice.allocate();
        try {
            SingleFileAudioPlayer audioPlayer = new SingleFileAudioPlayer(rawWavPathStr, AudioFileFormat.Type.WAVE);
            voice.setAudioPlayer(audioPlayer);
            voice.speak(text);
            audioPlayer.close(); // Flush streams
        } finally {
            voice.deallocate(); // Ensure release
        }

        // 4. Try FFmpeg MP3 Conversion Optional Step
        String mp3Path = AUDIO_DIR.resolve(baseAudioName + ".mp3").toString();
        if (convertWavToMp3(fullWavPath, mp3Path)) {
            return mp3Path; // Prefer returning MP3
        }

        return fullWavPath; // Fallback to WAV
    }

    private boolean convertWavToMp3(String wavPath, String mp3Path) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-i", wavPath, mp3Path);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && new File(mp3Path).exists()) {
                // Delete the bulky wav if mp3 conversion is successful
                new File(wavPath).delete();
                return true;
            }
        } catch (Exception e) {
            System.err.println("FFmpeg not installed or conversion failed: " + e.getMessage());
        }
        return false;
    }
}
