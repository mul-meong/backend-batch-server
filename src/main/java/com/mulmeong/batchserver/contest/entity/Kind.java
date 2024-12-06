package com.mulmeong.batchserver.contest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Kind {


    FISH("물고기컵"),
    AQUARIUM("수족관컵");

    private final String kind;


}
