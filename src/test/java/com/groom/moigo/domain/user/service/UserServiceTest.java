package com.groom.moigo.domain.user.service;

import com.groom.moigo.domain.user.dto.UserProfileResponse;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.ErrorCode;
import com.groom.moigo.global.error.S3Exception;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, s3Client, eventPublisher);
        ReflectionTestUtils.setField(userService, "bucketName", "moigo-images");
        ReflectionTestUtils.setField(userService, "s3PublicUrl", "https://images.example.com/");
    }

    @Test
    void uploadsValidatedImageAndReturnsPublicUrl() throws IOException {
        UserEntity user = UserEntity.createKakaoUser(123L, "user@example.com", "사용자");
        ReflectionTestUtils.setField(user, "userId", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpeg",
                "image/jpeg",
                jpegBytes()
        );

        UserProfileResponse response = userService.updateUserProfileImage(1L, image);

        assertThat(response.profileImage())
                .startsWith("https://images.example.com/1/")
                .endsWith(".jpg")
                .doesNotContain("//1/");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void rejectsFileWhoseContentIsNotAnImage() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpg",
                "image/jpeg",
                "not-an-image".getBytes()
        );

        assertThatThrownBy(() -> userService.updateUserProfileImage(1L, image))
                .isInstanceOfSatisfying(S3Exception.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_IMAGE_FILE)
                );
    }

    @Test
    void rejectsImageLargerThanTenMegabytes() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpg",
                "image/jpeg",
                new byte[10 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> userService.updateUserProfileImage(1L, image))
                .isInstanceOfSatisfying(S3Exception.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IMAGE_FILE_TOO_LARGE)
                );
    }

    @Test
    void rejectsImageWhoseResolutionExceedsLimit() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpg",
                "image/jpeg",
                jpegBytes(4097, 1)
        );

        assertThatThrownBy(() -> userService.updateUserProfileImage(1L, image))
                .isInstanceOfSatisfying(S3Exception.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IMAGE_PIXELS_TOO_LARGE)
                );
    }

    private byte[] jpegBytes() throws IOException {
        return jpegBytes(2, 2);
    }

    private byte[] jpegBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return output.toByteArray();
    }
}
