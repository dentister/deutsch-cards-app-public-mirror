package com.kniazev.cards.word.ui.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

/**
 * Vaadin's development mode takes the project folder from the first {@code target/classes} on the classpath. That is
 * the application module, while the frontend files and the {@code myapp} theme live in this one. When the application
 * runs from exploded classes (an IDE, {@code spring-boot:run}) and nobody has set {@value #PROPERTY}, it is pointed at
 * this module's folder. Inside the executable JAR this does nothing.
 */
public class VaadinProjectFolder implements EnvironmentPostProcessor {

    static final String PROPERTY = "vaadin.project.basedir";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (System.getProperty(PROPERTY) != null) {
            return;
        }

        File moduleFolder = moduleFolder(AppShell.class);

        if (moduleFolder != null) {
            System.setProperty(PROPERTY, moduleFolder.getAbsolutePath());
        }
    }

    /** The folder of the module the class was loaded from, or null when it comes from a JAR. */
    static File moduleFolder(Class<?> type) {
        CodeSource source = type.getProtectionDomain().getCodeSource();

        if (source == null) {
            return null;
        }

        try {
            File classes = new File(source.getLocation().toURI());
            File target = classes.getParentFile();

            if (classes.isDirectory() && "classes".equals(classes.getName()) && target != null
                    && "target".equals(target.getName())) {
                return target.getParentFile();
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            // not a plain file location: leave Vaadin to its own lookup
        }

        return null;
    }
}
