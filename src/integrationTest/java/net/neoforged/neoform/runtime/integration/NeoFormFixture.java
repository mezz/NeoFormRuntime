package net.neoforged.neoform.runtime.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class NeoFormFixture implements NfrtFixture {
    private final String minecraftVersion;
    private final Map<String, byte[]> sources;
    private final boolean compileLauncherSources;
    private final LegacyMappings legacyMappings;

    private NeoFormFixture(String minecraftVersion,
                           Map<String, byte[]> sources,
                           boolean compileLauncherSources,
                           LegacyMappings legacyMappings) {
        this.minecraftVersion = minecraftVersion;
        this.sources = Map.copyOf(sources);
        this.compileLauncherSources = compileLauncherSources;
        this.legacyMappings = legacyMappings;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public Materialized materialize(Context context) throws IOException {
        var neoForm = materializeNeoForm(context);
        return new Materialized(neoForm.launcherDirectory(), "--neoform=" + neoForm.neoFormArchive());
    }

    NeoFormMaterialized materializeNeoForm(Context context) throws IOException {
        var testDirectory = context.testDirectory();
        var sourcesArchive = testDirectory.resolve("sources.zip");
        NfrtFixtureSupport.writeZip(sourcesArchive, sources);

        var clientArtifact = compileLauncherSources
                ? NfrtFixtureSupport.compileSources(testDirectory, sources)
                : sourcesArchive;
        var downloads = new LinkedHashMap<String, Path>();
        downloads.put("client", clientArtifact);
        if (legacyMappings != null) {
            var officialMappings = testDirectory.resolve("client-mappings.txt");
            Files.writeString(officialMappings, legacyMappings.officialMappings());
            downloads.put("client_mappings", officialMappings);

            var intermediaryMappings = testDirectory.resolve("server-mappings.txt");
            Files.writeString(intermediaryMappings, legacyMappings.intermediaryMappings());
            downloads.put("server_mappings", intermediaryMappings);
        }

        var launcherDirectory = NfrtFixtureSupport.createLauncherDirectory(
                testDirectory,
                minecraftVersion,
                downloads
        );
        var neoFormArchive = createNeoFormArchive(context, sourcesArchive);
        return new NeoFormMaterialized(launcherDirectory, neoFormArchive);
    }

    private Path createNeoFormArchive(Context context, Path sourcesArchive) throws IOException {
        var steps = new ArrayList<Map<String, String>>();
        steps.add(Map.of("type", "downloadJson"));
        steps.add(Map.of("type", "downloadClient"));
        steps.add(Map.of("type", "strip", "name", "stripClient", "input", "{downloadClientOutput}"));
        if (legacyMappings != null) {
            steps.add(Map.of("type", "downloadClientMappings"));
            steps.add(Map.of("type", "downloadServerMappings", "name", "mergeMappings"));
        }
        steps.add(Map.of(
                "type", "copyFixtureSources",
                "name", "decompile",
                "input", "{stripClientOutput}",
                "fixtureInput", "{fixtureSources}"
        ));
        steps.add(Map.of("type", "copySources", "name", "patch", "input", "{decompileOutput}"));

        var copyArguments = List.of(
                "--in-format", "ARCHIVE",
                "--out-format", "ARCHIVE",
                "{input}", "{output}"
        );
        var config = new LinkedHashMap<String, Object>();
        config.put("spec", 1);
        config.put("version", minecraftVersion);
        config.put("official", true);
        config.put("java_target", 21);
        config.put("encoding", "UTF-8");
        config.put("data", Map.of("fixtureSources", "sources.zip"));
        config.put("steps", Map.of("joined", steps));
        config.put("functions", Map.of(
                "copyFixtureSources", Map.of(
                        "version", context.javaSourceTransformerCoordinate(),
                        "args", List.of(
                                "--in-format", "ARCHIVE",
                                "--out-format", "ARCHIVE",
                                "{fixtureInput}", "{output}"
                        )
                ),
                "copySources", Map.of(
                        "version", context.javaSourceTransformerCoordinate(),
                        "args", copyArguments
                )
        ));
        config.put("libraries", Map.of("joined", List.of()));

        var neoForm = context.testDirectory().resolve("neoform.zip");
        var entries = new LinkedHashMap<String, byte[]>();
        entries.put("config.json", NfrtFixtureSupport.jsonBytes(config));
        entries.put("sources.zip", Files.readAllBytes(sourcesArchive));
        NfrtFixtureSupport.writeZip(neoForm, entries);
        return neoForm;
    }

    record NeoFormMaterialized(Path launcherDirectory, Path neoFormArchive) {
    }

    static final class Builder {
        private final Map<String, byte[]> sources = new LinkedHashMap<>();
        private String minecraftVersion = "1.21";
        private boolean compileLauncherSources;
        private LegacyMappings legacyMappings;

        private Builder() {
        }

        Builder source(String path, String content) {
            return source(path, content.getBytes(StandardCharsets.UTF_8));
        }

        Builder source(String path, byte[] content) {
            sources.put(path, content.clone());
            return this;
        }

        Builder minecraftVersion(String minecraftVersion) {
            this.minecraftVersion = Objects.requireNonNull(minecraftVersion);
            return this;
        }

        Builder compileLauncherSources() {
            compileLauncherSources = true;
            return this;
        }

        Builder legacyMappings(LegacyMappings legacyMappings) {
            this.legacyMappings = Objects.requireNonNull(legacyMappings);
            return this;
        }

        NeoFormFixture build() {
            if (sources.isEmpty()) {
                throw new IllegalStateException("At least one source must be added");
            }
            return new NeoFormFixture(minecraftVersion, sources, compileLauncherSources, legacyMappings);
        }
    }
}
