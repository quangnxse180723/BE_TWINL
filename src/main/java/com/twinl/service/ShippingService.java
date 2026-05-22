package com.twinl.service;

import com.twinl.dto.request.GhnCreateShipmentRequest;
import com.twinl.dto.request.GhnWebhookRequest;
import com.twinl.dto.response.GhnWebhookResponse;
import com.twinl.dto.response.ShipmentResponse;

public interface ShippingService {
	ShipmentResponse createGhnShipment(Long orderId, GhnCreateShipmentRequest request);
	GhnWebhookResponse handleGhnWebhook(GhnWebhookRequest request, String tokenHeader);
}
