package com.portfolio.certification.repository;

import com.portfolio.certification.entity.Certification;
import com.portfolio.common.content.ContentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

	List<Certification> findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus status);

	List<Certification> findAllByOrderByDisplayOrderAscIdDesc();
}
