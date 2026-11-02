package eu.europeana.api.dataset.generation.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for generating and verifying MD5 checksum files for regular files.
 *
 * @author Srishti Singh
 * @since 22 October 2026
 */
public class CheckSumUtil {

    private static final String ALGORITHM = "MD5";
    private static final int BUFFER_SIZE = 8192;

    private CheckSumUtil() {
    }


    /**
     * Creates an MD5 checksum file for the given file. The checksum file will be created
     * in the same directory as the input file with the same name suffixed by ".md5".
     * The file will contain the computed MD5 checksum along with the file's name.
     *
     * @param file the file for which to create the MD5 checksum file; must be a regular file
     * @return the path to the newly created MD5 checksum file
     * @throws IOException if the input file does not exist or an error occurs while creating the checksum file
     */
    public static Path createMd5File(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("File does not exist: " + file);
        }

        String checksum = calculateMd5(file);
        Path checksumFile = file.resolveSibling(file.getFileName() + ".md5");
        String content = checksum + "  " + file.getFileName() + System.lineSeparator();
        Files.writeString(checksumFile, content, StandardCharsets.UTF_8);

        return checksumFile;
    }

    /*
     * Calculates the MD5 checksum of a file.
     *
     * @param file the file for which to calculate the MD5 checksum
     * @return the MD5 checksum as a hexadecimal string
     * @throws IOException if an error occurs while reading the file or calculating the checksum
     */
    public static String calculateMd5(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);

            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available", e);
        }
    }

    public static boolean verifyMd5File(Path file) throws IOException {
        Path checksumFile = file.resolveSibling(file.getFileName() + ".md5");

        if (!Files.isRegularFile(file) || !Files.isRegularFile(checksumFile)) {
            return false;
        }
        String content = Files.readString(checksumFile, StandardCharsets.UTF_8).trim();
        int separator = content.indexOf("  ");

        if (separator != 32
                || !content.substring(34)
                .equals(file.getFileName().toString())) {
            return false;
        }

        String expectedHash = content.substring(0, 32);

        if (!expectedHash.matches("[0-9a-f]{32}")) {
            return false;
        }

        String actualHash = calculateMd5(file);

        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                actualHash.getBytes(StandardCharsets.US_ASCII));
    }
}