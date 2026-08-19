package com.portfolio.settings.repository;

import com.portfolio.settings.entity.SocialLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {

	List<SocialLink> findAllByOrderByDisplayOrderAscIdAsc();

	List<SocialLink> findByVisibleTrueOrderByDisplayOrderAscIdAsc();
}
