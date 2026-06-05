package com.plateapp.plate_main.friend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.friend.dto.FriendDto;
import com.plateapp.plate_main.friend.dto.FriendRequests.CreateFriendRequest;
import com.plateapp.plate_main.friend.service.FriendService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class FriendControllerSecurityTest {

    @Mock
    private FriendService friendService;

    @Test
    void listUsesAuthenticatedUsernameWhenUsernameParamIsMissing() {
        FriendController controller = new FriendController(friendService);
        when(friendService.list("me", null)).thenReturn(List.of());

        controller.list(null, null, auth("me"));

        verify(friendService).list("me", null);
    }

    @Test
    void listRejectsAnotherUsername() {
        FriendController controller = new FriendController(friendService);

        assertThatThrownBy(() -> controller.list("other-user", null, auth("me")))
                .isInstanceOf(AppException.class)
                .satisfies(error ->
                        assertThat(((AppException) error).getErrorCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN));
    }

    @Test
    void addUsesAuthenticatedUsernameInsteadOfRequestUsername() {
        FriendController controller = new FriendController(friendService);
        CreateFriendRequest request = new CreateFriendRequest("me", "friend", "accepted", "me", "hello");
        FriendDto response = FriendDto.builder().username("me").friendName("friend").status("pending").build();
        when(friendService.add("me", request)).thenReturn(response);

        controller.add(request, auth("me"));

        verify(friendService).add("me", request);
    }

    private Authentication auth(String username) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(username, null);
        authentication.setAuthenticated(true);
        return authentication;
    }
}
