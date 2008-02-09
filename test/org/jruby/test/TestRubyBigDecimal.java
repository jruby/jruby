package org.jruby.test;

import java.math.BigDecimal;

import junit.framework.TestCase;
import org.jruby.RubyBigDecimal;
import org.jruby.Ruby;

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

    public void testBigDecimalHasGetValue() {
        BigDecimal bd = new BigDecimal("12333333333333333333334323535353");
        assertEquals(bd, RubyBigDecimal.newBigDecimal(Ruby.newInstance(), bd).getValue());
    }
}
