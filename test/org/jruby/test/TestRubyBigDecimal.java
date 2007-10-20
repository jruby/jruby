package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.RubyBigDecimal;

public class TestRubyBigDecimal extends TestCase {
  public void testFormatWithLeadingPlus() {
    assertTrue(RubyBigDecimal.formatWithLeadingPlus("+"));
    assertFalse(RubyBigDecimal.formatWithLeadingPlus(" "));
    assertFalse(RubyBigDecimal.formatWithLeadingPlus("1"));
  }

  public void testFormatWithLeadingSpace() {
    assertFalse(RubyBigDecimal.formatWithLeadingSpace("+"));
    assertTrue(RubyBigDecimal.formatWithLeadingSpace(" "));
    assertFalse(RubyBigDecimal.formatWithLeadingSpace("1"));
  }

  public void testFormatWithFloatingPointNotation() {
    assertFalse(RubyBigDecimal.formatWithFloatingPointNotation("5E"));
    assertTrue(RubyBigDecimal.formatWithFloatingPointNotation("5F"));
    assertFalse(RubyBigDecimal.formatWithFloatingPointNotation("5"));
  }

  public void testFormatFractionalDigitGroups() {
    assertEquals(0, RubyBigDecimal.formatFractionalDigitGroups("BLAH"));
    assertEquals(0, RubyBigDecimal.formatFractionalDigitGroups("+"));
    assertEquals(0, RubyBigDecimal.formatFractionalDigitGroups("F"));
    assertEquals(2, RubyBigDecimal.formatFractionalDigitGroups("2"));
    assertEquals(0, RubyBigDecimal.formatFractionalDigitGroups("-5"));
  }
}
