package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.service.RevokedApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApiKeyController {
    private final RevokedApiKeyService service;
    private final AuthProvider authProvider;
    private final CommunityAuthorizationRepository communityAuthRepository;

    @DeleteMapping("/api/keys/revoke")
    public void revokeApikey(){
        var communityAuthorization =
            communityAuthRepository.findByApiKey(authProvider.getPrincipal().getPassword());
        service.revokeCommunityApiKey(communityAuthorization.orElseThrow(ForbiddenException::new));
    }
}
