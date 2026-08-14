package net.neoforged.neoform.runtime.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR_NO_RECOMP_WITH_NEOFORGE;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR_WITH_NEOFORGE;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_JAR_WITH_SOURCES_AND_NEOFORGE;
import static net.neoforged.neoform.runtime.cli.ResultIds.GAME_SOURCES_WITH_NEOFORGE;
import static org.assertj.core.api.Assertions.assertThat;

class NeoForgeSourceResultIntegrationTest {
    @Test
    void routesTransformedSourcesAndCompiledClassesToNeoForgeResults(@TempDir Path tempDir) throws Exception {
        var neoForm = NeoFormFixture.builder()
                .minecraftVersion("1.21")
                .source("example/Game.java", """
                        package example;

                        public class Game {
                            private int neoForgeTransformed;
                            private int userTransformed;
                        }
                        """)
                .compileLauncherSources()
                .build();
        var fixture = NeoForgeFixture.builder(neoForm)
                .source("neoforge/Added.java", "package neoforge; public class Added {}")
                .universalEntry("neoforge-marker.txt", "universal")
                .accessTransformer("public example.Game neoForgeTransformed\n")
                .patch("example/Game.java.patch", """
                        --- a/example/Game.java
                        +++ b/example/Game.java
                        @@ -3,4 +3,8 @@
                         public class Game {
                             private int neoForgeTransformed;
                             private int userTransformed;
                        +
                        +    public String patched() {
                        +        return "yes";
                        +    }
                         }
                        """)
                .build();
        var command = NfrtCommand.builder(tempDir, fixture)
                .accessTransformer("public example.Game userTransformed\n")
                .result(GAME_JAR)
                .result(GAME_SOURCES_WITH_NEOFORGE)
                .result(GAME_JAR_WITH_NEOFORGE)
                .result(GAME_JAR_WITH_SOURCES_AND_NEOFORGE)
                .result(GAME_JAR_NO_RECOMP_WITH_NEOFORGE)
                .build();

        command.executeSuccessfully();

        assertThat(command.resultEntries(GAME_JAR))
                .contains("example/Game.class")
                .doesNotContain("neoforge/Added.java", "neoforge-marker.txt");
        assertThat(command.resultEntries(GAME_SOURCES_WITH_NEOFORGE))
                .contains("example/Game.java", "neoforge/Added.java")
                .doesNotContain("example/Game.class", "neoforge-marker.txt");
        assertThat(command.readResult(GAME_SOURCES_WITH_NEOFORGE, "example/Game.java"))
                .isEqualTo("""
                        package example;

                        public class Game {
                            public int neoForgeTransformed;
                            public int userTransformed;

                            public String patched() {
                                return "yes";
                            }
                        }
                        """);
        assertThat(command.resultEntries(GAME_JAR_WITH_NEOFORGE))
                .contains("example/Game.class", "neoforge-marker.txt")
                .doesNotContain("example/Game.java", "neoforge/Added.java");
        assertThat(command.resultEntries(GAME_JAR_WITH_SOURCES_AND_NEOFORGE))
                .contains("example/Game.class", "example/Game.java", "neoforge/Added.java", "neoforge-marker.txt");
        assertThat(command.resultEntries(GAME_JAR_NO_RECOMP_WITH_NEOFORGE))
                .contains("example/Game.class", "neoforge-marker.txt")
                .doesNotContain("example/Game.java");
        try (var loader = new URLClassLoader(
                new java.net.URL[]{command.resultPath(GAME_JAR_NO_RECOMP_WITH_NEOFORGE).toUri().toURL()}
        )) {
            var gameClass = loader.loadClass("example.Game");
            assertThat(Modifier.isPublic(gameClass.getDeclaredField("neoForgeTransformed").getModifiers())).isTrue();
            assertThat(Modifier.isPublic(gameClass.getDeclaredField("userTransformed").getModifiers())).isTrue();
        }
    }
}
