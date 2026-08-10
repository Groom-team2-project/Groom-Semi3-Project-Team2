package com.groom.moigo.domain.plan.entity;

import com.groom.moigo.domain.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invitationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private UserEntity inviter;

    @Column(name = "invite_code", nullable = false, unique = true, length = 20)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 새 초대 링크를 만듭니다. 생성 시점 상태는 항상 ACTIVE로 시작합니다. */
    public static InvitationEntity create(PlanEntity plan, UserEntity inviter, String inviteCode, LocalDateTime expiresAt) {
        InvitationEntity invitation = new InvitationEntity();
        invitation.plan = plan;
        invitation.inviter = inviter;
        invitation.inviteCode = inviteCode;
        invitation.status = InvitationStatus.ACTIVE;
        invitation.expiresAt = expiresAt;
        return invitation;
    }

    /** 재발급하거나 명시적으로 취소할 때 호출합니다. */
    public void revoke() {
        this.status = InvitationStatus.REVOKED;
    }

    public void expireIfNeeded() {
        if (this.status == InvitationStatus.ACTIVE && this.expiresAt.isBefore(LocalDateTime.now())) {
            this.status = InvitationStatus.EXPIRED;
        }
    }

    public boolean isUsable() {
        return this.status == InvitationStatus.ACTIVE && this.expiresAt.isAfter(LocalDateTime.now());
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}