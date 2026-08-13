package net.neoforged.neoform.runtime.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class LegacyMappings {
    private final String officialMappings;
    private final String intermediaryMappings;

    private LegacyMappings(List<ClassMapping> classes) {
        var official = new StringBuilder();
        var intermediary = new StringBuilder();
        for (var classMapping : classes) {
            classMapping.appendOfficialTo(official);
            classMapping.appendIntermediaryTo(intermediary);
        }
        officialMappings = official.toString();
        intermediaryMappings = intermediary.toString();
    }

    static Builder builder() {
        return new Builder();
    }

    String officialMappings() {
        return officialMappings;
    }

    String intermediaryMappings() {
        return intermediaryMappings;
    }

    private static final class ClassMapping {
        private final String officialName;
        private final String obfuscatedName;
        private final String intermediaryName;
        private final List<FieldMapping> fields = new ArrayList<>();

        private ClassMapping(String officialName, String obfuscatedName, String intermediaryName) {
            this.officialName = officialName;
            this.obfuscatedName = obfuscatedName;
            this.intermediaryName = intermediaryName;
        }

        private void addField(FieldMapping field) {
            fields.add(field);
        }

        private void appendOfficialTo(StringBuilder result) {
            result.append(officialName)
                    .append(" -> ")
                    .append(obfuscatedName)
                    .append(":\n");
            for (var field : fields) {
                field.appendOfficialTo(result);
            }
        }

        private void appendIntermediaryTo(StringBuilder result) {
            result.append(obfuscatedName)
                    .append(' ')
                    .append(intermediaryName)
                    .append('\n');
            for (var field : fields) {
                field.appendIntermediaryTo(result);
            }
        }
    }

    private record FieldMapping(String type,
                                String officialName,
                                String obfuscatedName,
                                String intermediaryName) {
        private void appendOfficialTo(StringBuilder result) {
            result.append("    ")
                    .append(type)
                    .append(' ')
                    .append(officialName)
                    .append(" -> ")
                    .append(obfuscatedName)
                    .append('\n');
        }

        private void appendIntermediaryTo(StringBuilder result) {
            result.append('\t')
                    .append(obfuscatedName)
                    .append(' ')
                    .append(intermediaryName)
                    .append('\n');
        }
    }

    static final class Builder {
        private final List<ClassMapping> classes = new ArrayList<>();
        private ClassMapping currentClass;

        private Builder() {
        }

        Builder classMapping(String officialName, String obfuscatedName, String intermediaryName) {
            currentClass = new ClassMapping(
                    Objects.requireNonNull(officialName),
                    Objects.requireNonNull(obfuscatedName),
                    Objects.requireNonNull(intermediaryName)
            );
            classes.add(currentClass);
            return this;
        }

        Builder fieldMapping(String type,
                             String officialName,
                             String obfuscatedName,
                             String intermediaryName) {
            if (currentClass == null) {
                throw new IllegalStateException("A class mapping must be added before its fields");
            }
            currentClass.addField(new FieldMapping(
                    Objects.requireNonNull(type),
                    Objects.requireNonNull(officialName),
                    Objects.requireNonNull(obfuscatedName),
                    Objects.requireNonNull(intermediaryName)
            ));
            return this;
        }

        LegacyMappings build() {
            if (classes.isEmpty()) {
                throw new IllegalStateException("At least one class mapping must be added");
            }
            return new LegacyMappings(classes);
        }
    }
}
