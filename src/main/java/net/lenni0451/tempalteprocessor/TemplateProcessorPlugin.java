package net.lenni0451.tempalteprocessor;

import net.lenni0451.tempalteprocessor.extensions.TemplateProcessorExtension;
import net.lenni0451.tempalteprocessor.tasks.TemplateProcessorTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class TemplateProcessorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project target) {
        TemplateProcessorExtension extension = target.getExtensions().create("templateProcessor", TemplateProcessorExtension.class, target);
        target.getTasks().register("processTemplates", TemplateProcessorTask.class, task -> {
            task.getTemplateDir().set(extension.getTemplateDir());
            task.getOutputDir().set(extension.getOutputDir());

            task.initSourceSets(extension.getSourceSets());
        });

        target.afterEvaluate(project -> {
            project.getTasks().getByName("compileJava").dependsOn("processTemplates");
            project.getTasks().getByName("compileTestJava").dependsOn("processTemplates");
            project.getTasks().getByName("sourcesJar").dependsOn("processTemplates");
        });
    }

}
