package org.jruby.util.cli;

/**
 * Represents a single option to JRuby, with a category, name, value type,
 * options, default value, and description.
 *
 * This type should be subclassed for specific types of values.
 *
 * @param <T> the type of value associated with the option
 */
public abstract class Option<T> {
    public Option(Category category, String name, Class<T> type, T[] options, T defval, String description) {
        this.category = category;
        this.name = name;
        this.type = type;
        this.options = options == null ? new String[]{type.getSimpleName()} : options;
        this.defval = defval;
        this.description = description;
    }

    @Override
    public String toString() {
        return "jruby." + name;
    }

    public abstract T load();
    public final Category category;
    public final String name;
    public final Class type;
    public final Object[] options;
    public final T defval;
    public final String description;
}
