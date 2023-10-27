package org.prism;

// @formatter:off
public final class ParseResult {

    public static final class MagicComment {
        public final Nodes.Location keyLocation;
        public final Nodes.Location valueLocation;

        public MagicComment(Nodes.Location keyLocation, Nodes.Location valueLocation) {
            this.keyLocation = keyLocation;
            this.valueLocation = valueLocation;
        }
    }

    public static final class Error {
        public final String message;
        public final Nodes.Location location;

        public Error(String message, Nodes.Location location) {
            this.message = message;
            this.location = location;
        }
    }

    public static final class Warning {
        public final String message;
        public final Nodes.Location location;

        public Warning(String message, Nodes.Location location) {
            this.message = message;
            this.location = location;
        }
    }

    public final Nodes.Node value;
    public final MagicComment[] magicComments;
    public final Error[] errors;
    public final Warning[] warnings;

    public ParseResult(Nodes.Node value, MagicComment[] magicComments, Error[] errors, Warning[] warnings) {
        this.value = value;
        this.magicComments = magicComments;
        this.errors = errors;
        this.warnings = warnings;
    }
}
// @formatter:on
