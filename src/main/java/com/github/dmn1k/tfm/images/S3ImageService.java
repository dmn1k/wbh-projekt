package com.github.dmn1k.tfm.images;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Profile("prod")
@Service
public class S3ImageService implements ImageService {
    private static final String CONTENT_TYPE = "content-type";
    private static final String FILE_NAME = "fileName";
    private static final String IMAGES_BUCKET = "tfm-images";
    private static final String THUMBNAILS_BUCKET = "tfm-thumbnails";

    @Autowired
    private AmazonS3 amazonS3Client;

    @Autowired
    private ImageResizer imageResizer;

    @Override
    @SneakyThrows
    public String upload(MultipartFile multipartFile) {
        if (!StringUtils.isEmpty(multipartFile.getOriginalFilename())) {
            @Cleanup InputStream inputStream = multipartFile.getInputStream();

            String key = UUID.randomUUID().toString();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.addUserMetadata(CONTENT_TYPE, multipartFile.getContentType());
            metadata.addUserMetadata(FILE_NAME, multipartFile.getOriginalFilename());
            PutObjectRequest putObjectRequest = new PutObjectRequest(IMAGES_BUCKET,
                key, inputStream, metadata);
            putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);

            amazonS3Client.putObject(putObjectRequest);


            imageResizer.tryCreateThumbnail(multipartFile)
                .ifPresent(thumbnail -> saveImage(THUMBNAILS_BUCKET, key, metadata, thumbnail));

            return key;
        }

        return null;
    }

    @SneakyThrows
    private void saveImage(String bucket, String key, ObjectMetadata metadata, byte[] image) {
        @Cleanup ByteArrayInputStream thumbnailInputStream = new ByteArrayInputStream(image);

        PutObjectRequest putThumbnailRequest = new PutObjectRequest(bucket,
            key, thumbnailInputStream, metadata);
        putThumbnailRequest.setCannedAcl(CannedAccessControlList.PublicRead);

        amazonS3Client.putObject(putThumbnailRequest);
    }

    @Override
    @SneakyThrows
    public ResponseEntity<byte[]> download(String key) {
        return downloadFromBucket(key, IMAGES_BUCKET);
    }

    @Override
    @SneakyThrows
    public ResponseEntity<byte[]> downloadThumbnail(String key) {
        return downloadFromBucket(key, THUMBNAILS_BUCKET);
    }

    private ResponseEntity<byte[]> downloadFromBucket(String key, String bucket) throws IOException {
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            @Cleanup S3Object s3Object = amazonS3Client.getObject(getObjectRequest);

            return createResponse(s3Object);
        } catch (Exception e) {
            @Cleanup S3Object s3Object = amazonS3Client.getObject(new GetObjectRequest(THUMBNAILS_BUCKET, "thumbnail-missing.png"));

            return createResponse(s3Object);
        }
    }

    @SneakyThrows
    private ResponseEntity<byte[]> createResponse(S3Object s3Object) {
        @Cleanup S3ObjectInputStream objectInputStream = s3Object.getObjectContent();
        ObjectMetadata objectMetadata = s3Object.getObjectMetadata();

        byte[] bytes = IOUtils.toByteArray(objectInputStream);

        String metadataContentType = objectMetadata.getUserMetaDataOf(CONTENT_TYPE);
        MediaType contentType = metadataContentType == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(metadataContentType);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(contentType);
        httpHeaders.setContentLength(bytes.length);
        httpHeaders.setContentDisposition(ContentDisposition.builder("inline")
            .filename(objectMetadata.getUserMetaDataOf(FILE_NAME))
            .build());

        return new ResponseEntity<>(bytes, httpHeaders, HttpStatus.OK);
    }
}
