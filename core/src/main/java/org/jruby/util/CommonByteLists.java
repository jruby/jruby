package org.jruby.util;

/**
 * Values which are referenced in multiple places.  A ByteList warehouse!
 */
public class CommonByteLists {
    public static final ByteList EMPTY = new ByteList(new byte[] {});
    public static final ByteList AMPERSAND_AMPERSAND = new ByteList(new byte[] {'&', '&'});
    public static final ByteList AREF_METHOD = new ByteList(new byte[] {'[', ']'});
    public static final ByteList ASET_METHOD = new ByteList(new byte[] {'[', ']', '='});
    public static final ByteList BACKTRACE_IN = new ByteList(new byte[] {':', 'i', 'n', ' ', '`'});
    public static final ByteList COLON = new ByteList(new byte[] {':'});
    public static final ByteList COLON_COLON = new ByteList(new byte[] {':', ':'});
    public static final ByteList CONSTANTS = new ByteList(new byte[] {'c', 'o', 'n', 's', 't', 'a', 'n', 't', 's'});
    public static final ByteList DEFINE_METHOD_METHOD = new ByteList(new byte[] {'d', 'e', 'f', 'i', 'n', 'e', '_', 'm', 'e', 't', 'h', 'o', 'd'});
    public static final ByteList DOLLAR_SLASH = new ByteList(new byte[] {'$', '/'});
    public static final ByteList DOLLAR_BACKSLASH = new ByteList(new byte[] {'$', '\\'});
    public static final ByteList EACH = new ByteList(new byte[] {'e', 'a', 'c', 'h'});
    public static final ByteList EXCEPTION = new ByteList(new byte[] {'E', 'x', 'c', 'e', 'p', 't', 'i', 'o', 'n'});
    public static final ByteList _END_ = new ByteList(new byte[] {'_', 'E', 'N', 'D', '_'});
    public static final ByteList EQUAL_TILDE = new ByteList(new byte[] {'=', '~'});
    public static final ByteList METHODS = new ByteList(new byte[] {'m', 'e', 't', 'h', 'o', 'd', 's'});
    public static final ByteList FREEZE_METHOD = new ByteList(new byte[] {'f', 'r', 'e', 'e', 'z', 'e'});
    public static final ByteList NEW = new ByteList(new byte[] {'n', 'e', 'w'});
    public static final ByteList NEW_METHOD = NEW;
    public static final ByteList NOT_IMPLEMENTED_ERROR = new ByteList(new byte[] {'N', 'o', 't', 'I', 'm', 'p', 'l', 'e', 'm', 'e', 'n', 't', 'e', 'd', 'E', 'r', 'r', 'o', 'r'});
    public static final ByteList OR_OR = new ByteList(new byte[] {'|', '|'});
    public static final ByteList SINGLE_QUOTE = new ByteList(new byte[] {'\''});
    public static final ByteList STAR = new ByteList(new byte[] {'*'});
    public static final ByteList USING_METHOD = new ByteList(new byte[] {'u', 's', 'i', 'n', 'g'});
    public static final ByteList REFINE_METHOD = new ByteList(new byte[] {'r', 'e', 'f', 'i', 'n', 'e'});
}
