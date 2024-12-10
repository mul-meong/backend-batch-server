package com.mulmeong.batchserver.contest.entity.contestRead;

import com.mulmeong.batchserver.contest.entity.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "contest_post")
public class ContestPostRead {

    @Id
    private String id;
    private String postUuid;
    private Long contestId;
    private String memberUuid;
    private String mediaUrl;
    private MediaType mediaType;
    private LocalDateTime createdAt;
    private Long voteCount;


}
