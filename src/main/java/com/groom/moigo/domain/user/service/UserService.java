package com.groom.moigo.domain.user.service;

import com.groom.moigo.domain.user.event.ProfileImageChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import com.groom.moigo.domain.user.dto.UserProfileResponse;
import com.groom.moigo.domain.user.dto.NicknameUpdateRequest;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import com.groom.moigo.global.error.S3Exception;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private static final int MAX_IMAGE_WIDTH = 4096;
    private static final int MAX_IMAGE_HEIGHT = 4096;
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;
    private static final long MAX_PROFILE_IMAGE_SIZE = 10L * 1024 * 1024;

    private final UserRepository userRepository;
    private final S3Client s3Client;

    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.aws.s3.bucket}")
    private String bucketName;

    @Value("${app.aws.s3.public-url}")
    private String s3PublicUrl;

    public UserProfileResponse getMe(Long userId) {
        UserEntity user = findUser(userId);

        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateUserNickname(Long userId, NicknameUpdateRequest request) {
        UserEntity user = findUser(userId);

        user.updateNickname(
                request.nickname()
        );

        return UserProfileResponse.from(user);
    }

    private String validateImage(MultipartFile image) {
        if (image.isEmpty() || Objects.isNull(image.getOriginalFilename())) {
            throw new S3Exception(ErrorCode.EMPTY_FILE_EXCEPTION);
        }

        if (image.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new S3Exception(ErrorCode.IMAGE_FILE_TOO_LARGE);
        }

        String contentType = Objects.toString(image.getContentType(), "")
                .toLowerCase(Locale.ROOT);
        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new S3Exception(ErrorCode.INVALID_FILE_EXTENSION);
        };

        try (InputStream inputStream = image.getInputStream();
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {

            if (imageInputStream == null) {
                throw new S3Exception(ErrorCode.INVALID_IMAGE_FILE);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new S3Exception(ErrorCode.INVALID_IMAGE_FILE);
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInputStream, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;

                if (width > MAX_IMAGE_WIDTH
                        || height > MAX_IMAGE_HEIGHT
                        || pixels > MAX_IMAGE_PIXELS) {
                    throw new S3Exception(ErrorCode.IMAGE_PIXELS_TOO_LARGE);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new S3Exception(ErrorCode.IO_EXCEPTION_ON_IMAGE_UPLOAD, exception);
        }

        return extension;
    }

    @Transactional
    public UserProfileResponse updateUserProfileImage(Long userId, MultipartFile image) {
        String extension = validateImage(image);

        UserEntity user = findUser(userId);
        String prevImgUrl = user.getProfileImage();

        String newKey = userId + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(newKey)
                .contentType(image.getContentType())
                .contentLength(image.getSize())
                .build();

        try (InputStream is = image.getInputStream()) {
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(is, image.getSize())
            );
        } catch (IOException e) {
            throw new S3Exception(ErrorCode.IO_EXCEPTION_ON_IMAGE_UPLOAD);
        } catch (Exception e) {
            throw new S3Exception(ErrorCode.PUT_OBJECT_EXCEPTION);
        }
        String prefix = s3PublicUrl.replaceAll("/+$", "") + "/";

        String prevKey = extractPreviousKey(prevImgUrl, prefix);
        eventPublisher.publishEvent(new ProfileImageChangedEvent(newKey, prevKey));

        String imgUrl = prefix + newKey;
        user.updateProfileImage(imgUrl);

        return UserProfileResponse.from(user);
    }

    private String extractPreviousKey(
            String prevImgUrl,
            String prefix
    ) {
        if (prevImgUrl == null
                || !prevImgUrl.startsWith(prefix)) {
            return null;
        }

        return prevImgUrl.substring(prefix.length());
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "인증된 사용자 정보를 찾을 수 없습니다"
                ));
    }
}
