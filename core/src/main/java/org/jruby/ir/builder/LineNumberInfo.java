package org.jruby.ir.builder;

/**
 * Line coverage state of an {@link IRBuilder}: which statements are line events and the line each one is
 * counted on. Only created while Coverage measures the scope being built.
 *
 * @param <U> the builder's node type
 */
final class LineNumberInfo<U> {
    /**
     * The last statement that was a line event: the line it starts on and its first instruction's node.
     */
    record LastEvent<U>(int line, U firstInstruction) {}

    // How deep the builder is in the tree it builds, and how deep the statement whose line event is pending is
    private int depth = 0;
    private int pendingDepth = 0;
    // The line coverage counts the pending statement on. Calls built before its first instruction move the line
    // backtraces report, but not this one.
    private int pendingLine = -1;
    private int lastEventLine = -1;
    private U lastEventFirstInstruction = null;

    void enter() {
        depth++;
    }

    void exit() {
        depth--;
    }

    /**
     * As in MRI, a statement is a line event unless the last one started on the same line, or it is inside the
     * last one's statement and starts with the same instruction.
     */
    boolean isNewEvent(int line, U firstInstruction) {
        return line != lastEventLine && firstInstruction != lastEventFirstInstruction;
    }

    /**
     * Whether the pending event belongs to a statement enclosing the one being built now.
     */
    boolean isPendingEnclosing() {
        return pendingDepth < depth;
    }

    /**
     * Make the statement starting on line the pending line event, counted on countedLine.
     */
    void startEvent(int line, U firstInstruction, int countedLine) {
        pendingDepth = depth;
        pendingLine = countedLine;
        lastEventLine = line;
        lastEventFirstInstruction = firstInstruction;
    }

    /**
     * A statement on line that shares the last line event.
     */
    void continueEvent(int line) {
        lastEventLine = line;
    }

    int getPendingLine() {
        return pendingLine;
    }

    LastEvent<U> getLastEvent() {
        return new LastEvent<>(lastEventLine, lastEventFirstInstruction);
    }

    void setLastEvent(LastEvent<U> event) {
        lastEventLine = event.line();
        lastEventFirstInstruction = event.firstInstruction();
    }
}
