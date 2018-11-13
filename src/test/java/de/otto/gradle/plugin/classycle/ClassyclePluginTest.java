package de.otto.gradle.plugin.classycle;

import static org.junit.Assert.assertTrue;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

public class ClassyclePluginTest {

    @Test
    public void shouldApplyTaskCorrectly() {

        // given
        final Project project = ProjectBuilder.builder().build();
        final ClassyclePlugin plugin = new ClassyclePlugin();

        // when
        plugin.apply(project);

        // then
        // ... not dead
    }

    @Test
    public void shouldCreateConfigObjectInProject() {

        // given
        final Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");

        final ClassyclePlugin plugin = new ClassyclePlugin();

        // when
        plugin.apply(project);

        // then
        assertTrue(project.getExtensions().getByName("classycleConfig") instanceof ClassycleExtension);
    }

    @Test
    public void shouldCreateOverallClassycleTaskInProject() {

        // given
        final Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");

        final ClassyclePlugin plugin = new ClassyclePlugin();

        // when
        plugin.apply(project);

        // then
        final Task classycleTask = project.getTasks().getByName("classycle");
        assertTrue(project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).getDependsOn().contains(classycleTask));
    }
}