package dev.qilletni.toolchain.logging;

import dev.qilletni.api.exceptions.QilletniException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;

@Plugin(name = "ProgressDisplayAppender", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ProgressDisplayAppender extends AbstractAppender {

    protected ProgressDisplayAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @PluginFactory
    public static ProgressDisplayAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {
        return new ProgressDisplayAppender(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();

        // Note: We use %s and pass the message as an arg to prevent issues if the message contains % signs
        if (event.getLevel().equals(Level.ERROR) || event.getLevel().equals(Level.FATAL)) {
            Throwable t = event.getThrown();
            // TODO: Are there any exceptions/edge cases to this?
            if (t instanceof QilletniException) {
                ProgressDisplay.error("%s", t, message);
            } else {
                ProgressDisplay.error("%s", message);
            }
        } else if (event.getLevel().equals(Level.WARN)) {
            ProgressDisplay.warn("%s", message);
        } else if (event.getLevel().equals(Level.INFO)) {
            ProgressDisplay.info("%s", message);
        }
    }
}
