/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.anno;

import org.jruby.RubyModule;

/**
 *
 * @author headius
 */
public interface TypePopulator {
    void populate(RubyModule clsmod);
}
