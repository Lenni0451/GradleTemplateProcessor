package net.lenni0451.templateprocessor.extensions;

import lombok.Getter;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;

import java.util.HashSet;
import java.util.Set;

public abstract class TemplateProcessorExtension {

    private final Project project;
    @Getter
    private final Set<SourceSet> sourceSets;

    public TemplateProcessorExtension(final Project project) {
        this.project = project;
        this.sourceSets = new HashSet<>();
    }

    public Project getProject() {
        return this.project;
    }

    @Internal
    public void markAsSource(final SourceSet sourceSet) {
        this.sourceSets.add(sourceSet);
    }

    @InputDirectory
    public abstract DirectoryProperty getTemplateDir();

    @InputDirectory
    public abstract DirectoryProperty getOutputDir();

}
