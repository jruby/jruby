package org.jruby.ext.fiber;

public enum CoroutineFiberState {
    RUNNING, SUSPENDED_YIELD, SUSPENDED_RESUME, SUSPENDED_TRANSFER, FINISHED
}
