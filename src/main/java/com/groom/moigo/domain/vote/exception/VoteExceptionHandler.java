package com.groom.moigo.domain.vote.exception;

import com.groom.moigo.domain.vote.controller.VoteController;
import com.groom.moigo.domain.vote.controller.VoteParticipationController;
import com.groom.moigo.global.error.ErrorCode;
import com.groom.moigo.global.response.CommonResponse;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 투표 도메인 전용 예외 핸들러. 응답은 공통 {@link CommonResponse} 규격을 따른다.
 *
 * <p>TODO 공통 전역 예외 핸들러가 생기면 이 클래스를 제거하고 그쪽을 따른다. 그전까지 투표 API만 처리하도록 범위를 좁혀 둔다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {VoteController.class, VoteParticipationController.class})
public class VoteExceptionHandler {

	@ExceptionHandler(VoteException.class)
	public ResponseEntity<CommonResponse<Void>> handleVoteException(VoteException exception) {
		VoteErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.body(CommonResponse.error(errorCode.name(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CommonResponse<Void>> handleValidationException(
			MethodArgumentNotValidException exception) {
		String message =
				exception.getBindingResult().getFieldErrors().stream()
						.map(error -> error.getField() + ": " + error.getDefaultMessage())
						.collect(Collectors.joining(", "));
		return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
				.body(CommonResponse.error(ErrorCode.INVALID_INPUT_VALUE.name(), message));
	}
}
