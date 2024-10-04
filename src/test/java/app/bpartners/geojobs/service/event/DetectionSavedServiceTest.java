package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.BPToitureModel.ModelNameEnum.BP_TOITURE;
import static app.bpartners.geojobs.service.event.DetectionSavedService.DETECTION_SAVED_TEMPLATE;
import static app.bpartners.geojobs.service.event.DetectionSavedService.computeStaticEmailBody;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.rest.model.BPToitureModel;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectModelStringMapValue;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.detection.DetectableObjectModelMapper;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

class DetectionSavedServiceTest {
  HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
  BucketComponent bucketComponentMock = mock();
  Mailer mailerMock = mock();
  DetectableObjectModelMapper detectableObjectModelMapper = new DetectableObjectModelMapper();
  DetectionSavedService subject =
      new DetectionSavedService(mailerMock, bucketComponentMock, detectableObjectModelMapper);

  @SneakyThrows
  @Test
  void accept_ok() {
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI("http://localhost").toURL());
    var shapeFileKey = "dummy";
    var detection =
        Detection.builder()
            .shapeFileKey(shapeFileKey)
            .bpToitureModel(new BPToitureModel().modelName(BP_TOITURE))
            .build();
    List<InternetAddress> cc = List.of();
    List<InternetAddress> bcc = List.of();
    String htmlBody =
        computeStaticEmailBody(detection, bucketComponentMock, detectableObjectModelMapper);
    List<File> attachments = List.of();

    subject.accept(DetectionSaved.builder().detection(detection).build());

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    var durationCaptor = ArgumentCaptor.forClass(Duration.class);
    verify(mailerMock, only()).accept(emailCaptor.capture());
    verify(bucketComponentMock, times(2)).presign(eq(shapeFileKey), durationCaptor.capture());
    var urlDurationValue = durationCaptor.getValue();
    var actualEmail = emailCaptor.getValue();
    var expectedMail =
        new Email(
            new InternetAddress("tech@bpartners.app"),
            cc,
            bcc,
            actualEmail.subject(),
            htmlBody,
            attachments);
    assertEquals(expectedMail, actualEmail);
    assertEquals(Duration.ofHours(24L), urlDurationValue);
  }

  @Test
  void parse_detection_saved_email_body() {
    HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
    Context context = new Context();
    BPToitureModel modelInstance = new BPToitureModel().modelName(BP_TOITURE);
    List<DetectableObjectModelStringMapValue> detectableObjectModelStringMapValues =
        detectableObjectModelMapper.apply(modelInstance);
    context.setVariable(
        "detectableObjectModelStringMapValues", detectableObjectModelStringMapValues);
    context.setVariable("detection", new Detection());
    context.setVariable("shapeFileUrl", null);
    context.setVariable("excelFileUrl", null);

    var actual = htmlTemplateParser.apply(DETECTION_SAVED_TEMPLATE, context);

    var expected = expectedEmailBody();
    assertEquals(expected, actual);
  }

  private String expectedEmailBody() {
    return """
           <html lang="fr">
           <head>
               <title>Detection saved</title>
               <style>
                   body {
                       font-family: Arial, sans-serif;
                       background-color: #F1E4E7;
                       color: #582d37;
                       margin: 0;
                       padding: 20px;
                   }

                   section {
                       background-color: white;
                       border-radius: 8px;
                       box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                       padding: 20px;
                       max-width: 600px;
                       margin: auto;
                       border-left: 6px solid rgba(171, 0, 86, 0.2);
                   }

                   p {
                       font-size: 16px;
                       line-height: 1.6;
                       color: #660033;
                   }

                   ul {
                       list-style-type: none;
                       padding: 0;
                   }

                   ul li {
                       background-color: rgba(0, 0, 0, 0.05);
                       margin: 10px 0;
                       padding: 10px;
                       border-left: 4px solid rgba(171, 0, 86, 0.5);
                       border-radius: 4px;
                   }

                   ul li span {
                       font-weight: bold;
                       color: rgba(122, 0, 61, 0.7);
                   }

                   ul li:not(:last-child) {
                       margin-bottom: 10px;
                   }

                   h1 {
                       font-size: 24px;
                       color: rgba(171, 0, 86, 0.7);
                       text-align: center;
                   }
               </style>
           </head>
           <body>
           <section>
               <h1>Informations de Détection</h1>
               <p>Bonjour,</p>
               <p>Voici les éléments fournis par le consommateur d'API  :</p>
               <ul>
                   <li>Email associé : <span></span></li>
                   <li>Nom de zone fournie par le consommateur : <span></span></li>
                   <li>Configurations de la détection :
                       <ul>
                           <li>
                               <span class="indent">modelName</span> :
                               <span class="indent">BP_TOITURE</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">toitureRevetement</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">arbre</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">velux</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">panneauPhotovoltaique</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">moisissure</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">usure</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">fissureCassure</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">obstacle</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">cheminee</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">humidite</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul><ul>
                           <li>
                               <span class="indent">risqueFeu</span> :
                               <span class="indent">oui</span>
                           </li>
                       </ul>
                   </li>
                   <li>Configuration du geoServeur : <br><span></span></li>
                   <li>Zone en geoJson fournie : <br>
                      \s
                   </li>
                   <li>Fichier shape à convertir en geoJson : <br>
                      \s
                   </li>
                   <li>Fichier excel à convertir en geoJson : <br>
                      \s
                   </li>
               </ul>
               <p>Cordialement,</p>
           </section>
           </body>
           </html>
           """;
  }
}
