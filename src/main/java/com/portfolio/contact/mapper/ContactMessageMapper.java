package com.portfolio.contact.mapper;

import com.portfolio.contact.dto.ContactMessageResponse;
import com.portfolio.contact.entity.ContactMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContactMessageMapper {

	ContactMessageResponse toResponse(ContactMessage message);
}
