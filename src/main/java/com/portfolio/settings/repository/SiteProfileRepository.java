package com.portfolio.settings.repository;

import com.portfolio.settings.entity.SiteProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteProfileRepository extends JpaRepository<SiteProfile, Long> {

	/** The singleton row (D-015). Empty until the admin first sets a photo or resume. */
	Optional<SiteProfile> findFirstByOrderByIdAsc();
}
