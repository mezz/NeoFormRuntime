package net.neoforged.neoform.runtime.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static net.neoforged.neoform.runtime.cli.ResultIds.CLIENT_RESOURCES;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR_NO_RECOMP;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR_WITH_SOURCES;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_SOURCES;
import static org.assertj.core.api.Assertions.assertThat;

class NeoFormIntegrationTest {
    @Test
    void writesStandardNeoFormArtifacts(@TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .source("example/Example.java", "package example; public class Example {}")
                .source("assets/example.txt", "resource")
                .compileLauncherSources()
                .build();
        var command = NfrtCommand.builder(tempDir, fixture)
                .result(GAME_JAR)
                .result(GAME_SOURCES)
                .result(GAME_JAR_WITH_SOURCES)
                .result(GAME_JAR_NO_RECOMP)
                .result(CLIENT_RESOURCES)
                .build();

        command.executeSuccessfully();

        assertThat(command.resultEntries(GAME_JAR))
                .contains("example/Example.class", "assets/example.txt")
                .doesNotContain("example/Example.java");
        assertThat(command.resultEntries(GAME_SOURCES))
                .contains("example/Example.java", "assets/example.txt")
                .doesNotContain("example/Example.class");
        assertThat(command.resultEntries(GAME_JAR_WITH_SOURCES))
                .contains("example/Example.class", "example/Example.java", "assets/example.txt");
        assertThat(command.resultEntries(GAME_JAR_NO_RECOMP))
                .contains("example/Example.class")
                .doesNotContain("example/Example.java", "assets/example.txt");
        assertThat(command.resultEntries(CLIENT_RESOURCES))
                .contains("assets/example.txt")
                .doesNotContain("example/Example.class", "example/Example.java");
    }

    @ParameterizedTest(name = "use Eclipse compiler: {0}")
    @ValueSource(booleans = {false, true})
    void recompilesJavaSourcesAtConfiguredVersionAndPreservesResources(boolean useEclipseCompiler,
                                                                       @TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .source("example/Example.java", "package example; public class Example {}")
                .source("data.txt", "resource")
                .javaVersion(17)
                .build();
        var builder = NfrtCommand.builder(tempDir, fixture)
                .result(GAME_JAR);
        if (useEclipseCompiler) {
            builder.argument("--use-eclipse-compiler");
        }
        var command = builder.build();

        command.executeSuccessfully();

        assertThat(command.resultEntries(GAME_JAR))
                .containsExactlyInAnyOrder("example/Example.class", "data.txt");
        command.assertResultClassJavaVersion(GAME_JAR, "example/Example.class", 17);
        assertThat(command.readResult(GAME_JAR, "data.txt")).isEqualTo("resource");
    }
}
