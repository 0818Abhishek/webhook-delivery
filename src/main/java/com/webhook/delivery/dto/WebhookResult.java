package com.webhook.delivery.dto;

public record WebhookResult(boolean success, int statusCode, String responseSnippet) {
}