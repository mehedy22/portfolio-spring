package com.portfolio.contact.service.impl;

import com.portfolio.common.exception.RateLimitExceededException;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.common.ratelimit.RateLimiter;
import com.portfolio.common.response.PageResponse;
import com.portfolio.contact.ContactProperties;
import com.portfolio.contact.dto.ContactMessageRequest;
import com.portfolio.contact.dto.ContactMessageResponse;
import com.portfolio.contact.entity.ContactMessage;
import com.portfolio.contact.entity.ContactMessageStatus;
import com.portfolio.contact.mapper.ContactMessageMapper;
import com.portfolio.contact.repository.ContactMessageRepository;
import com.portfolio.contact.service.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Submission pipeline, in order: rate limit → honeypot → persist.
 *
 * <p>The order matters. The rate-limit check runs first and outside any transaction, so a flood
 * never reaches Postgres (docs/11-technical-design/backend-design.md). The honeypot runs second
 * and <em>consumes</em> an attempt, so a bot cannot spend its allowance for free.
 */
@Service
public class ContactServiceImpl implements ContactService {

	private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

	private static final String RATE_LIMIT_KEY_PREFIX = "contact:submit:";
	private static final int MAX_PAGE_SIZE = 100;

	private final ContactMessageRepository contactMessageRepository;
	private final ContactMessageMapper contactMessageMapper;
	private final RateLimiter rateLimiter;
	private final ContactProperties properties;

	public ContactServiceImpl(
			ContactMessageRepository contactMessageRepository,
			ContactMessageMapper contactMessageMapper,
			RateLimiter rateLimiter,
			ContactProperties properties) {
		this.contactMessageRepository = contactMessageRepository;
		this.contactMessageMapper = contactMessageMapper;
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	@Override
	public void submit(ContactMessageRequest request, String clientIp) {
		String key = RATE_LIMIT_KEY_PREFIX + clientIp;
		if (!rateLimiter.isWithinLimit(key, properties.rateLimit().maxAttempts())) {
			log.warn("Contact submission rate limit exceeded");
			throw new RateLimitExceededException("Too many messages sent. Please try again later.");
		}
		rateLimiter.recordAttempt(key, properties.rateLimit().window());

		if (isBot(request)) {
			// Silently accepted: the caller gets the same 201 a human gets, so a bot cannot
			// distinguish "delivered" from "dropped" and tune its way past the trap.
			log.info("Contact submission discarded: honeypot field was filled");
			return;
		}

		// No @Transactional on this method: the write is a single insert, which
		// SimpleJpaRepository.save already runs in its own transaction. Declaring one here would
		// also mean the rate-limit check and the honeypot rejection held a database connection
		// open for requests that never touch Postgres.
		ContactMessage saved = contactMessageRepository.save(new ContactMessage(
				request.name().trim(),
				request.email().trim(),
				request.subject() == null || request.subject().isBlank() ? null : request.subject().trim(),
				request.message()));
		log.info("Contact message received: id={} status=NEW", saved.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ContactMessageResponse> listForAdmin(ContactMessageStatus status, int page, int size) {
		// An inbox: newest first.
		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		Page<ContactMessage> found = status == null
				? contactMessageRepository.findAll(pageable)
				: contactMessageRepository.findByStatus(status, pageable);
		return PageResponse.from(found.map(contactMessageMapper::toResponse));
	}

	@Override
	@Transactional
	public ContactMessageResponse updateStatus(Long id, ContactMessageStatus status) {
		ContactMessage message = require(id);
		// Any transition is allowed, in both directions: NEW → READ → REPLIED is the expected
		// flow, but marking something back to NEW ("deal with this later") is a normal inbox
		// action, and no design phase asked for a one-way state machine.
		message.setStatus(status);
		return contactMessageMapper.toResponse(contactMessageRepository.save(message));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		contactMessageRepository.delete(require(id));
		log.info("Contact message deleted (soft): id={}", id);
	}

	private boolean isBot(ContactMessageRequest request) {
		return request.website() != null && !request.website().isBlank();
	}

	private ContactMessage require(Long id) {
		return contactMessageRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Contact message " + id + " not found"));
	}
}
