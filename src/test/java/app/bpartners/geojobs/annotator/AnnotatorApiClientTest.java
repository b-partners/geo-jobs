package app.bpartners.geojobs.annotator;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.annotator.AnnotatorApiClient;
import app.bpartners.geojobs.repository.annotator.gen.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class AnnotatorApiClientTest {
  AnnotatorApiClient subject;
  HttpClient httpClientMock;
  private static final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void setUp() {
    httpClientMock = mock(HttpClient.class);
    subject = new AnnotatorApiClient("https://dummy.com", httpClientMock);
  }

  private static Job job1() {
    return Job.builder()
        .id("job1_id")
        .bucketName("dummyBucket")
        .type(JobType.REVIEWING)
        .status(JobStatus.PENDING)
        .folderPath("dummyFolderPath")
        .labels(List.of(label1()))
        .build();
  }

  private static Label label1() {
    return Label.builder().id("label1_id").name("ROOF").color("#fff").build();
  }

  private static Task task1() {
    return Task.builder()
        .id("task1_id")
        .filename("task_filename1")
        .userId("user1_id")
        .imageUri("image1_uri")
        .status(TaskStatus.PENDING)
        .build();
  }

  private static Polygon polygon1() {
    return Polygon.builder().points(List.of(Point.builder().x(12.0).y(34.0).build())).build();
  }

  private static AnnotationBatch annotationBatch1() {
    return AnnotationBatch.builder()
        .id("annotation_batch1_id")
        .annotations(List.of(annotation1()))
        .creationDatetime(now())
        .build();
  }

  private static Annotation annotation1() {
    return Annotation.builder()
        .id("annotation1_id")
        .userId("user1_id")
        .taskId(task1().getId())
        .label(label1())
        .polygon(polygon1())
        .build();
  }

  @Test
  void crupdate_annotated_job_ok() throws URISyntaxException, IOException, InterruptedException {
    Job expected = job1();
    String jobId = expected.getId();
    CrupdateAnnotatedJob crupdateAnnotatedJob = CrupdateAnnotatedJob.builder().id(jobId).build();
    String urlString = subject.getAnnotatorApiUrl() + "/annotated-jobs/" + jobId;
    when(httpClientMock.send(
            HttpRequest.newBuilder()
                .uri(new URI(urlString))
                .PUT(
                    HttpRequest.BodyPublishers.ofString(
                        om.writeValueAsString(crupdateAnnotatedJob)))
                .build(),
            HttpResponse.BodyHandlers.ofString()))
        .thenReturn(httpResponseMock(200, expected));

    assertEquals(expected, subject.crupdateAnnotatedJob(crupdateAnnotatedJob));
  }

  @Test
  void get_job_by_id_ok() throws URISyntaxException, IOException, InterruptedException {
    Job expected = job1();
    String jobId = expected.getId();
    when(httpClientMock.send(
            HttpRequest.newBuilder()
                .uri(new URI(subject.getAnnotatorApiUrl() + "/jobs/" + jobId))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()))
        .thenReturn(httpResponseMock(200, expected));

    assertEquals(expected, subject.getJobById(jobId));
  }

  @Test
  void get_tasks_by_job_id_ok() throws URISyntaxException, IOException, InterruptedException {
    List<Task> expected = List.of(task1());
    String jobId = job1().getId();
    when(httpClientMock.send(
            HttpRequest.newBuilder()
                .uri(new URI(subject.getAnnotatorApiUrl() + "/jobs/" + jobId + "/tasks"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()))
        .thenReturn(httpResponseMock(200, expected));

    assertEquals(expected, subject.getTasksByJobId(jobId, null, null, null, null));
  }

  @Test
  void get_annotation_batches_by_job_and_task_id_ok()
      throws URISyntaxException, IOException, InterruptedException {
    List<AnnotationBatch> expected = List.of(annotationBatch1());
    String jobId = job1().getId();
    String taskId = task1().getId();
    String url = subject.getAnnotatorApiUrl() + "/jobs/" + jobId + "/tasks/" + taskId;
    when(httpClientMock.send(
            HttpRequest.newBuilder().uri(new URI(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString()))
        .thenReturn(httpResponseMock(200, expected));

    assertEquals(expected, subject.getAnnotationBatches(jobId, taskId, null, null));
  }

  public static HttpResponse<String> httpResponseMock(Integer statusCode, Object body) {
    return new HttpResponse<>() {
      @Override
      public int statusCode() {
        return statusCode;
      }

      @Override
      public HttpRequest request() {
        return null;
      }

      @Override
      public Optional<HttpResponse<String>> previousResponse() {
        return Optional.empty();
      }

      @Override
      public HttpHeaders headers() {
        return null;
      }

      @Override
      public String body() {
        if (body.getClass() == String.class) {
          return body.toString();
        }
        try {
          return om.writeValueAsString(body);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Optional<SSLSession> sslSession() {
        return Optional.empty();
      }

      @Override
      public URI uri() {
        return null;
      }

      @Override
      public HttpClient.Version version() {
        return null;
      }
    };
  }
}
