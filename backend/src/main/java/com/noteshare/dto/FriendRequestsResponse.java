package com.noteshare.dto;

import java.util.List;

public record FriendRequestsResponse(List<String> incoming, List<String> outgoing) {}
