package org.jruby.util;

import java.util.HashMap;
import java.util.Map;

/**
 * An enum for all "defined?" messages.
 */
public enum DefinedMessage {
    EXPRESSION("expression"),
    ASSIGNMENT("assignment"),
    GLOBAL_VARIABLE("global-variable"),
    METHOD("method"),
    CLASS_VARIABLE("class variable"),
    CONSTANT("constant"),
    LOCAL_VARIABLE("local-variable"),
    LOCAL_VARIABLE_IN_BLOCK("local-variable(in-block)"),
    FALSE("false"),
    INSTANCE_VARIABLE("instance-variable"),
    NIL("nil"),
    SELF("self"),
    SUPER("super"),
    TRUE("true"),
    YIELD("yield"),
    BACKREF_AMPERSAND("$&"),
    BACKREF_PLUS("$+"),
    BACKREF_BACKTICK("$`"),
    BACKREF_SQUOTE("$'"),
    BACKREF_ONE("$1"),
    BACKREF_TWO("$2"),
    BACKREF_THREE("$3"),
    BACKREF_FOUR("$4"),
    BACKREF_FIVE("$5"),
    BACKREF_SIX("$6"),
    BACKREF_SEVEN("$7"),
    BACKREF_EIGHT("$8"),
    BACKREF_NINE("$9");

    private static final Map<String, DefinedMessage> byText = new HashMap<String, DefinedMessage>();

    static {
        for (DefinedMessage definedMessage : values()) {
            byText.put(definedMessage.getText(), definedMessage);
        }
    }

    public String getText() {
        return text;
    }

    public static DefinedMessage byText(String text) {
        return byText.get(text);
    }

    private DefinedMessage(String text) {
        this.text = text;
    }

    private final String text;
}
