package com.portfolio.contact.controller;

import com.portfolio.common.response.ApiResponse;
import com.portfolio.common.response.PageResponse;
import com.portfolio.common.web.ClientIp;
import com.portfolio.contact.dto.ContactMessageRequest;
import com.portfolio.contact.dto.ContactMessageResponse;
import com.portfolio.contact.dto.ContactMessageStatusRequest;
import com.portfolio.contact.entity.ContactMessageStatus;
import com.portfolio.contact.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contact endpoints per docs/07-api/endpoints.md. Submitting is public; the inbox is not. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Contact", description = "Visitor messages (FR-09, FR-10)")
public class ContactController {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final ContactService contactService;

	public ContactController(ContactService contactService) {
		this.contactService = contactService;
	}

	@PostMapping("/contact")
	@Operation(
			summary = "Send a message",
			description = "Anonymous. Rate-limited per IP (429 on abuse). The response is identical "
					+ "whether the message was stored or dropped as spam.")
	public ResponseEntity<ApiResponse<Void>> submit(
			@Valid @RequestBody ContactMessageRequest request, HttpServletRequest httpRequest) {

		contactService.submit(request, ClientIp.of(httpRequest));
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(null, "Message received"));
	}

	@GetMapping("/admin/contact-messages")
	@Operation(summary = "Inbox", description = "Newest first, paginated, optional ?status= filter.")
	public ApiResponse<PageResponse<ContactMessageResponse>> list(
			@RequestParam(required = false) ContactMessageStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

		return ApiResponse.of(contactService.listForAdmin(status, page, size));
	}

	@PatchMapping("/admin/contact-messages/{id}/status")
	@Operation(summary = "Mark a message", description = "NEW | READ | REPLIED, in either direction.")
	public ApiResponse<ContactMessageResponse> updateStatus(
			@PathVariable Long id, @Valid @RequestBody ContactMessageStatusRequest request) {
		return ApiResponse.of(contactService.updateStatus(id, request.status()), "Status updated");
	}

	@DeleteMapping("/admin/contact-messages/{id}")
	@Operation(summary = "Delete a message", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		contactService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
