package com.service.report.generator.utility;

import com.service.report.generator.exception.InvalidDataException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Component
public class FileUtils {


    public boolean ensureDirectoryExists(String directoryPath) {
        File file = new File(directoryPath);
        return createDirectoryIfNotExist(file);
    }

    private boolean createDirectoryIfNotExist(File file) {
        return file.exists() || file.mkdirs();
    }

    public String getExtension(String fileName) throws IllegalArgumentException {
        if (StringUtils.isBlank(fileName))
            return null;

        int index = indexOfExtension(fileName);
        return index == -1
                ? null
                : fileName.substring(index + 1);
    }

    private int indexOfExtension(String fileName) throws IllegalArgumentException {
        if (StringUtils.isBlank(fileName))
            return -1;

        int offset = fileName.lastIndexOf(46);
        int lastSeparator = indexOfLastSeparator(fileName);
        return lastSeparator > offset
                ? -1
                : offset;
    }

    private int indexOfLastSeparator(String fileName) {
        if (StringUtils.isBlank(fileName))
            return -1;

        int lastUnixPos = fileName.lastIndexOf(47);
        int lastWindowsPos = fileName.lastIndexOf(92);
        return Math.max(lastUnixPos, lastWindowsPos);
    }

    public String hasValidFileExtension(String fileName) {
        if (StringUtils.isBlank(fileName))
            return null;

        return hasValidFileExtension(
                fileName,
                new HashSet<>(Set.of("csv", "json", "xlsx"))
        );
    }

    /**
     * Deletes a file locally using the specified file path.
     *
     * @param filePath The path of the file to be deleted.
     * @return true if the file is successfully deleted or does not exist; false otherwise.
     */
    public boolean deleteFileLocally(String filePath) {
        File existingFile = new File(filePath);
        return !existingFile.exists() || existingFile.delete();
    }

    private String hasValidFileExtension(String fileName, Set<String> allowedExtensions) {
        if (StringUtils.isBlank(fileName))
            return null;

        String fileExtension = getExtension(fileName);
        if (null == fileExtension)
            return null;

        if(!allowedExtensions.contains(fileExtension.toLowerCase()))
            return null;

        return fileExtension;
    }
}
