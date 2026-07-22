package com.tripdesigner.trip.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 行程分享领域实体。
 *
 * 代表一个行程的分享链接，可被外部用户通过 token 访问行程详情。
 * 支持 VIEW/EDIT 两种分享类型、最大访问次数限制、过期时间、撤销等能力。
 *
 * 使用不可变对象模式：
 * - 所有字段通过 @Getter 只读
 * - 状态变更通过 withXxx() 方法返回新实例
 * - 通过静态工厂方法 TripShare.create() 创建
 */
@Getter
@Builder
public class TripShare {

    /** 主键 ID */
    private Long id;

    /** 关联的行程 ID */
    private Long tripId;

    /** 行程所有者用户 ID */
    private Long ownerUserId;

    /** 分享 token（UUID 去横线，用于公开访问） */
    private String shareToken;

    /** 分享类型 */
    private ShareType shareType;

    /** 最大访问次数（null 表示不限制） */
    private Integer maxViews;

    /** 当前已访问次数 */
    private Integer currentViews;

    /** 过期时间（null 表示不过期） */
    private Instant expiresAt;

    /** 状态 */
    private ShareStatus status;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /** 乐观锁版本号 */
    private Integer version;

    /**
     * 静态工厂方法：创建一个新的行程分享。
     * 初始 currentViews=0，status=ACTIVE，version=0。
     *
     * @param tripId      行程 ID
     * @param ownerUserId 所有者用户 ID
     * @param shareToken  分享 token
     * @param shareType   分享类型
     * @param maxViews    最大访问次数（可 null）
     * @param expiresAt   过期时间（可 null）
     * @return 新的 TripShare 实例
     */
    public static TripShare create(Long tripId, Long ownerUserId, String shareToken,
                                   ShareType shareType, Integer maxViews, Instant expiresAt) {
        return TripShare.builder()
                .tripId(tripId)
                .ownerUserId(ownerUserId)
                .shareToken(shareToken)
                .shareType(shareType)
                .maxViews(maxViews)
                .currentViews(0)
                .expiresAt(expiresAt)
                .status(ShareStatus.ACTIVE)
                .version(0)
                .build();
    }

    /**
     * 生成新的 TripShare 实例，仅变更状态字段。
     *
     * @param status 新状态
     * @return 更新了状态的新实例
     */
    public TripShare withUpdatedStatus(ShareStatus status) {
        return TripShare.builder()
                .id(id).tripId(tripId).ownerUserId(ownerUserId).shareToken(shareToken)
                .shareType(shareType).maxViews(maxViews).currentViews(currentViews)
                .expiresAt(expiresAt).status(status)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    /**
     * 生成新的 TripShare 实例，更新可编辑字段。
     * 仅更新非 null 的字段，保持原有值不变。
     *
     * @param maxViews  新最大访问次数（null 忽略）
     * @param expiresAt 新过期时间（null 忽略）
     * @return 更新了字段的新实例
     */
    public TripShare withUpdatedFields(Integer maxViews, Instant expiresAt) {
        return TripShare.builder()
                .id(id).tripId(tripId).ownerUserId(ownerUserId).shareToken(shareToken)
                .shareType(shareType)
                .maxViews(maxViews != null ? maxViews : this.maxViews)
                .currentViews(currentViews)
                .expiresAt(expiresAt != null ? expiresAt : this.expiresAt)
                .status(status)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }

    /**
     * 是否已过期。
     * 满足以下任一条件即视为过期：
     * - 状态已为 EXPIRED
     * - expiresAt 不为 null 且早于当前时间
     * - maxViews 不为 null 且 currentViews 已达到或超过 maxViews
     *
     * @return true 表示已过期
     */
    public boolean isExpired() {
        if (status == ShareStatus.EXPIRED) {
            return true;
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return true;
        }
        if (maxViews != null && currentViews != null && currentViews >= maxViews) {
            return true;
        }
        return false;
    }

    /**
     * 是否已撤销。
     *
     * @return true 表示已撤销
     */
    public boolean isRevoked() {
        return status == ShareStatus.REVOKED;
    }

    /**
     * 是否可访问。
     * 同时满足：未撤销、未过期。
     *
     * @return true 表示可以访问
     */
    public boolean canAccess() {
        return !isRevoked() && !isExpired();
    }

    /**
     * 增加一次访问次数。
     * 返回新的 TripShare 实例，currentViews + 1。
     * 如果已达上限，状态自动置为 EXPIRED。
     *
     * @return 更新了访问次数的新实例
     */
    public TripShare incrementViews() {
        int newViews = (currentViews != null ? currentViews : 0) + 1;
        ShareStatus newStatus = status;
        if (maxViews != null && newViews >= maxViews) {
            newStatus = ShareStatus.EXPIRED;
        }
        return TripShare.builder()
                .id(id).tripId(tripId).ownerUserId(ownerUserId).shareToken(shareToken)
                .shareType(shareType).maxViews(maxViews).currentViews(newViews)
                .expiresAt(expiresAt).status(newStatus)
                .createdAt(createdAt).updatedAt(updatedAt).version(version)
                .build();
    }
}
