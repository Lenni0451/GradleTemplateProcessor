package net.lenni0451.tempalteprocessor.helper;

import net.lenni0451.tempalteprocessor.OutputCleaner;
import org.trimou.handlebars.AbstractHelper;
import org.trimou.handlebars.Options;

import java.util.HashMap;
import java.util.Map;

public class VarHelper extends AbstractHelper {

    private final Map<String, String> variables = new HashMap<>();

    @Override
    public void execute(Options options) {
        if (options.getParameters().size() != 1) throw new IllegalArgumentException("{{var}} helper must have exactly one parameter");
        String varName = options.getParameters().get(0).toString();
        if (this.isSection(options)) {
            StringBuilder content = new StringBuilder();
            options.fn(content);
            this.variables.put(varName, content.toString());
            this.append(options, OutputCleaner.CLEAN_PLACEHOLDER);
        } else {
            this.append(options, this.variables.get(varName));
        }
    }

}
