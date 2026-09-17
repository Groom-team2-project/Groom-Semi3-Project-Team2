package com.groom.moigo.domain.plan.entity;

import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity owner;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "recruitment_count")
    private Integer recruitmentCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 계획 생성 검증
    public static PlanEntity create(UserEntity owner, String title, String description,
                                    LocalDate startDate, LocalDate endDate, Integer recruitmentCount) {
        validate(title, startDate, endDate, recruitmentCount);

        PlanEntity plan = new PlanEntity();
        plan.owner = owner;
        plan.title = title;
        plan.description = description;
        plan.startDate = startDate;
        plan.endDate = endDate;
        plan.recruitmentCount = recruitmentCount;
        return plan;
    }

    // 계획 수정 검증
    public void update(String title, String description,
                       LocalDate startDate, LocalDate endDate, Integer recruitmentCount) {
        String nextTitle = title != null ? title : this.title;
        String nextDescription = description != null ? description : this.description;
        LocalDate nextStartDate = startDate != null ? startDate : this.startDate;
        LocalDate nextEndDate = endDate != null ? endDate : this.endDate;
        Integer nextRecruitmentCount = recruitmentCount != null ? recruitmentCount : this.recruitmentCount;

        validate(nextTitle, nextStartDate, nextEndDate, nextRecruitmentCount);

        this.title = nextTitle;
        this.description = nextDescription;
        this.startDate = nextStartDate;
        this.endDate = nextEndDate;
        this.recruitmentCount = nextRecruitmentCount;
    }

    private static void validate(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            Integer recruitmentCount
    ) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PLAN_TITLE);
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_PLAN_DATE);
        }

        if (recruitmentCount != null && recruitmentCount < 1) {
            throw new BusinessException(ErrorCode.INVALID_RECRUITMENT_COUNT);
        }
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}