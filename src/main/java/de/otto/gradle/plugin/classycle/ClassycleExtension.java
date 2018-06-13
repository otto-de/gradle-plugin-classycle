package de.otto.gradle.plugin.classycle;

import java.util.ArrayList;
import java.util.Collection;

import org.gradle.api.tasks.SourceSet;

import groovy.lang.GroovyObjectSupport;

public class ClassycleExtension extends GroovyObjectSupport {

    private String definitionFile = "etc/classycle/dependencies.ddf";
    private Collection<SourceSet> sourceSets = new ArrayList<>();

    public String getDefinitionFile() {
        return definitionFile;
    }

    public void setDefinitionFile(final String definitionFile) {
        this.definitionFile = definitionFile;
    }

    public Collection<SourceSet> getSourceSets() {
        return sourceSets;
    }

    public void setSourceSets(final Collection<Object> sourceSets) {
        // this is just an ugly hack, but for some reason, the sourceSets are a collection themselves
        for (final Object o1 : sourceSets) {
            if (o1 instanceof Collection) {
                final Collection c = (Collection) o1;
                for (final Object o2 : c) {
                    if (o2 instanceof SourceSet) {
                        this.sourceSets.add((SourceSet) o2);
                    } else {
                        throw new RuntimeException("unable to cast " + o2.getClass().toString() + " to SourceSet");
                    }
                }
            } else if (o1 instanceof SourceSet) {
                this.sourceSets.add((SourceSet) o1);
            } else {
                throw new RuntimeException("unable to cast " + o1.getClass().toString() + " to SourceSet");
            }
        }
    }
}
