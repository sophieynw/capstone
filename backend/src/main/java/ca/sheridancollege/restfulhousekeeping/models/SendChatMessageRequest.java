package ca.sheridancollege.restfulhousekeeping.models;

public record SendChatMessageRequest(
	    Long conversationId,
	    String body
	) {}
