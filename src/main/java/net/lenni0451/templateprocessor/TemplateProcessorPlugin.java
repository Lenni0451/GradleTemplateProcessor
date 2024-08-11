package net.lenni0451.templateprocessor;

import net.lenni0451.templateprocessor.extensions.TemplateProcessorExtension;
import net.lenni0451.templateprocessor.tasks.TemplateProcessorTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.Optional;

public class TemplateProcessorPlugin implements Plugin<Project> {

    private static final String PROCESS_TEMPLATES_TASK_NAME = "processTemplates";

    @Override
    public void apply(Project target) {
        TemplateProcessorExtension extension = target.getExtensions().create("templateProcessor", TemplateProcessorExtension.class, target);
        target.getTasks().register(PROCESS_TEMPLATES_TASK_NAME, TemplateProcessorTask.class, task -> {
            task.getTemplateDir().set(extension.getTemplateDir());
            task.getOutputDir().set(extension.getOutputDir());

            task.initSourceSets(extension.getSourceSets());
        });

        target.afterEvaluate(project -> {
            for (SourceSet sourceSet : project.getExtensions().getByType(SourceSetContainer.class)) {
                Optional.ofNullable(project.getTasks().findByName(sourceSet.getCompileJavaTaskName())).ifPresent(task -> task.dependsOn(PROCESS_TEMPLATES_TASK_NAME));
                Optional.ofNullable(project.getTasks().findByName(sourceSet.getSourcesJarTaskName())).ifPresent(task -> task.dependsOn(PROCESS_TEMPLATES_TASK_NAME));
            }
        });
    }

}
