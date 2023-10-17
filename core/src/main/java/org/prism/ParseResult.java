package org.prism;

public final class ParseResult {

    public enum CommentType {
        /** # comment */
        INLINE,
        /** =begin/=end */
        EMBEDDED_DOCUMENT,
        /** after __END__ */
        __END__;

        static final CommentType[] VALUES = values();
    }

    public static final class Comment {
        public final CommentType type;
        public final Nodes.Location location;

        public Comment(CommentType type, Nodes.Location location) {
            this.type = type;
            this.location = location;
        }
    }

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
    public final Comment[] comments;
    public final MagicComment[] magicComments;
    public final Error[] errors;
    public final Warning[] warnings;

    public ParseResult(Nodes.Node value, Comment[] comments, MagicComment[] magicComments, Error[] errors, Warning[] warnings) {
        this.value = value;
        this.comments = comments;
        this.magicComments = magicComments;
        this.errors = errors;
        this.warnings = warnings;
    }
}
