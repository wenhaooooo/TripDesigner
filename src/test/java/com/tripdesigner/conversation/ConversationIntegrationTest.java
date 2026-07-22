package com.tripdesigner.conversation;

import com.tripdesigner.auth.application.AuthAppService;
import com.tripdesigner.common.security.JwtUtil;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.conversation.api.dto.AddMessageRequest;
import com.tripdesigner.conversation.api.dto.CreateConversationRequest;
import com.tripdesigner.conversation.api.vo.ConversationVo;
import com.tripdesigner.conversation.application.ConversationAppService;
import com.tripdesigner.conversation.domain.ConversationRole;
import com.tripdesigner.support.IntegrationTest;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConversationIntegrationTest extends IntegrationTest {

    private final ConversationAppService convService;
    private final AuthAppService authService;
    private final JwtUtil jwt;

    protected ConversationIntegrationTest(ConversationAppService convService,
                                           AuthAppService authService,
                                           JwtUtil jwt) {
        this.convService = convService;
        this.authService = authService;
        this.jwt = jwt;
    }

    @Test
    void fullConversationCrud() {
        // 1. Register & login
        var token = authService.register("conv-test@example.com", "password123");
        var claims = jwt.parse(token.getAccessToken());
        UserContextHolder.set(new UserContext(claims.get("uid", Long.class), claims.getSubject()));

        try {
            // 2. Create conversation
            CreateConversationRequest createReq = new CreateConversationRequest();
            createReq.setTitle("My Trip Planning");
            ConversationVo created = convService.create(createReq);
            assertNotNull(created.getId());
            assertEquals("My Trip Planning", created.getTitle());
            assertEquals("ACTIVE", created.getStatus());

            // 3. List conversations
            List<ConversationVo> list = convService.list();
            assertTrue(list.stream().anyMatch(c -> c.getId().equals(created.getId())));

            // 4. Get conversation
            ConversationVo got = convService.get(created.getId());
            assertEquals("My Trip Planning", got.getTitle());

            // 5. Add messages
            AddMessageRequest msgReq = new AddMessageRequest();
            msgReq.setRole(ConversationRole.USER);
            msgReq.setContent("Plan a 5-day trip to Tokyo");
            var msg = convService.addMessage(created.getId(), msgReq);
            assertNotNull(msg.getId());
            assertEquals("USER", msg.getRole());
            assertEquals("Plan a 5-day trip to Tokyo", msg.getContent());

            // 6. List messages
            var messages = convService.listMessages(created.getId());
            assertEquals(1, messages.size());

            // 7. Update conversation title
            CreateConversationRequest updateReq = new CreateConversationRequest();
            updateReq.setTitle("Updated Title");
            var updated = convService.update(created.getId(), updateReq);
            assertEquals("Updated Title", updated.getTitle());

            // 8. Delete conversation
            convService.delete(created.getId());
            assertThrows(Exception.class, () -> convService.get(created.getId()));
        } finally {
            UserContextHolder.clear();
        }
    }
}
