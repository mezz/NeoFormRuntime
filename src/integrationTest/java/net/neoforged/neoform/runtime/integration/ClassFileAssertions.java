package net.neoforged.neoform.runtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

final class ClassFileAssertions {
    /**
     * Java 1 used class-file major version 45, with each subsequent Java release incrementing it by one.
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.1">JVMS §4.1</a>
     */
    private static final int CLASS_FILE_MAJOR_VERSION_OFFSET = 44;

    private ClassFileAssertions() {
    }

    /**
     * Asserts that a class file targets the requested Java release, such as Java 17.
     */
    static void assertTargetsJava(byte[] classFile, int expectedJavaVersion) {
        assertThat(classFile)
                .as("Class file")
                .hasSizeGreaterThanOrEqualTo(8);
        var actualJavaVersion = classFileMajorVersion(classFile) - CLASS_FILE_MAJOR_VERSION_OFFSET;
        assertThat(actualJavaVersion)
                .as("Java version targeted by class file")
                .isEqualTo(expectedJavaVersion);
    }

    /**
     * Reads the unsigned, big-endian {@code major_version} field from a class-file header.
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.1">JVMS §4.1</a>
     */
    private static int classFileMajorVersion(byte[] classFile) {
        return (Byte.toUnsignedInt(classFile[6]) << 8) | Byte.toUnsignedInt(classFile[7]);
    }
}
