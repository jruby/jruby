/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.java_signature;

import java.util.List;

/**
 *
 */
public class ConstructorSignatureNode extends SignatureNode {
    public ConstructorSignatureNode(String name, List<ParameterNode> parameterList) {
        super(name, parameterList);
    }
}
