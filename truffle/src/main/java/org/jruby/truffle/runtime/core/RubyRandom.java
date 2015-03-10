package org.jruby.truffle.runtime.core;

/**
 * Represents the Ruby {@code Random} class.
 */
public class RubyRandom extends RubyBasicObject  {

    private Long seedValue;

    public RubyRandom(RubyClass randomClass) {
        super(randomClass);
    }

    public void setSeedValue(Long seedValue) {
        this.seedValue = seedValue;
    }

}
