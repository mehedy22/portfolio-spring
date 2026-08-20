package com.portfolio.analytics.entity;

/**
 * Coarse device bucket, mirroring {@code ck_page_view_device_type}.
 *
 * <p>Three buckets and an unknown is as much as can be derived without retaining something
 * identifying — which is the point: the raw User-Agent is a fingerprint and is never stored.
 */
public enum DeviceType {
	DESKTOP,
	MOBILE,
	TABLET,
	UNKNOWN
}
