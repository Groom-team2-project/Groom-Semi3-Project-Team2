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

    public static MemberEntity createFromInvitation(PlanEntity plan, UserEntity user, InvitationRole invitationRole) {
        return new MemberEntity(plan, user, MemberRole.valueOf(invitationRole.name()));
    }

    public void rejoin(MemberRole newRole) {
        if (this.status == MemberStatus.JOINED) {
            throw new IllegalStateException("이미 참여 중인 멤버입니다.");
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
        if (this.role == MemberRole.OWNER) {
            throw new IllegalStateException("OWNER 권한은 변경할 수 없습니다.");
        }
        if (newRole == MemberRole.OWNER) {
            throw new IllegalStateException("OWNER 권한은 다른 회원에게 부여할 수 없습니다.");
        }
        this.role = newRole;
    }

    public void leave() {
        if (this.role == MemberRole.OWNER) {
            throw new IllegalStateException("OWNER는 계획에서 나갈 수 없습니다.");
        }
        this.status = MemberStatus.LEFT;
    }

    public void remove() {
        if (this.role == MemberRole.OWNER) {
            throw new IllegalStateException("OWNER는 내보낼 수 없습니다.");
        }
        this.status = MemberStatus.LEFT;
    }
}