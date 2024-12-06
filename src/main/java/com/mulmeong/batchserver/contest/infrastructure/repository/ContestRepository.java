package com.mulmeong.batchserver.contest.infrastructure.repository;

import com.mulmeong.batchserver.contest.entity.contest.Contest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

public interface ContestRepository extends JpaRepository<Contest, Long> {

    Iterable<Contest> findByStatusTrue();
}
