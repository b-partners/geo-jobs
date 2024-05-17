package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.model.AnnotationJobProcessing;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class JobController {
  private final JobAnnotationService jobAnnotationService;

  @PostMapping("/jobs/{id}/annotationProcessing")
  public AnnotationJobProcessing processAnnotationJob(@PathVariable String id) {
    return jobAnnotationService.processAnnotationJob(id);
  }
}
