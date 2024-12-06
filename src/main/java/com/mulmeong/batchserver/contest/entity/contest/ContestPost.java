package com.mulmeong.batchserver.contest.entity.contest;

import com.mulmeong.batchserver.contest.entity.MediaType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ContestPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 36)
    private String postUuid;
    @Column(nullable = false)
    private Long contestId;
    @Column(nullable = false, length = 36)
    private String memberUuid;
    @Column(nullable = false, length = 2083)
    private String mediaUrl;
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ContestPost(
            Long id,
            String postUuid,
            Long contestId,
            String memberUuid,
            String mediaUrl,
            MediaType mediaType
    ) {
        this.id = id;
        this.postUuid = postUuid;
        this.contestId = contestId;
        this.memberUuid = memberUuid;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
