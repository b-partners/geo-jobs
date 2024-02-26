package app.bpartners.geojobs.service.tiling.downloader;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class IsPreprodEnvCondition implements Condition{

    public static final String ENV_PROPERTY = "env";
    private static final String PREPROD_ENV_VALUE = "preprod";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String envValue = env.getProperty(ENV_PROPERTY);
        return PREPROD_ENV_VALUE.equalsIgnoreCase(envValue);    }
}
