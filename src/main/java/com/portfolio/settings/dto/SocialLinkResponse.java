package com.portfolio.settings.dto;

public record SocialLinkResponse(Long id, String platform, String url, int displayOrder, boolean visible) {
}
