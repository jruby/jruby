package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.RubyFloat;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.ThreadContext;

import java.math.BigDecimal;

public class TestRubyBigDecimal extends TestCase {
  public void testFormatWithLeadingPlus() {
    assertTrue(RubyBigDecimal.formatHasLeadingPlus("+"));
    assertFalse(RubyBigDecimal.formatHasLeadingPlus(" "));
    assertFalse(RubyBigDecimal.formatHasLeadingPlus("1"));
  }

  public void testFormatWithLeadingSpace() {
    assertFalse(RubyBigDecimal.formatHasLeadingSpace("+"));
    assertTrue(RubyBigDecimal.formatHasLeadingSpace(" "));
    assertFalse(RubyBigDecimal.formatHasLeadingSpace("1"));
  }

  public void testGetVpValueWithPrec19ToRaiseFloatDomainErrorExceptionForFloatNAN() {
    Ruby runtime  = Ruby.newInstance();
    ThreadContext currentContext = runtime.getCurrentContext();

    RubyBigDecimal decimalValue = new RubyBigDecimal(runtime, new BigDecimal("1"));

    RubyFloat NAN = RubyFloat.newFloat(runtime, RubyFloat.NAN);

    try {
      decimalValue.op_quo(currentContext, NAN);
    } catch (RaiseException re) {
        RubyException rubyException = re.getException();
        assertEquals(runtime.getFloatDomainError(), rubyException.getMetaClass());
    }
  }

  public void testGetVpValueWithPrec19ToRaiseFloatDomainErrorExceptionForFloatINFINITY() {
    Ruby runtime  = Ruby.newInstance();
    ThreadContext currentContext = runtime.getCurrentContext();

    RubyBigDecimal decimalValue = new RubyBigDecimal(runtime, new BigDecimal("1"));

    RubyFloat INFINITY = RubyFloat.newFloat(runtime, RubyFloat.INFINITY);

    try {
      decimalValue.op_quo(currentContext, INFINITY);
    } catch (RaiseException re) {
      RubyException rubyException = re.getException();
      assertEquals(runtime.getFloatDomainError(), rubyException.getMetaClass());
    }
  }

  public void testFormatWithFloatingPointNotation() {
    assertFalse(RubyBigDecimal.formatHasFloatingPointNotation("5E"));
    assertTrue(RubyBigDecimal.formatHasFloatingPointNotation("5F"));
    assertFalse(RubyBigDecimal.formatHasFloatingPointNotation("5"));
  }

  public void testFormatFractionalDigitGroups() {
    assertEquals(0, RubyBigDecimal.formatFractionalDigitGroups("BLAH"));
    assertEquals(0, RubyBigDecimal.formatFractionalDigitGroups("+"));
    assertEquals(0, RubyBigDecimal.formatFractionalDigitGroups("F"));
    assertEquals(2, RubyBigDecimal.formatFractionalDigitGroups("2"));
    assertEquals(0, RubyBigDecimal.formatFractionalDigitGroups("-5"));
  }
}
