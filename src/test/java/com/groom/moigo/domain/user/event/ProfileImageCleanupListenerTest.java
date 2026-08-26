package com.groom.moigo.domain.user.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileImageCleanupListenerTest {

    @Mock
    private S3Client s3Client;

    private ProfileImageCleanupListener listener;

    @BeforeEach
    void setUp() {
        listener = new ProfileImageCleanupListener(s3Client);
        ReflectionTestUtils.setField(listener, "bucketName", "moigo-images");
    }

    @Test
    void deletesPreviousImageAfterCommit() {
        listener.deletePreviousImage(
                new ProfileImageChangedEvent("1/new.jpg", "1/previous.jpg")
        );

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("moigo-images");
        assertThat(captor.getValue().key()).isEqualTo("1/previous.jpg");
    }

    @Test
    void doesNotDeletePreviousImageWhenThereWasNone() {
        listener.deletePreviousImage(
                new ProfileImageChangedEvent("1/new.jpg", null)
        );

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deletesUploadedImageAfterRollback() {
        listener.deleteUploadedImage(
                new ProfileImageChangedEvent("1/new.jpg", "1/previous.jpg")
        );

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo("1/new.jpg");
    }

    @Test
    void ignoresS3DeletionFailure() {
        doThrow(new RuntimeException("S3 unavailable"))
                .when(s3Client)
                .deleteObject(any(DeleteObjectRequest.class));

        assertThatCode(() -> listener.deleteUploadedImage(
                new ProfileImageChangedEvent("1/new.jpg", null)
        )).doesNotThrowAnyException();
    }
}
