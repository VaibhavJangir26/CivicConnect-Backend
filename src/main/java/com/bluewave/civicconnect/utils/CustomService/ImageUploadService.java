package com.bluewave.civicconnect.utils.CustomService;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a file to Cloudinary and returns the response Map containing
     * "secure_url" and "public_id".
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }
            // Uploads image bytes using Cloudinary SDK
            return (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an image from Cloudinary using its public ID.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteImage(String publicId) {
        try {
            return (Map<String, Object>) cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image from Cloudinary: " + e.getMessage(), e);
        }
    }
}