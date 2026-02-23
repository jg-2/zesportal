package pl.pekao.zesportal.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.pekao.zesportal.entity.Environment;
import pl.pekao.zesportal.entity.SampleEntity;
import pl.pekao.zesportal.service.EnvironmentService;
import pl.pekao.zesportal.service.SampleEntityService;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SampleEntityService sampleEntityService;
    private final EnvironmentService environmentService;

    public DataInitializer(SampleEntityService sampleEntityService, EnvironmentService environmentService) {
        this.sampleEntityService = sampleEntityService;
        this.environmentService = environmentService;
    }

    @Override
    public void run(String... args) {
        initializeSampleEntities();
        initializeEnvironments();
    }

    private void initializeSampleEntities() {
        if (sampleEntityService.findAll().isEmpty()) {
            SampleEntity e1 = new SampleEntity();
            e1.setName("Example 1");
            e1.setDescription("First entry in H2 database");
            sampleEntityService.save(e1);

            SampleEntity e2 = new SampleEntity();
            e2.setName("Example 2");
            e2.setDescription("Second entry");
            sampleEntityService.save(e2);
        }
    }

    private void initializeEnvironments() {
        if (environmentService.findAll().isEmpty()) {
            Environment e1 = new Environment();
            e1.setCode("DEV");
            e1.setName("Development Environment");
            e1.setDescription("Environment for testing and application development");
            e1.setStatus(Environment.EnvironmentStatus.ACTIVE);
            environmentService.save(e1);

            Environment e2 = new Environment();
            e2.setCode("TEST");
            e2.setName("Test Environment");
            e2.setDescription("Environment for integration and acceptance tests");
            e2.setStatus(Environment.EnvironmentStatus.ACTIVE);
            environmentService.save(e2);

            Environment e3 = new Environment();
            e3.setCode("PROD");
            e3.setName("Production Environment");
            e3.setDescription("Production environment for end users");
            e3.setStatus(Environment.EnvironmentStatus.ACTIVE);
            environmentService.save(e3);

            Environment e4 = new Environment();
            e4.setCode("ARCHIVED");
            e4.setName("Archived Environment");
            e4.setDescription("Inactive archived environment");
            e4.setStatus(Environment.EnvironmentStatus.INACTIVE);
            environmentService.save(e4);
        }
    }
}
