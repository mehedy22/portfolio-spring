package com.portfolio.problemsolving.repository;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.problemsolving.entity.ProblemSolvingProfile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSolvingProfileRepository extends JpaRepository<ProblemSolvingProfile, Long> {

	List<ProblemSolvingProfile> findByStatusOrderByDisplayOrderAscIdAsc(ContentStatus status);

	List<ProblemSolvingProfile> findAllByOrderByDisplayOrderAscIdAsc();

	boolean existsByPlatformIgnoreCaseAndHandleIgnoreCase(String platform, String handle);

	boolean existsByPlatformIgnoreCaseAndHandleIgnoreCaseAndIdNot(String platform, String handle, Long id);
}
