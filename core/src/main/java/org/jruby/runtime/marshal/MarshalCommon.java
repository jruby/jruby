package org.jruby.runtime.marshal;

public class MarshalCommon {
    static final char TYPE_NIL = '0';
    static final char TYPE_TRUE = 'T';
    static final char TYPE_FALSE = 'F';
    static final char TYPE_FIXNUM = 'i';
    static final char TYPE_EXTENDED	= 'e';
    static final char TYPE_UCLASS = 'C';
    static final char TYPE_OBJECT = 'o';
    static final char TYPE_DATA = 'd';
    static final char TYPE_USERDEF = 'u';
    static final char TYPE_USRMARSHAL = 'U';
    static final char TYPE_FLOAT = 'f';
    static final char TYPE_BIGNUM = 'l';
    static final char TYPE_STRING = '"';
    static final char TYPE_REGEXP = '/';
    static final char TYPE_ARRAY = '[';
    static final char TYPE_HASH = '{';
    static final char TYPE_HASH_DEF = '}';
    static final char TYPE_STRUCT = 'S';
    static final char TYPE_MODULE_OLD = 'M';
    static final char TYPE_CLASS = 'c';
    static final char TYPE_MODULE = 'm';
    static final char TYPE_SYMBOL = ':';
    static final char TYPE_SYMLINK = ';';
    static final char TYPE_IVAR	= 'I';
    static final char TYPE_LINK	= '@';

    public static final String SYMBOL_ENCODING_SPECIAL = "E";
    public static final String SYMBOL_RUBY2_KEYWORDS_HASH_SPECIAL = "K";
    public static final String SYMBOL_ENCODING = "encoding";
}
