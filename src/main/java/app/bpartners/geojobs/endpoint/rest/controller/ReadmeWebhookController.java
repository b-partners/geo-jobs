package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.readme.webhook.ReadmeWebhookConf;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.ReadmeWebhookService;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.ReadmeWebhookValidator;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.CreateWebhook;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.SingleUserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReadmeWebhookController {
  private final ReadmeWebhookValidator readmeWebhookValidator;
  private final ReadmeWebhookConf readmeWebhookConf;
  private final ReadmeWebhookService service;

  @PostMapping("/readme/webhook/apiKey")
  public SingleUserInfo readmeWebhook(@RequestBody CreateWebhook body, HttpServletRequest request) {
    readmeWebhookValidator.accept(body, request, readmeWebhookConf);
    return service.retrieveUserInfoByEmail(body.email());
  }
}
