package com.project.cadence.utility;

public class Utils {
    public static String getContentTypeFromBase64(String base64Data) {
        if (base64Data == null || !base64Data.contains(",")) {
            throw new IllegalArgumentException("Invalid Base64 data");
        }

        // Example: "data:image/png;base64,iVBORw0KGgoAAAANS..."
        String[] parts = base64Data.split(",");
        String metaData = parts[0]; // "data:image/png;base64"

        if (!metaData.contains(";")) {
            throw new IllegalArgumentException("Invalid Base64 metadata format");
        }

        // Extract MIME type
        return metaData.substring(metaData.indexOf(":") + 1, metaData.indexOf(";"));
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null) return "file";

        // 1. Replace whitespace with underscore
        String sanitized = fileName.replaceAll("\\s+", "_");

        // 2. Remove special characters except letters, numbers, underscore, and hyphen
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_-]", "");

        return sanitized;
    }

}
