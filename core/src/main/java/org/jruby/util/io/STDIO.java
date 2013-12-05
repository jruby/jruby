package org.jruby.util.io;

public enum STDIO {

    IN, OUT, ERR;

    public int fileno() {
        switch (this) {
        case IN:
            return 0;
        case OUT:
            return 1;
        case ERR:
            return 2;
        default:
            throw new RuntimeException();
        }
    }

    public static boolean isSTDIO(int fileno) {
        if (fileno >= 0 && fileno <= 2) {
            return true;
        }

        return false;
    }
}
