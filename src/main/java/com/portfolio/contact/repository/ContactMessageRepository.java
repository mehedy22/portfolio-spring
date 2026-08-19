package com.portfolio.contact.repository;

import com.portfolio.contact.entity.ContactMessage;
import com.portfolio.contact.entity.ContactMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

	Page<ContactMessage> findByStatus(ContactMessageStatus status, Pageable pageable);
}
