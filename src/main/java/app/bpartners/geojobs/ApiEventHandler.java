package app.bpartners.geojobs;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@PojaGenerated
@SuppressWarnings("all")
@Slf4j
public class ApiEventHandler implements RequestStreamHandler {
  private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse>
      handler;

  static {
    try {
      handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(PojaApplication.class);
    } catch (ContainerInitializationException e) {
      throw new RuntimeException("Initialization of Spring Boot Application failed", e);
    }
  }

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
      throws IOException {
    log.info("Input={}, Output={}, Context={}", input,output, context);
    handler.proxyStream(input, output, context);
  }
}
