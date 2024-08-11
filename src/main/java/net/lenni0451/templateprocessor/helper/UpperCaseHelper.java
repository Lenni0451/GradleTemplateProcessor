package net.lenni0451.templateprocessor.helper;

import org.trimou.handlebars.AbstractHelper;
import org.trimou.handlebars.Options;

import java.util.Locale;

public class UpperCaseHelper extends AbstractHelper {

    @Override
    public void execute(Options options) {
        if (options.getParameters().size() != 1) throw new IllegalArgumentException("{{upper}} helper must have exactly one parameter");
        this.append(options, this.convertValue(options.getParameters().get(0)).toUpperCase(Locale.ROOT));
    }

}
