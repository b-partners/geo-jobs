/*
 * Image Annotation API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: latest
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package app.bpartners.gen.annotator.endpoint.rest.api;

import app.bpartners.gen.annotator.endpoint.rest.OpenapiGenerated;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiClient;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiResponse;
import app.bpartners.gen.annotator.endpoint.rest.client.Pair;
import app.bpartners.gen.annotator.endpoint.rest.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

@OpenapiGenerated
public class JobsApi {
  private final HttpClient memberVarHttpClient;
  private final ObjectMapper memberVarObjectMapper;
  private final String memberVarBaseUri;
  private final Consumer<HttpRequest.Builder> memberVarInterceptor;
  private final Duration memberVarReadTimeout;
  private final Consumer<HttpResponse<InputStream>> memberVarResponseInterceptor;
  private final Consumer<HttpResponse<String>> memberVarAsyncResponseInterceptor;

  public JobsApi() {
    this(new ApiClient());
  }

  public JobsApi(ApiClient apiClient) {
    memberVarHttpClient = apiClient.getHttpClient();
    memberVarObjectMapper = apiClient.getObjectMapper();
    memberVarBaseUri = apiClient.getBaseUri();
    memberVarInterceptor = apiClient.getRequestInterceptor();
    memberVarReadTimeout = apiClient.getReadTimeout();
    memberVarResponseInterceptor = apiClient.getResponseInterceptor();
    memberVarAsyncResponseInterceptor = apiClient.getAsyncResponseInterceptor();
  }

  protected ApiException getApiException(String operationId, HttpResponse<InputStream> response)
      throws IOException {
    String body = response.body() == null ? null : new String(response.body().readAllBytes());
    String message = formatExceptionMessage(operationId, response.statusCode(), body);
    return new ApiException(response.statusCode(), message, response.headers(), body);
  }

  private String formatExceptionMessage(String operationId, int statusCode, String body) {
    if (body == null || body.isEmpty()) {
      body = "[no body]";
    }
    return operationId + " call failed with: " + statusCode + " - " + body;
  }

  /**
   * exports a job to COCO or VGG format
   *
   * @param jobId (required)
   * @param format (required)
   * @param emailCC one email address which will receive the exported job other than the ownerEmail,
   *     will default to none (optional)
   * @return String
   * @throws ApiException if fails to make API call
   */
  public String exportJob(String jobId, ExportFormat format, String emailCC) throws ApiException {
    ApiResponse<String> localVarResponse = exportJobWithHttpInfo(jobId, format, emailCC);
    return localVarResponse.getData();
  }

  /**
   * exports a job to COCO or VGG format
   *
   * @param jobId (required)
   * @param format (required)
   * @param emailCC one email address which will receive the exported job other than the ownerEmail,
   *     will default to none (optional)
   * @return ApiResponse&lt;String&gt;
   * @throws ApiException if fails to make API call
   */
  public ApiResponse<String> exportJobWithHttpInfo(
      String jobId, ExportFormat format, String emailCC) throws ApiException {
    HttpRequest.Builder localVarRequestBuilder = exportJobRequestBuilder(jobId, format, emailCC);
    try {
      HttpResponse<InputStream> localVarResponse =
          memberVarHttpClient.send(
              localVarRequestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
      if (memberVarResponseInterceptor != null) {
        memberVarResponseInterceptor.accept(localVarResponse);
      }
      try {
        if (localVarResponse.statusCode() / 100 != 2) {
          throw getApiException("exportJob", localVarResponse);
        }
        // for plain text response
        if (localVarResponse.headers().map().containsKey("Content-Type")
            && "text/plain"
                .equalsIgnoreCase(
                    localVarResponse
                        .headers()
                        .map()
                        .get("Content-Type")
                        .get(0)
                        .split(";")[0]
                        .trim())) {
          java.util.Scanner s = new java.util.Scanner(localVarResponse.body()).useDelimiter("\\A");
          String responseBodyText = s.hasNext() ? s.next() : "";
          return new ApiResponse<String>(
              localVarResponse.statusCode(), localVarResponse.headers().map(), responseBodyText);
        } else {
          throw new RuntimeException(
              "Error! The response Content-Type is supposed to be `text/plain` but it's not: "
                  + localVarResponse);
        }
      } finally {
      }
    } catch (IOException e) {
      throw new ApiException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(e);
    }
  }

  private HttpRequest.Builder exportJobRequestBuilder(
      String jobId, ExportFormat format, String emailCC) throws ApiException {
    // verify the required parameter 'jobId' is set
    if (jobId == null) {
      throw new ApiException(400, "Missing the required parameter 'jobId' when calling exportJob");
    }
    // verify the required parameter 'format' is set
    if (format == null) {
      throw new ApiException(400, "Missing the required parameter 'format' when calling exportJob");
    }

    HttpRequest.Builder localVarRequestBuilder = HttpRequest.newBuilder();

    String localVarPath =
        "/jobs/{jobId}/export".replace("{jobId}", ApiClient.urlEncode(jobId.toString()));

    List<Pair> localVarQueryParams = new ArrayList<>();
    StringJoiner localVarQueryStringJoiner = new StringJoiner("&");
    String localVarQueryParameterBaseName;
    localVarQueryParameterBaseName = "format";
    localVarQueryParams.addAll(ApiClient.parameterToPairs("format", format));
    localVarQueryParameterBaseName = "emailCC";
    localVarQueryParams.addAll(ApiClient.parameterToPairs("emailCC", emailCC));

    if (!localVarQueryParams.isEmpty() || localVarQueryStringJoiner.length() != 0) {
      StringJoiner queryJoiner = new StringJoiner("&");
      localVarQueryParams.forEach(p -> queryJoiner.add(p.getName() + '=' + p.getValue()));
      if (localVarQueryStringJoiner.length() != 0) {
        queryJoiner.add(localVarQueryStringJoiner.toString());
      }
      localVarRequestBuilder.uri(
          URI.create(memberVarBaseUri + localVarPath + '?' + queryJoiner.toString()));
    } else {
      localVarRequestBuilder.uri(URI.create(memberVarBaseUri + localVarPath));
    }

    localVarRequestBuilder.header("Accept", "text/plain");

    localVarRequestBuilder.method("GET", HttpRequest.BodyPublishers.noBody());
    if (memberVarReadTimeout != null) {
      localVarRequestBuilder.timeout(memberVarReadTimeout);
    }
    if (memberVarInterceptor != null) {
      memberVarInterceptor.accept(localVarRequestBuilder);
    }
    return localVarRequestBuilder;
  }

  /**
   * get a job specified by its id
   *
   * @param jobId (required)
   * @return Job
   * @throws ApiException if fails to make API call
   */
  public Job getJob(String jobId) throws ApiException {
    ApiResponse<Job> localVarResponse = getJobWithHttpInfo(jobId);
    return localVarResponse.getData();
  }

  /**
   * get a job specified by its id
   *
   * @param jobId (required)
   * @return ApiResponse&lt;Job&gt;
   * @throws ApiException if fails to make API call
   */
  public ApiResponse<Job> getJobWithHttpInfo(String jobId) throws ApiException {
    HttpRequest.Builder localVarRequestBuilder = getJobRequestBuilder(jobId);
    try {
      HttpResponse<InputStream> localVarResponse =
          memberVarHttpClient.send(
              localVarRequestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
      if (memberVarResponseInterceptor != null) {
        memberVarResponseInterceptor.accept(localVarResponse);
      }
      try {
        if (localVarResponse.statusCode() / 100 != 2) {
          throw getApiException("getJob", localVarResponse);
        }
        return new ApiResponse<Job>(
            localVarResponse.statusCode(),
            localVarResponse.headers().map(),
            localVarResponse.body() == null
                ? null
                : memberVarObjectMapper.readValue(
                    localVarResponse.body(), new TypeReference<Job>() {}) // closes the InputStream
            );
      } finally {
      }
    } catch (IOException e) {
      throw new ApiException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(e);
    }
  }

  private HttpRequest.Builder getJobRequestBuilder(String jobId) throws ApiException {
    // verify the required parameter 'jobId' is set
    if (jobId == null) {
      throw new ApiException(400, "Missing the required parameter 'jobId' when calling getJob");
    }

    HttpRequest.Builder localVarRequestBuilder = HttpRequest.newBuilder();

    String localVarPath = "/jobs/{jobId}".replace("{jobId}", ApiClient.urlEncode(jobId.toString()));

    localVarRequestBuilder.uri(URI.create(memberVarBaseUri + localVarPath));

    localVarRequestBuilder.header("Accept", "application/json");

    localVarRequestBuilder.method("GET", HttpRequest.BodyPublishers.noBody());
    if (memberVarReadTimeout != null) {
      localVarRequestBuilder.timeout(memberVarReadTimeout);
    }
    if (memberVarInterceptor != null) {
      memberVarInterceptor.accept(localVarRequestBuilder);
    }
    return localVarRequestBuilder;
  }

  /**
   * @param jobId (required)
   * @return List&lt;AnnotationNumberPerLabel&gt;
   * @throws ApiException if fails to make API call
   */
  public List<AnnotationNumberPerLabel> getJobLatestAnnotationStatistics(String jobId)
      throws ApiException {
    ApiResponse<List<AnnotationNumberPerLabel>> localVarResponse =
        getJobLatestAnnotationStatisticsWithHttpInfo(jobId);
    return localVarResponse.getData();
  }

  /**
   * @param jobId (required)
   * @return ApiResponse&lt;List&lt;AnnotationNumberPerLabel&gt;&gt;
   * @throws ApiException if fails to make API call
   */
  public ApiResponse<List<AnnotationNumberPerLabel>> getJobLatestAnnotationStatisticsWithHttpInfo(
      String jobId) throws ApiException {
    HttpRequest.Builder localVarRequestBuilder =
        getJobLatestAnnotationStatisticsRequestBuilder(jobId);
    try {
      HttpResponse<InputStream> localVarResponse =
          memberVarHttpClient.send(
              localVarRequestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
      if (memberVarResponseInterceptor != null) {
        memberVarResponseInterceptor.accept(localVarResponse);
      }
      try {
        if (localVarResponse.statusCode() / 100 != 2) {
          throw getApiException("getJobLatestAnnotationStatistics", localVarResponse);
        }
        return new ApiResponse<List<AnnotationNumberPerLabel>>(
            localVarResponse.statusCode(),
            localVarResponse.headers().map(),
            localVarResponse.body() == null
                ? null
                : memberVarObjectMapper.readValue(
                    localVarResponse.body(),
                    new TypeReference<
                        List<AnnotationNumberPerLabel>>() {}) // closes the InputStream
            );
      } finally {
      }
    } catch (IOException e) {
      throw new ApiException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(e);
    }
  }

  private HttpRequest.Builder getJobLatestAnnotationStatisticsRequestBuilder(String jobId)
      throws ApiException {
    // verify the required parameter 'jobId' is set
    if (jobId == null) {
      throw new ApiException(
          400,
          "Missing the required parameter 'jobId' when calling getJobLatestAnnotationStatistics");
    }

    HttpRequest.Builder localVarRequestBuilder = HttpRequest.newBuilder();

    String localVarPath =
        "/jobs/{jobId}/annotationStatistics"
            .replace("{jobId}", ApiClient.urlEncode(jobId.toString()));

    localVarRequestBuilder.uri(URI.create(memberVarBaseUri + localVarPath));

    localVarRequestBuilder.header("Accept", "application/json");

    localVarRequestBuilder.method("GET", HttpRequest.BodyPublishers.noBody());
    if (memberVarReadTimeout != null) {
      localVarRequestBuilder.timeout(memberVarReadTimeout);
    }
    if (memberVarInterceptor != null) {
      memberVarInterceptor.accept(localVarRequestBuilder);
    }
    return localVarRequestBuilder;
  }

  /**
   * Get all jobs
   *
   * @param page (optional)
   * @param pageSize (optional)
   * @param status (optional)
   * @param name filters jobs by name (optional)
   * @param type filters by job type (optional)
   * @return List&lt;Job&gt;
   * @throws ApiException if fails to make API call
   */
  public List<Job> getJobs(
      Integer page, Integer pageSize, JobStatus status, String name, JobType type)
      throws ApiException {
    ApiResponse<List<Job>> localVarResponse =
        getJobsWithHttpInfo(page, pageSize, status, name, type);
    return localVarResponse.getData();
  }

  /**
   * Get all jobs
   *
   * @param page (optional)
   * @param pageSize (optional)
   * @param status (optional)
   * @param name filters jobs by name (optional)
   * @param type filters by job type (optional)
   * @return ApiResponse&lt;List&lt;Job&gt;&gt;
   * @throws ApiException if fails to make API call
   */
  public ApiResponse<List<Job>> getJobsWithHttpInfo(
      Integer page, Integer pageSize, JobStatus status, String name, JobType type)
      throws ApiException {
    HttpRequest.Builder localVarRequestBuilder =
        getJobsRequestBuilder(page, pageSize, status, name, type);
    try {
      HttpResponse<InputStream> localVarResponse =
          memberVarHttpClient.send(
              localVarRequestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
      if (memberVarResponseInterceptor != null) {
        memberVarResponseInterceptor.accept(localVarResponse);
      }
      try {
        if (localVarResponse.statusCode() / 100 != 2) {
          throw getApiException("getJobs", localVarResponse);
        }
        return new ApiResponse<List<Job>>(
            localVarResponse.statusCode(),
            localVarResponse.headers().map(),
            localVarResponse.body() == null
                ? null
                : memberVarObjectMapper.readValue(
                    localVarResponse.body(),
                    new TypeReference<List<Job>>() {}) // closes the InputStream
            );
      } finally {
      }
    } catch (IOException e) {
      throw new ApiException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(e);
    }
  }

  private HttpRequest.Builder getJobsRequestBuilder(
      Integer page, Integer pageSize, JobStatus status, String name, JobType type)
      throws ApiException {

    HttpRequest.Builder localVarRequestBuilder = HttpRequest.newBuilder();

    String localVarPath = "/jobs";

    List<Pair> localVarQueryParams = new ArrayList<>();
    StringJoiner localVarQueryStringJoiner = new StringJoiner("&");
    String localVarQueryParameterBaseName;
    localVarQueryParameterBaseName = "page";
    localVarQueryParams.addAll(ApiClient.parameterToPairs("page", page));
    localVarQueryParameterBaseName = "pageSize";
    localVarQueryParams.addAll(ApiClient.parameterToPairs("pageSize", pageSize));
    localVarQueryParameterBaseName = "status";
    localVarQueryParams.addAll(ApiClient.parameterToPairs("status", status));
    localVarQueryParameterBaseName = "name";
    localVarQueryParams.addAll(ApiClient.parameterToPairs("name", name));
    localVarQueryParameterBaseName = "type";
    localVarQueryParams.addAll(ApiClient.parameterToPairs("type", type));

    if (!localVarQueryParams.isEmpty() || localVarQueryStringJoiner.length() != 0) {
      StringJoiner queryJoiner = new StringJoiner("&");
      localVarQueryParams.forEach(p -> queryJoiner.add(p.getName() + '=' + p.getValue()));
      if (localVarQueryStringJoiner.length() != 0) {
        queryJoiner.add(localVarQueryStringJoiner.toString());
      }
      localVarRequestBuilder.uri(
          URI.create(memberVarBaseUri + localVarPath + '?' + queryJoiner.toString()));
    } else {
      localVarRequestBuilder.uri(URI.create(memberVarBaseUri + localVarPath));
    }

    localVarRequestBuilder.header("Accept", "application/json");

    localVarRequestBuilder.method("GET", HttpRequest.BodyPublishers.noBody());
    if (memberVarReadTimeout != null) {
      localVarRequestBuilder.timeout(memberVarReadTimeout);
    }
    if (memberVarInterceptor != null) {
      memberVarInterceptor.accept(localVarRequestBuilder);
    }
    return localVarRequestBuilder;
  }

  /**
   * Create or update a job
   *
   * @param jobId (required)
   * @param crupdateJob (optional)
   * @return Job
   * @throws ApiException if fails to make API call
   */
  public Job saveJob(String jobId, CrupdateJob crupdateJob) throws ApiException {
    ApiResponse<Job> localVarResponse = saveJobWithHttpInfo(jobId, crupdateJob);
    return localVarResponse.getData();
  }

  /**
   * Create or update a job
   *
   * @param jobId (required)
   * @param crupdateJob (optional)
   * @return ApiResponse&lt;Job&gt;
   * @throws ApiException if fails to make API call
   */
  public ApiResponse<Job> saveJobWithHttpInfo(String jobId, CrupdateJob crupdateJob)
      throws ApiException {
    HttpRequest.Builder localVarRequestBuilder = saveJobRequestBuilder(jobId, crupdateJob);
    try {
      HttpResponse<InputStream> localVarResponse =
          memberVarHttpClient.send(
              localVarRequestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
      if (memberVarResponseInterceptor != null) {
        memberVarResponseInterceptor.accept(localVarResponse);
      }
      try {
        if (localVarResponse.statusCode() / 100 != 2) {
          throw getApiException("saveJob", localVarResponse);
        }
        return new ApiResponse<Job>(
            localVarResponse.statusCode(),
            localVarResponse.headers().map(),
            localVarResponse.body() == null
                ? null
                : memberVarObjectMapper.readValue(
                    localVarResponse.body(), new TypeReference<Job>() {}) // closes the InputStream
            );
      } finally {
      }
    } catch (IOException e) {
      throw new ApiException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(e);
    }
  }

  private HttpRequest.Builder saveJobRequestBuilder(String jobId, CrupdateJob crupdateJob)
      throws ApiException {
    // verify the required parameter 'jobId' is set
    if (jobId == null) {
      throw new ApiException(400, "Missing the required parameter 'jobId' when calling saveJob");
    }

    HttpRequest.Builder localVarRequestBuilder = HttpRequest.newBuilder();

    String localVarPath = "/jobs/{jobId}".replace("{jobId}", ApiClient.urlEncode(jobId.toString()));

    localVarRequestBuilder.uri(URI.create(memberVarBaseUri + localVarPath));

    localVarRequestBuilder.header("Content-Type", "application/json");
    localVarRequestBuilder.header("Accept", "application/json");

    try {
      byte[] localVarPostBody = memberVarObjectMapper.writeValueAsBytes(crupdateJob);
      localVarRequestBuilder.method(
          "PUT", HttpRequest.BodyPublishers.ofByteArray(localVarPostBody));
    } catch (IOException e) {
      throw new ApiException(e);
    }
    if (memberVarReadTimeout != null) {
      localVarRequestBuilder.timeout(memberVarReadTimeout);
    }
    if (memberVarInterceptor != null) {
      memberVarInterceptor.accept(localVarRequestBuilder);
    }
    return localVarRequestBuilder;
  }
}
