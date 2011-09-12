package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.ext.bigdecimal.RubyBigDecimal;

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
}
