package net.neoforged.neoform.runtime.integration;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_SOURCES;
import static org.assertj.core.api.Assertions.assertThat;

class RunNeoFormCommandTest {
    @ParameterizedTest(name = "absolute paths: {0}")
    @ValueSource(booleans = {false, true})
    void appliesAccessTransformerPaths(boolean absolutePaths, @TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .source("Example.java", """
                        class Example {
                            private int regularField;
                            private int validatedField;
                        }
                        """)
                .build();

        var regularAtPath = Path.of("config/regular-access-transformer.cfg");
        var validatedAtPath = Path.of("config/validated-access-transformer.cfg");
        if (absolutePaths) {
            regularAtPath = tempDir.resolve(regularAtPath).toAbsolutePath();
            validatedAtPath = tempDir.resolve(validatedAtPath).toAbsolutePath();
        }

        var command = NfrtCommand.builder(tempDir, fixture)
                .accessTransformer(
                        regularAtPath,
                        "public Example regularField\n"
                )
                .validatedAccessTransformer(
                        validatedAtPath,
                        "public Example validatedField\n"
                )
                .build();

        command.executeSuccessfully();

        assertThat(command.readResult(GAME_SOURCES, "Example.java"))
                .isEqualTo("""
                        class Example {
                            public int regularField;
                            public int validatedField;
                        }
                        """);
    }
}
