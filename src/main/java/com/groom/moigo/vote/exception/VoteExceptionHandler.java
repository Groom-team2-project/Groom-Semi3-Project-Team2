package com.groom.moigo.vote.exception;

import com.groom.moigo.vote.controller.VoteController;
import com.groom.moigo.vote.controller.VoteParticipationController;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 투표 도메인 전용 예외 핸들러.
 *
 * <p>TODO 공통 기반의 전역 예외 핸들러가 머지되면 이 클래스를 제거하고 공통 응답 포맷을 따른다. 그전까지 투표 API만 처리하도록 범위를 좁혀 둔다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {VoteController.class, VoteParticipationController.class})
public class VoteExceptionHandler {

	@ExceptionHandler(VoteException.class)
	public ResponseEntity<VoteErrorResponse> handleVoteException(VoteException exception) {
		VoteErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.body(VoteErrorResponse.of(errorCode, exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<VoteErrorResponse> handleValidationException(
			MethodArgumentNotValidException exception) {
		String message =
				exception.getBindingResult().getFieldErrors().stream()
						.map(error -> error.getField() + ": " + error.getDefaultMessage())
						.collect(Collectors.joining(", "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new VoteErrorResponse("INVALID_REQUEST", message));
	}
}
