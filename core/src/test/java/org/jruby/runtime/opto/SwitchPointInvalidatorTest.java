package org.jruby.runtime.opto;

import java.lang.invoke.SwitchPoint;
import java.util.ArrayList;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.Options;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class SwitchPointInvalidatorTest {

    @Test
    public void invalidateDuringInvalidateAllInvalidatesTheSwitchPointAlreadyHandedOut() {
        SwitchPointInvalidator target = new SwitchPointInvalidator();
        SwitchPointInvalidator other = new SwitchPointInvalidator();
        SwitchPoint handedOut = (SwitchPoint) target.getData();
        other.getData();

        boolean[] hookRan = {false};
        boolean[] invalidWhenInvalidateReturned = {false};
        List<Invalidator> batch = new ArrayList<>(List.of(target, other)) {
            @Override
            public Invalidator get(int index) {
                if (index == 1 && !hookRan[0]) {
                    hookRan[0] = true;
                    target.invalidate();
                    invalidWhenInvalidateReturned[0] = handedOut.hasBeenInvalidated();
                }
                return super.get(index);
            }
        };

        target.invalidateAll(batch);

        assertTrue("hook inside invalidateAll did not run", hookRan[0]);
        assertTrue("invalidate() returned while the SwitchPoint it handed out was still valid", invalidWhenInvalidateReturned[0]);
    }

    @Test
    public void singletonClosureWriteDuringBulkInvalidationUsesTheNewReader() {
        assumeTrue("requires indy dispatch", Options.COMPILE_INVOKEDYNAMIC.load());
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setCompileMode(RubyInstanceConfig.CompileMode.JIT);
        config.setJitThreshold(0);
        config.setJitBackground(false);
        Ruby runtime = Ruby.newInstance(config);
        try {
            // Reduced class_attribute: each owner's reader and writer share a value;
            // an inherited writer creates the subclass's own pair on its first write.
            RubyClass child = (RubyClass) runtime.evalScriptlet("""
                    module CallbackAttribute
                      def self.redefine(owner, value)
                        owner.singleton_class.class_eval do
                          define_method(:__callbacks_value) { value }
                          define_method(:__callbacks_value=) do |replacement|
                            if equal?(owner)
                              value = replacement
                            else
                              CallbackAttribute.redefine(self, replacement)
                            end
                          end
                          private :__callbacks_value, :__callbacks_value=
                        end
                      end
                    end
                    class CallbackBase
                      def self.__callbacks = __callbacks_value
                      def self.__callbacks=(value)
                        self.__callbacks_value = value
                      end
                      def self.copy_and_write
                        self.__callbacks = __callbacks.dup
                        self.__callbacks[:child] = true
                      end
                    end
                    CallbackAttribute.redefine(CallbackBase, {parent: true})
                    class CallbackChild < CallbackBase
                      extend Module.new
                    end
                    class CallbackSibling < CallbackBase; end
                    20.times do
                      CallbackBase.__callbacks
                      CallbackSibling.__callbacks
                      CallbackChild.__callbacks
                    end
                    CallbackChild
                    """);

            RubyModule.InvalidatorList<Invalidator> invalidators = new RubyModule.InvalidatorList<>(2);
            child.getMetaClass().getInvalidator().addIfUsed(invalidators);
            assertTrue("warm reader did not register an indy SwitchPoint", invalidators.size() == 1);
            SwitchPointInvalidator other = new SwitchPointInvalidator();
            other.getData();
            invalidators.add(other);

            boolean[] hookRan = {false};
            boolean[] isolated = {false};
            List<Invalidator> batch = new ArrayList<Invalidator>(invalidators) {
                @Override
                public Invalidator get(int index) {
                    if (index == 1 && !hookRan[0]) {
                        hookRan[0] = true;
                        // Schedule the first write after the child's SwitchPoint has
                        // been collected, but before the batch is invalidated. The
                        // hook runs ordinary Ruby definitions; it never replaces a
                        // SwitchPoint itself.
                        isolated[0] = runtime.evalScriptlet("""
                                CallbackChild.copy_and_write
                                own = CallbackChild.singleton_class.instance_method(:__callbacks_value)
                                fresh = own.bind_call(CallbackChild)
                                own.owner == CallbackChild.singleton_class &&
                                  fresh == {parent: true, child: true} &&
                                  CallbackBase.__callbacks == {parent: true}
                                """).isTrue();
                    }
                    return super.get(index);
                }
            };

            other.invalidateAll(batch);
            assertTrue("hook inside invalidateAll did not run", hookRan[0]);
            assertTrue("the hot reader wrote into its parent's closure value", isolated[0]);
            assertTrue("later writes or redefinition changed a sibling's inherited value",
                    runtime.evalScriptlet("""
                            CallbackChild.__callbacks = {second: true}
                            second_write = CallbackChild.__callbacks == {second: true}
                            CallbackAttribute.redefine(CallbackChild, {redefined: true})
                            second_write && CallbackChild.__callbacks == {redefined: true} &&
                              CallbackSibling.__callbacks == {parent: true} &&
                              CallbackBase.__callbacks == {parent: true}
                            """).isTrue());
        } finally {
            runtime.tearDown();
        }
    }
}
