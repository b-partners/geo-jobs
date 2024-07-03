package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.ProcessJobAnnotation;
import app.bpartners.geojobs.endpoint.rest.validator.ProcessJobAnnotationValidator;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class JobControllerTest extends FacadeIT {
  @MockBean JobAnnotationService annotationServiceMock;
  @Autowired ProcessJobAnnotationValidator processJobAnnotationValidatorMock;
  @Autowired JobController subject;

  @Test
  void provide_bad_confidence_ko() {
    assertThrows(
        BadRequestException.class,
        () ->
            subject.processAnnotationJob(
                "jobId", new ProcessJobAnnotation().minimumConfidence(null)));
    assertThrows(
        BadRequestException.class,
        () ->
            subject.processAnnotationJob(
                "jobId", new ProcessJobAnnotation().minimumConfidence(1.01)));
    assertThrows(
        BadRequestException.class,
        () ->
            subject.processAnnotationJob(
                "jobId", new ProcessJobAnnotation().minimumConfidence(-0.01)));
  }
}
