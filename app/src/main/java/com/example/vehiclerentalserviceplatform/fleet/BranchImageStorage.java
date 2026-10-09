package com.example.vehiclerentalserviceplatform.fleet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Stores decoded raster uploads outside the application source tree. */
@Service
public class BranchImageStorage {
    private final Path directory;
    public BranchImageStorage(@Value("${driveease.branch-images-directory:./data/branch-images}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    public byte[] validate(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return null;
        if (image.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Branch images must be 5 MB or smaller.");
        byte[] bytes = image.getBytes();
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("Use a valid JPEG or PNG branch image.");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw new IllegalArgumentException("Use a JPEG or PNG branch image.");
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 25_000_000)
                    throw new IllegalArgumentException("Branch images must be under 25 megapixels.");
                if (reader.read(0) == null) throw new IllegalArgumentException("The image could not be read.");
            } finally { reader.dispose(); }
        }
        return bytes;
    }
    public String store(byte[] bytes) throws IOException {
        if (bytes == null) return null;
        // Re-encode the decoded image to discard metadata and never serve uploaded executable content.
        var decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + ".png";
        ImageIO.write(decoded, "png", directory.resolve(filename).toFile());
        return "/branch-images/" + filename;
    }
    public Path directory() { return directory; }
}
