require 'test/unit'

class TestStringPrintf < Test::Unit::TestCase

  ##### binary (%b) #####
  def test_binary
    assert_equal("101", "%b" % 5)
    assert_equal("101", "%b" % "5")
    assert_equal("1011010111100110001000001111010010000000000101", "%b" % 50000000000005)
    assert_equal("101111000001010000111111101001001110001001010000111010110011000100010111110110010101010110100000000000000000000000000000000000101", "%b" % 500000000000000000000000000000000000005)
    assert_equal(" 101", "% b" % 5)
    assert_equal("-101", "% b" % -5)
    assert_equal(" -101", "% 5b" % -5)
    assert_equal("101", "%1b" % 5)
    assert_equal("00101", "%.5b" % 5)
    assert_equal("00101", "%05b" % 5)
    assert_equal("11011", "%05b" % -5)
    assert_equal("101", "%b" % 5.5)
    assert_equal("0b101", "%#b" % 5)
    assert_equal("0b..1011", "%#b" % -5)
    assert_equal("+101", "%+b" % 5)
    assert_equal("101  ", "%-5b" % 5)
    assert_equal("0", "%b" % nil)
    assert_equal("%b" % :howdy.to_i, "%b" % :howdy)
    assert_raises(ArgumentError) {"%b" % "a"}
    assert_raises(TypeError) {"%b" % true}
    assert_raises(TypeError) {"%b" % [[1, 2]]}
    assert_raises(TypeError) {"%b" % {'A' => 1}}
  end

  ##### char (%c) #####
  def test_char
    assert_equal("A", "%c" % 65)
    assert_equal("m", "%c" % 365)
    assert_equal("[", "%c" % -165)
    assert_equal("A", "% c" % 65)
    assert_equal("A", "%0c" % 65)
    assert_equal("A", "%.5c" % 65)
    assert_equal("A", "%#c" % 65)
    assert_equal("A", "%+c" % 65)
    assert_equal("    A", "%5c" % 65)
    assert_equal("    A", "%05c" % 65)
    assert_equal("A    ", "%-5c" % 65)
    assert_equal("A", "%c" % 65.8)
    assert_equal("%c" % :howdy.to_i, "%c" % :howdy)
    # FIXME: validity of test pending decision on
    # MRI vs. YARV compliance
#    assert_raises(TypeError) {"%c" % "65"}
    assert_raises(TypeError) {"%c" % true}
    assert_raises(TypeError) {"%c" % nil}
    assert_raises(TypeError) {"%c" % [[1, 2]]}
    assert_raises(TypeError) {"%c" % {'A' => 1}}
    assert_raises(RangeError) {"%c" % 500000000000000000000000000000000000005}
  end

  ##### inspect (%p) #####
  def test_inspect
    assert_equal("65", "%p" % ?A)
    assert_equal('"howdy"', "%p" % 'howdy')
    assert_equal(":howdy", "%p" % :howdy)
    assert_equal("[1, 2]", "%p" % [[1,2]])
    assert_equal('{"A"=>1}', "%p" % {'A' => 1})
    assert_equal("   65", "%5p" % ?A)
    assert_equal("   65", "%05p" % ?A)
    assert_equal("65   ", "%-5p" % ?A)
    assert_equal("  nil", "%5p" % nil)
  end

  def strangePrintf
    game = '41181 jpa:awh'
    opponent = game.scan("jpa")[0]
    sprintf "%s", opponent
  end

  def testStrangePrintf
    assert_equal('jpa', strangePrintf)
  end
end
