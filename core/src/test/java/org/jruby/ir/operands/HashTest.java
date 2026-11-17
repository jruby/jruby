package org.jruby.ir.operands;

import junit.framework.TestCase;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.util.KeyValuePair;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HashTest extends TestCase {
    public void testKeywordMarkerSurvivesIRTransforms() {
        Hash keywords = keywordHash();

        assertTrue(((Hash) keywords.getSimplifiedOperand(Map.of(), false)).keywords);
        assertTrue(((Hash) keywords.cloneForLVarDepth(0)).keywords);
        assertTrue(((Hash) keywords.cloneForInlining(new SimpleCloneInfo(null, false))).keywords);
    }

    public void testKeywordMarkerSurvivesPersistence() {
        List<Object> encoded = new ArrayList<>();
        IRWriterEncoder writer = proxy(IRWriterEncoder.class, (proxy, method, args) -> {
            if (method.getName().equals("encode")) encoded.add(args[0]);
            return null;
        });

        new Hash(List.of(), true).encode(writer);

        assertEquals(OperandType.HASH.getCoded(), encoded.get(0));
        assertEquals(Boolean.TRUE, encoded.get(1));

        Iterator<Object> fields = encoded.subList(1, encoded.size()).iterator();
        IRReaderDecoder reader = proxy(IRReaderDecoder.class, (proxy, method, args) -> {
            if (method.getName().equals("decodeBoolean")) return fields.next();
            if (method.getName().equals("decodeInt")) return fields.next();
            return null;
        });

        assertTrue(Hash.decode(reader).keywords);
    }

    private static Hash keywordHash() {
        List<KeyValuePair<Operand, Operand>> pairs = new ArrayList<>();
        pairs.add(new KeyValuePair<>(new Fixnum(1), new LocalVariable(null, 1, 0)));
        return new Hash(pairs, true);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
