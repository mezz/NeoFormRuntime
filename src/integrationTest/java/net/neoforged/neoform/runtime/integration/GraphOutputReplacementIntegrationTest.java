package net.neoforged.neoform.runtime.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_SOURCES;
import static org.assertj.core.api.Assertions.assertThat;

class GraphOutputReplacementIntegrationTest {
    @Test
    void replacementFeedsConsumersAndPublishedResults(@TempDir Path tempDir) throws Exception {
        var fixture = NeoFormFixture.builder()
                .source("Example.java", """
                        class Example {
                            private int value;
                        }
                        """)
                .source("Consumer.java", """
                        class Consumer {
                            int read(Example value) {
                                return value.value;
                            }
                        }
                        """)
                .build();
        var command = NfrtCommand.builder(tempDir, fixture)
                .accessTransformer("public Example value\n")
                .result(GAME_SOURCES)
                .result(GAME_JAR)
                .build();

        command.executeSuccessfully();

        assertThat(command.readResult(GAME_SOURCES, "Example.java"))
                .isEqualTo("""
                        class Example {
                            public int value;
                        }
                        """);
        assertThat(command.resultEntries(GAME_JAR))
                .contains("Example.class", "Consumer.class");
    }
}
