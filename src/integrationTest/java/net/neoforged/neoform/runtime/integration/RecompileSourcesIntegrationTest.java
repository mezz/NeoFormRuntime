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
        var exampleClass = command.readResultBytes(GAME_JAR, "example/Example.class");
        assertThat(classFileMajorVersion(exampleClass)).isEqualTo(61);
        assertThat(command.readResult(GAME_JAR, "data.txt")).isEqualTo("resource");
    }

    private static int classFileMajorVersion(byte[] classFile) {
        return (Byte.toUnsignedInt(classFile[6]) << 8) | Byte.toUnsignedInt(classFile[7]);
    }
}
