package de.otto.gradle.plugin.classycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ClassyclePluginIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private void createJavaSources() throws IOException {
        final File rootDir = new File(testProjectDir.getRoot(), "src/main/java/org/example/");
        rootDir.mkdirs();
        final File aDir = new File(testProjectDir.getRoot(), "src/main/java/org/example/a");
        aDir.mkdirs();
        final File bDir = new File(testProjectDir.getRoot(), "src/main/java/org/example/b");
        bDir.mkdirs();

        final File mainClassFile = new File(rootDir, "Test.java");
        final String mainSource = "package org.example;\n" +
                "import org.example.a.A;\n" +
                "import org.example.b.B;\n" +
                "public class Test { public static void doIt() { A.doIt(); B.doIt(); } }";
        writeFile(mainClassFile, mainSource);

        final File aClassFile = new File(rootDir, "A.java");
        final String aSource = "package org.example.a;\n" +
                "import org.example.b.B;\n" +
                "public class A { public static void doIt() { B.doIt(); } }";
        writeFile(aClassFile, aSource);

        final File bClassFile = new File(rootDir, "B.java");
        final String bSource = "package org.example.b;\n" +
                "public class B { public static void doIt() {} }";
        writeFile(bClassFile, bSource);
    }

    private void createNonConflictingClassycleDefinitionsFile() throws IOException {
        final File srcDir = new File(testProjectDir.getRoot(), "etc/classycle/");
        srcDir.mkdirs();
        final File testFile = new File(srcDir, "dependencies.ddf");
        final String source = "{root} = org.example\n" +
                "[root] = ${root}.*\n" +
                "[a] = ${root}.a.*\n" +
                "[b] = ${root}.b.*\n" +
                "check sets [root] [a] [b]\n";

        writeFile(testFile, source);
    }

    private void createConflictingClassycleDefinitionsFile() throws IOException {
        final File srcDir = new File(testProjectDir.getRoot(), "etc/classycle/");
        srcDir.mkdirs();
        final File testFile = new File(srcDir, "dependencies.ddf");
        final String source = "{root} = org.example\n" +
                "[root] = ${root}.*\n" +
                "[a] = ${root}.a.*\n" +
                "[b] = ${root}.b.*\n" +
                "check sets [root] [a] [b]\n" +
                "check [a] independentOf [b]";

        writeFile(testFile, source);
    }

    private void createBuildFile() throws IOException {
        final File buildFile = testProjectDir.newFile("build.gradle");
        final String buildFileContent = "plugins {\n" +
                "    id 'java'\n" +
                "    id 'de.otto.classycle'\n" +
                "}";
        writeFile(buildFile, buildFileContent);
    }

    private boolean reportFileExists() throws IOException {
        final File report = new File(testProjectDir.getRoot(), "/build/reports/classycle_main.txt");
        return report.exists() && report.length() > 0;
    }

    @Test
    public void shouldEvaluateSuccessfully() throws IOException {

        // given
        createBuildFile();
        createJavaSources();
        createNonConflictingClassycleDefinitionsFile();

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withGradleVersion("4.7")
                .withPluginClasspath()
                .withArguments("classycle")
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":classycle").getOutcome());
        assertFalse(reportFileExists());
    }

    @Test
    public void shouldEvaluateWithError() throws IOException {

        // given
        createBuildFile();
        createJavaSources();
        createConflictingClassycleDefinitionsFile();

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withGradleVersion("4.7")
                .withPluginClasspath()
                .withArguments("classycle")
                .buildAndFail();

        assertTrue(reportFileExists());
    }

    private void writeFile(File destination, String content) throws IOException {
        try (BufferedWriter output = new BufferedWriter(new FileWriter(destination))) {
            output.write(content);
        }
    }
}


