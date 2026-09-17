package com.groom.moigo.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groom.moigo.domain.auth.dto.OAuthState;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthStateServiceTest {

    private final OAuthStateService oAuthStateService = new OAuthStateService();

    @Test
    @DisplayName("발급한 state와 nonce를 한 번 검증할 수 있다")
    void validatesIssuedStateAndNonceOnce() {
        OAuthState issued = oAuthStateService.issueState();

        String validatedNonce = oAuthStateService.validateAndConsume(
                issued.state(),
                issued.nonce()
        );

        assertThat(validatedNonce).isEqualTo(issued.nonce());
        assertThatThrownBy(() -> oAuthStateService.validateAndConsume(
                issued.state(),
                issued.nonce()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("잘못된 nonce는 거부하고 해당 state를 재사용할 수 없다")
    void rejectsWrongNonceAndConsumesState() {
        OAuthState issued = oAuthStateService.issueState();

        assertThatThrownBy(() -> oAuthStateService.validateAndConsume(
                issued.state(),
                "wrong-nonce"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        assertThatThrownBy(() -> oAuthStateService.validateAndConsume(
                issued.state(),
                issued.nonce()
        )).isInstanceOf(BusinessException.class);
    }
}
