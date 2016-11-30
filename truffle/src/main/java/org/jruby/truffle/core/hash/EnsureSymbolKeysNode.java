package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class EnsureSymbolKeysNode extends RubyNode {

    @Child private RubyNode child;

    private final BranchProfile errorProfile = BranchProfile.create();

    public EnsureSymbolKeysNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object hash = child.execute(frame);
        for (KeyValue keyValue : HashOperations.iterableKeyValues((DynamicObject) hash)) {
            if (!RubyGuards.isRubySymbol(keyValue.getKey())) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorWrongArgumentType(keyValue.getKey(), "Symbol", this));
            }
        }
        return hash;
    }
}
