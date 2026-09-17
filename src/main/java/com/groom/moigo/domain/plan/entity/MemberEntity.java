package com.groom.moigo.domain.plan.entity;

import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 계획에 참여한 멤버 엔티티입니다.
 */
@Entity
@Table(
        name = "members",
        uniqueConstraints = @UniqueConstraint(name = "uk_members_plan_user", columnNames = {"plan_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    private MemberEntity(PlanEntity plan, UserEntity user, MemberRole role) {
        this.plan = plan;
        this.user = user;
        this.role = role;
        this.status = MemberStatus.JOINED;
        this.joinedAt = LocalDateTime.now();
    }

    public static MemberEntity createOwner(PlanEntity plan, UserEntity user) {
        return new MemberEntity(plan, user, MemberRole.OWNER);
    }

    public static MemberEntity createFromInvitation(PlanEntity plan, UserEntity user, MemberRole role) {
        return new MemberEntity(plan, user, role);
    }

    public void rejoin(MemberRole newRole) {
        if (newRole == null) {
            throw new BusinessException(ErrorCode.MEMBER_ROLE_REQUIRED);
        }
        if (this.status == MemberStatus.JOINED) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_JOINED);
        }
        this.role = newRole;
        this.status = MemberStatus.JOINED;
        this.joinedAt = LocalDateTime.now();
    }

    public boolean isOwner() {
        return this.role == MemberRole.OWNER;
    }

    public boolean isJoined() {
        return this.status == MemberStatus.JOINED;
    }

    public void changeRole(MemberRole newRole) {
        if (newRole == null) {
            throw new BusinessException(ErrorCode.MEMBER_ROLE_REQUIRED);
        }
        if (this.role == MemberRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_ROLE_CANNOT_BE_CHANGED);
        }
        if (newRole == MemberRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_ROLE_CANNOT_BE_ASSIGNED);
        }
        this.role = newRole;
    }

    public void leave() {
        if (this.role == MemberRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE);
        }
        this.status = MemberStatus.LEFT;
    }

    public void remove() {
        if (this.role == MemberRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_BE_REMOVED);
        }
        this.status = MemberStatus.LEFT;
    }
}