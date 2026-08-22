package com.groom.moigo.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {
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

        try (InputStream inputStream = image.getInputStream()) {
            if (ImageIO.read(inputStream) == null) {
                throw new S3Exception(ErrorCode.INVALID_IMAGE_FILE);
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

        String s3FileName = userId + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3FileName)
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
        String imgUrl = prefix + s3FileName;


        user.updateProfileImage(imgUrl);
        eventPublisher.publishEvent(new PrevProfileImageDeleteEvent(prevImgUrl, prefix));
        return UserProfileResponse.from(user);
    }

    private record PrevProfileImageDeleteEvent(
            String prevImgUrl, String prefix
    ) {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deletePrevImage(PrevProfileImageDeleteEvent event) {
        if (event.prevImgUrl != null && event.prevImgUrl.startsWith(event.prefix)) {
            String prevKey = event.prevImgUrl.substring(event.prefix.length());
            try {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(prevKey)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
            } catch (Exception e) {
                log.warn("기존 프로필 이미지 삭제 실패. key={}", prevKey, e);
            }
        }
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "인증된 사용자 정보를 찾을 수 없습니다"
                ));
    }
}
