package com.portfolio.contact.service;

import com.portfolio.common.response.PageResponse;
import com.portfolio.contact.dto.ContactMessageRequest;
import com.portfolio.contact.dto.ContactMessageResponse;
import com.portfolio.contact.entity.ContactMessageStatus;

public interface ContactService {

	/**
	 * Accepts a visitor's submission.
	 *
	 * <p>Returns nothing on purpose: the caller is anonymous and gets the same acknowledgement
	 * whether the message was stored or silently dropped as spam.
	 *
	 * @param clientIp the address the per-IP allowance is counted against
	 */
	void submit(ContactMessageRequest request, String clientIp);

	PageResponse<ContactMessageResponse> listForAdmin(ContactMessageStatus status, int page, int size);

	ContactMessageResponse updateStatus(Long id, ContactMessageStatus status);

	/** Soft delete. */
	void delete(Long id);
}
