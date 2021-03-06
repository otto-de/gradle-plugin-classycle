package de.otto.gradle.plugin.classycle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.types.FileSet;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.internal.logging.ConsoleRenderer;

import classycle.ant.DependencyCheckingTask;

public class ClassyclePlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {

        // create tasks only when java plugin is loaded
        project.getPlugins().withType(JavaPlugin.class, new Action<JavaPlugin>() {
            @Override
            public void execute(final JavaPlugin javaPlugin) {
                // create configuration object
                project.getExtensions().create("classycleConfig", ClassycleExtension.class);

                // create overall classycle task
                final Task overallTask = project.task("classycle");
                // check task depends on the overall
                final Task checkTask = project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME);
                checkTask.dependsOn(overallTask);

                // after evaluation, all scourceSets are filled
                project.afterEvaluate(new Action<Project>() {
                    @Override
                    public void execute(final Project project) {
                        final ClassycleExtension extension = getClassycleExtension(project);
                        for (final SourceSet sourceSet : extension.getSourceSets()) {
                            final Task sourceSetTask = createClassycleTask(project, extension, sourceSet);
                            overallTask.dependsOn(sourceSetTask);
                        }
                    }
                });
            }
        });
    }

    private Task createClassycleTask(final Project project, final ClassycleExtension extension, final SourceSet sourceSet) {

        final String taskName = sourceSet.getTaskName("classycle", null);
        final FileCollection classesDirs = sourceSet.getOutput().getClassesDirs();
        final File reportFile = getReportingExtension(project).file("classycle_" + sourceSet.getName() + ".txt");

        final Task task = project.task(taskName);
        task.getInputs().files(classesDirs, extension.getDefinitionFile());
        task.getOutputs().file(reportFile);
        task.doLast(new ClassyclePlugin.ClassycleAction(classesDirs, reportFile, extension));

        // the classycle task depends on the corresponding classes task
        final String classesTask = sourceSet.getClassesTaskName();
        task.dependsOn(classesTask);

        if (project.getLogger().isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            for (final File file : classesDirs) {
                sb.append(file.getAbsolutePath()).append(" ");
            }
            project.getLogger()
                    .debug("Created classycle task: " + taskName + ", report file: " + reportFile + ", depends on: "
                            + classesTask + " - sourceSetDirs: " + sb.toString());
        }

        return task;
    }

    private JavaPluginConvention getJavaPluginConvention(final Project project) {
        final JavaPluginConvention javaPlugin = project.getConvention().getPlugin(JavaPluginConvention.class);
        if (javaPlugin == null) {
            throw new GradleException("You must apply the java plugin before the classycle plugin.");
        }
        return javaPlugin;
    }

    private ReportingExtension getReportingExtension(final Project project) {
        return project.getExtensions().getByType(ReportingExtension.class);
    }

    private ClassycleExtension getClassycleExtension(final Project project) {
        ClassycleExtension extension = project.getExtensions().findByType(ClassycleExtension.class);
        if (extension == null) {
            extension = new ClassycleExtension();
        }

        // in case no sourceSet is defined, use main SourceSet...
        if (extension.getSourceSets().isEmpty()) {
            final SourceSet mainSourceSet = getJavaPluginConvention(project).getSourceSets()
                    .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            if (mainSourceSet == null) {
                throw new GradleException("no source set specified and no main source set found");
            }
            extension.getSourceSets().add(mainSourceSet);
        }

        return extension;
    }

    private static class ClassycleAction implements Action<Task> {

        private final FileCollection classesDirs;
        private final File reportFile;
        private final ClassycleExtension extension;

        private ClassycleAction(final FileCollection classesDirs, final File reportFile, final ClassycleExtension extension) {
            this.classesDirs = classesDirs;
            this.reportFile = reportFile;
            this.extension = extension;
        }

        @Override
        public void execute(final Task task) {
            final Logger log = task.getProject().getLogger();

            // check for definition file
            final File definitionFile = task.getProject().file(extension.getDefinitionFile());
            if (!definitionFile.exists()) {
                throw new RuntimeException("Classycle definition file not found: " + definitionFile);
            }

            // check for classesDirs
            final List<File> existingClassesDirs = new ArrayList<>();
            for (final File classesDir : classesDirs) {
                if (classesDir.isDirectory()) {
                    existingClassesDirs.add(classesDir);
                } else {
                    log.warn("Classycle: source directory does not exist: " + classesDir.getAbsolutePath());
                }
            }

            if (existingClassesDirs.isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                for (final File file : classesDirs) {
                    sb.append(file.getAbsolutePath()).append(" ");
                }
                throw new RuntimeException("Classycle: classes directories do not exists: " + sb.toString());
            }

            // create location for report file
            reportFile.getParentFile().mkdirs();
            try {
                log.debug("Running classycle analysis");
                final DependencyCheckingTask classycle = new DependencyCheckingTask();
                classycle.setReportFile(reportFile);
                classycle.setFailOnUnwantedDependencies(true);
                classycle.setMergeInnerClasses(true);
                classycle.setDefinitionFile(definitionFile);
                classycle.setProject(task.getProject().getAnt().getAntProject());
                for (final File classesDir : existingClassesDirs) {
                    final FileSet fileSet = new FileSet();
                    fileSet.setDir(classesDir);
                    fileSet.setIncludes("**/*.class");
                    fileSet.setProject(classycle.getProject());
                    classycle.add(fileSet);
                }
                classycle.execute();
            } catch (Exception e) {
                throw new GradleException("Classycle check failed: " + e.getMessage()
                        + ". See report at " + new ConsoleRenderer().asClickableFileUrl(reportFile), e);
            }
        }
    }
}