package com.kniazev.cards.word.ui.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;

class VaadinProjectFolderTests {

    @Test
    void theFolderOfTheModuleIsFoundWhenRunningFromExplodedClasses() {
        File folder = VaadinProjectFolder.moduleFolder(AppShell.class);

        assertThat(folder).isNotNull();
        assertThat(new File(folder, "src/main/frontend/themes/myapp")).isDirectory();
    }

    @Test
    void aClassFromAJarHasNoModuleFolder() {
        assertThat(VaadinProjectFolder.moduleFolder(org.junit.jupiter.api.Test.class)).isNull();
    }

    @Test
    void thePropertyIsSetOnlyWhenNobodySetItBefore() {
        System.clearProperty(VaadinProjectFolder.PROPERTY);

        try {
            new VaadinProjectFolder().postProcessEnvironment(new MockEnvironment(), new SpringApplication());
            String found = System.getProperty(VaadinProjectFolder.PROPERTY);

            assertThat(found).isEqualTo(VaadinProjectFolder.moduleFolder(AppShell.class).getAbsolutePath());

            System.setProperty(VaadinProjectFolder.PROPERTY, "/somewhere/else");
            new VaadinProjectFolder().postProcessEnvironment(new MockEnvironment(), new SpringApplication());

            assertThat(System.getProperty(VaadinProjectFolder.PROPERTY)).isEqualTo("/somewhere/else");
        } finally {
            System.clearProperty(VaadinProjectFolder.PROPERTY);
        }
    }
}
