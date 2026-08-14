package net.neoforged.neoform.runtime.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static net.neoforged.neoform.runtime.cli.ResultIds.CLIENT_RESOURCES;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR_NO_RECOMP;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR_WITH_SOURCES;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_SOURCES;
import static org.assertj.core.api.Assertions.assertThat;

class CommonNfrtWorkflowsIntegrationTest {
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

    @Test
    void reusesCachedWorkAcrossInvocations(@TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .source("Example.java", "class Example {}")
                .build();
        var command = NfrtCommand.builder(tempDir, fixture)
                .enableCache()
                .result(GAME_JAR)
                .build();

        command.executeSuccessfully();
        var secondOutput = command.executeSuccessfully();

        assertThat(secondOutput)
                .contains("REUSE Used cache of decompile")
                .contains("REUSE Used cache of recompile");
        assertThat(command.resultEntries(GAME_JAR)).contains("Example.class");
    }
}
