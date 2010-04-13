require File.join(File.dirname(__FILE__), 'helpers', 'signature_parser_helper')

describe JavaSignatureParser do
  it "should parse primitive-only signatures like 'void foo()'" do
    signature('void foo()').should have_signature(VOID, 'foo', [])
    signature('byte foo()').should have_signature(BYTE, 'foo', [])
    signature('void foo(float)').should have_signature(VOID, 'foo', [FLOAT])
    signature('void foo(float bar)').should have_signature(VOID, 'foo', [FLOAT])
    signature('int foo(float, boolean)').should have_signature(INT, 'foo', [FLOAT, BOOLEAN])
  end

  it "should parse simple-non generic types like 'Set foo(String)'" do
    signature('Set foo(String)').should have_signature(:Set, 'foo', [:String])
    signature('Set foo(String, boolean)').should have_signature(:Set, 'foo', [:String, BOOLEAN])
  end

  it "should parse various array primitives like 'void foo(int[] bar)" do
    signature('void foo(int[] bar)').should have_signature(VOID, 'foo', [arrayOf(INT)])
    signature('void foo(int[][] bar)').should have_signature(VOID, 'foo', [arrayOf(arrayOf(INT))])
    signature('void foo(int bar[])').should have_signature(VOID, 'foo', [arrayOf(INT)])
    signature('void foo(int[] bar[])').should have_signature(VOID, 'foo', [arrayOf(arrayOf(INT))])
    signature('int[] foo(int bar)').should have_signature(arrayOf(INT), 'foo', [INT])
    signature('int[][] foo(int bar)').should have_signature(arrayOf(arrayOf(INT)), 'foo', [INT])
  end

  it "should parse constructor signatures like 'public Foo(int, String)'" do
    signature('Foo(int, String)').should have_constructor_signature('Foo', [INT, :String])
  end
end
