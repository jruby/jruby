package org.jruby.scanner;

public final class Operators implements TokenTypes {

    public final String getOperator(IToken token) {
        switch (token.getType()) {
            case OP_ASSIGN :
            	if (token.getData() == null) {
            	    return "=";
            	} else {
            	    return token.getData() + "=";
            	}
            case OP_ASSOC :
                return "=>";
            case OP_EQ :
                return "==";
            case OP_EQQ :
                return "===";
            case OP_MATCH :
                return "=~";
            case OP_MUL:
            	return "*";
            case OP_NEQ :
                return "!=";
            case OP_NMATCH :
                return "!~";
            case OP_NOT :
                return "!";
            case OP_POW :
                return "**";
            default :
                return null;
        }
    }
}