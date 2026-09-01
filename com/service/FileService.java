package service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {
    public String readFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
    }
}