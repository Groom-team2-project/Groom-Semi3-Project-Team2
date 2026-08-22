package com.groom.moigo.domain.user.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageCleanupListener {
    private final S3Client s3Client;

    @Value("${app.aws.s3.bucket}")
    private String bucketName;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deletePreviousImage(ProfileImageChangedEvent event) {
        if (event.prevKey() == null) {
            return;
        }

        deleteQuietly(event.prevKey(), "기존 프로필 이미지");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void deleteUploadedImage(ProfileImageChangedEvent event) {
        deleteQuietly(event.newKey(), "롤백된 신규 프로필 이미지");
    }

    private void deleteQuietly(String key, String description) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
        } catch (Exception exception) {
            log.warn("{} 삭제 실패. key={}", description, key, exception);
        }
    }
}
