package net.lenni0451.tempalteprocessor.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.tempalteprocessor.OutputCleaner;
import net.lenni0451.tempalteprocessor.helper.CapitalizeHelper;
import net.lenni0451.tempalteprocessor.helper.LowerCaseHelper;
import net.lenni0451.tempalteprocessor.helper.UpperCaseHelper;
import net.lenni0451.tempalteprocessor.helper.VarHelper;
import net.lenni0451.tempalteprocessor.model.TemplateConfig;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.trimou.Mustache;
import org.trimou.engine.MustacheEngine;
import org.trimou.engine.MustacheEngineBuilder;
import org.trimou.engine.config.EngineConfigurationKey;
import org.trimou.engine.locator.FileSystemTemplateLocator;
import org.trimou.engine.resolver.MapResolver;
import org.trimou.gson.resolver.JsonElementResolver;
import org.trimou.handlebars.HelpersBuilder;

import java.io.*;
import java.util.*;

@Slf4j
public abstract class TemplateProcessorTask extends DefaultTask {

    private final Gson GSON = new Gson();

    @InputFiles
    public abstract DirectoryProperty getTemplateDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    public void initSourceSets(final Set<SourceSet> sourceSets) {
        if (this.getTemplateDir().get().getAsFile().exists()) {
//            this.getOutputDir().get().getAsFile().mkdirs(); //Create the output directory if it does not exist
            for (SourceSet sourceSet : sourceSets) {
                //Add the output directory to all required source sets
                sourceSet.getJava().srcDir(this.getOutputDir().get());
                log.debug("Added output '{}' directory to source set '{}'", this.getOutputDir().get().getAsFile().getAbsolutePath(), sourceSet.getName());
            }
        }
    }

    @TaskAction
    public void run() throws IOException {
        if (!this.getTemplateDir().get().getAsFile().exists()) {
            log.warn("Template directory '{}' does not exist and will be skipped", this.getTemplateDir().get().getAsFile().getAbsolutePath());
            return;
        }
        this.getOutputDir().get().getAsFile().mkdirs(); //Create the output directory if it does not exist

        List<TemplateConfig> templates = this.loadTemplates(this.getTemplateDir().get().getAsFile());
        if (templates.isEmpty()) {
            log.warn("No templates found in directory '{}'", this.getTemplateDir().get().getAsFile().getAbsolutePath());
            return;
        }
        log.info("Loaded {} templates", templates.size());

        for (TemplateConfig templateConfig : templates) {
            MustacheEngine templateEngine = this.buildTemplateEngine(templateConfig.globals);
            Mustache template = templateEngine.getMustache(new File(templateConfig.baseDir, templateConfig.template).getPath());
            Mustache variantTemplate = templateEngine.compileMustache(templateConfig.target);
            for (JsonElement variant : templateConfig.variants) {
                String relativeTarget = variantTemplate.render(variant);
                File target = new File(this.getOutputDir().get().getAsFile(), relativeTarget);
                target.getParentFile().mkdirs();
                long time = System.nanoTime();
                try (FileOutputStream fos = new FileOutputStream(target)) {
                    StringWriter writer = new StringWriter();
                    template.render(writer, this.merge(variant, templateConfig.variables));
                    fos.write(OutputCleaner.clean(writer.toString()).getBytes());
                }
                time = System.nanoTime() - time;
                log.info("Rendered template '{}' to '{}' in {}ms", templateConfig.template, target.getAbsolutePath(), time / 1_000_000);
            }
        }
    }

    private List<TemplateConfig> loadTemplates(final File dir) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();
        File[] files = dir.listFiles();
        if (files == null) return Collections.emptyList();

        List<TemplateConfig> templates = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                templates.addAll(this.loadTemplates(file));
            } else if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
                templates.add(this.loadTemplate(file));
            }
        }
        return templates;
    }

    private TemplateConfig loadTemplate(final File file) throws IOException {
        if (!file.exists()) throw new FileNotFoundException("The template file '" + file.getName() + "' does not exist");
        if (!file.isFile()) throw new IllegalStateException("The template file '" + file.getName() + "' is not a file");

        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            TemplateConfig templateConfig = GSON.fromJson(reader, TemplateConfig.class);
            if (templateConfig == null) throw new IllegalStateException("The template file '" + file.getName() + "' could not be parsed");

            if (templateConfig.parent != null) {
                TemplateConfig basedOn = this.loadTemplate(new File(file.getParentFile(), templateConfig.parent));
                templateConfig.copyFrom(basedOn);
            }
            templateConfig.baseDir = this.getTemplateDir().get().getAsFile().toPath().relativize(file.getParentFile().toPath()).toString();
            if (templateConfig.template == null) throw new IllegalStateException("The template file '" + file.getName() + "' does not contain a 'template' field");
            if (templateConfig.target == null) throw new IllegalStateException("The template file '" + file.getName() + "' does not contain a 'target' field");
            if (templateConfig.globals == null) templateConfig.globals = new JsonObject();
            if (templateConfig.variants == null) templateConfig.variants = new JsonElement[]{new JsonObject()};
            if (templateConfig.variables == null) templateConfig.variables = new JsonObject();

            return templateConfig;
        }
    }

    private JsonElement merge(final JsonElement variant, final JsonElement variables) {
        if (variant == null && variables == null) return new JsonObject();
        if (variables == null) return variant;

        if (variant instanceof JsonObject && variables instanceof JsonObject) {
            JsonObject merged = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : ((JsonObject) variant).entrySet()) merged.add(entry.getKey(), entry.getValue());
            for (Map.Entry<String, JsonElement> entry : ((JsonObject) variables).entrySet()) merged.add(entry.getKey(), entry.getValue());
            return merged;
        }
        log.warn("Could not merge variant '{}' with variables '{}'", variant, variables);
        return variables;
    }

    private MustacheEngine buildTemplateEngine(final JsonObject globals) {
        MustacheEngineBuilder builder = MustacheEngineBuilder
                .newBuilder()
                .setProperty(JsonElementResolver.UNWRAP_JSON_PRIMITIVE_KEY, true)
                .addTemplateLocator(new FileSystemTemplateLocator(0, this.getTemplateDir().get().getAsFile().getAbsolutePath()))
                .addResolver(new MapResolver())
                .registerHelpers(
                        HelpersBuilder
                                .all()
                                .add("lower", new LowerCaseHelper())
                                .add("upper", new UpperCaseHelper())
                                .add("var", new VarHelper())
                                .add("capitalize", new CapitalizeHelper())
                                .build()
                )
                .setProperty(EngineConfigurationKey.SKIP_VALUE_ESCAPING, true);
        for (Map.Entry<String, JsonElement> entry : globals.entrySet()) builder.addGlobalData(entry.getKey(), this.resolvePrimitive(entry.getValue()));
        return builder.build();
    }

    private Object resolvePrimitive(final JsonElement element) {
        if (!(element instanceof JsonPrimitive)) return element;
        JsonPrimitive primitive = (JsonPrimitive) element;
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isString()) return primitive.getAsString();
        if (primitive.isNumber()) return primitive.getAsNumber();
        return primitive;
    }

}
