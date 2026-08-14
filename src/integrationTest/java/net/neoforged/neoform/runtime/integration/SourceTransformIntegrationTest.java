package net.neoforged.neoform.runtime.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR_NO_RECOMP;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_SOURCES;
import static org.assertj.core.api.Assertions.assertThat;

class SourceTransformIntegrationTest {
    @Test
    void appliesModernTransformsToSourcesAndClasses(@TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .minecraftVersion("1.21")
                .source("example/Example.java", """
                        package example;
                        public class Example {
                            private int regularField;
                            private int validatedField;
                        }
                        """)
                .compileLauncherSources()
                .build();
        var command = NfrtCommand.builder(tempDir, fixture)
                .accessTransformer("public example.Example regularField\n")
                .validatedAccessTransformer("public example.Example validatedField\n")
                .parchmentData(ParchmentData.builder()
                        .classJavadoc("example/Example", "Documented by Parchment")
                        .build())
                .result(GAME_SOURCES)
                .result(GAME_JAR)
                .result(GAME_JAR_NO_RECOMP)
                .build();

        command.executeSuccessfully();

        assertThat(command.readResult(GAME_SOURCES, "example/Example.java"))
                .isEqualTo("""
                        package example;
                        /**
                         * Documented by Parchment
                         */
                        public class Example {
                            public int regularField;
                            public int validatedField;
                        }
                        """);
        assertThat(command.resultEntries(GAME_JAR)).contains("example/Example.class");
        try (var loader = new URLClassLoader(
                new java.net.URL[]{command.resultPath(GAME_JAR_NO_RECOMP).toUri().toURL()}
        )) {
            var exampleClass = loader.loadClass("example.Example");
            assertThat(Modifier.isPublic(exampleClass.getDeclaredField("regularField").getModifiers())).isTrue();
            assertThat(Modifier.isPublic(exampleClass.getDeclaredField("validatedField").getModifiers())).isTrue();
        }
    }

    @Test
    void compilesAgainstGeneratedInterfaceStubsAndTransformsBinaryResult(@TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .minecraftVersion("1.21")
                .source("example/Target.java", """
                        package example;

                        public class Target {
                        }
                        """)
                .compileLauncherSources()
                .build();
        var command = NfrtCommand.builder(tempDir, fixture)
                .interfaceInjectionData(Map.of("example/Target", "injected/MissingInterface"))
                .result(GAME_SOURCES)
                .result(GAME_JAR)
                .result(GAME_JAR_NO_RECOMP)
                .build();

        command.executeSuccessfully();

        assertThat(command.readResult(GAME_SOURCES, "example/Target.java"))
                .isEqualTo("""
                        package example;

                        import injected.MissingInterface;

                        public class Target implements MissingInterface {
                        }
                        """);
        assertThat(command.resultEntries(GAME_JAR))
                .contains("example/Target.class")
                .doesNotContain("injected/MissingInterface.class");
        assertThat(new String(
                command.readResultBytes(GAME_JAR_NO_RECOMP, "example/Target.class"),
                StandardCharsets.ISO_8859_1
        )).contains("injected/MissingInterface");
    }

    @Test
    void appliesLegacyTransformsInNamingOrder(@TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .minecraftVersion("1.20.1")
                .source("Example.java", """
                        class Example {
                            private int f_1_;
                        }
                        """)
                .legacyMappings(LegacyMappings.builder()
                        .classMapping("Example", "a", "Example")
                        .fieldMapping("int", "officialField", "b", "f_1_")
                        .build())
                .build();
        var command = NfrtCommand.builder(tempDir, fixture)
                .accessTransformer("public Example f_1_\n")
                .parchmentData(ParchmentData.builder()
                        .fieldJavadoc("Example", "officialField", "I", "Mapped field documentation")
                        .build())
                .result(GAME_SOURCES)
                .result(GAME_JAR)
                .build();

        command.executeSuccessfully();

        assertThat(command.readResult(GAME_SOURCES, "Example.java"))
                .isEqualTo("""
                        class Example {
                            /**
                             * Mapped field documentation
                             */
                            public int officialField;
                        }
                        """);
        assertThat(command.resultEntries(GAME_JAR)).contains("Example.class");
    }
}
