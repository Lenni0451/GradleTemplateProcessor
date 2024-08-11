package net.lenni0451.tempalteprocessor;

import net.lenni0451.tempalteprocessor.extensions.TemplateProcessorExtension;
import net.lenni0451.tempalteprocessor.tasks.TemplateProcessorTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

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
                project.getTasks().getByName(sourceSet.getCompileJavaTaskName()).dependsOn(PROCESS_TEMPLATES_TASK_NAME);
                project.getTasks().getByName(sourceSet.getSourcesJarTaskName()).dependsOn(PROCESS_TEMPLATES_TASK_NAME);
            }
        });
    }

}
