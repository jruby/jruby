package org.jruby.ir.instructions;

/**
 * Represents a site for a call or yield.  Possibly other types of sites.
 */
public interface Site {
    long getCallSiteId();
    // FIXME: It would be nice to eliminate needing to change siteid via cloning.
    void setCallSiteId(long siteId);
}
