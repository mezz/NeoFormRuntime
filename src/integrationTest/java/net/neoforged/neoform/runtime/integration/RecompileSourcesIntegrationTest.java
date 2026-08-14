package net.neoforged.neoform.runtime.integration;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR;
import static org.assertj.core.api.Assertions.assertThat;

class RecompileSourcesIntegrationTest {
    @ParameterizedTest(name = "use Eclipse compiler: {0}")
    @ValueSource(booleans = {false, true})
    void recompilesJavaSourcesAndPreservesResources(boolean useEclipseCompiler,
                                                    @TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .source("example/Example.java", "package example; public class Example {}")
                .source("data.txt", "resource")
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
        assertThat(command.readResult(GAME_JAR, "data.txt")).isEqualTo("resource");
    }
}
