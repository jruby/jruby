require File.join(File.dirname(__FILE__), 'helpers', 'signature_parser_helper')

describe JavaSignatureParser do
  it "parses primitive-only signatures like 'void foo()'" do
    signature('void foo()').should have_signature(VOID, 'foo', [])
    signature('byte foo()').should have_signature(BYTE, 'foo', [])
    signature('void foo(float)').should have_signature(VOID, 'foo', [FLOAT])
    signature('void foo(float bar)').should have_signature(VOID, 'foo', [FLOAT])
    signature('int foo(float, boolean)').should have_signature(INT, 'foo', [FLOAT, BOOLEAN])
  end

  it "parses simple-non generic types like 'Set foo(String)'" do
    signature('Set foo(String)').should have_signature(:Set, 'foo', [:String])
    signature('Set foo(String, boolean)').should have_signature(:Set, 'foo', [:String, BOOLEAN])
  end

  it "parses various array primitives like 'void foo(int[] bar)" do
    signature('void foo(int[] bar)').should have_signature(VOID, 'foo', [arrayOf(INT)])
    signature('void foo(int[][] bar)').should have_signature(VOID, 'foo', [arrayOf(arrayOf(INT))])
    signature('void foo(int bar[])').should have_signature(VOID, 'foo', [arrayOf(INT)])
    signature('void foo(int[] bar[])').should have_signature(VOID, 'foo', [arrayOf(arrayOf(INT))])
    signature('int[] foo(int bar)').should have_signature(arrayOf(INT), 'foo', [INT])
    signature('int[][] foo(int bar)').should have_signature(arrayOf(arrayOf(INT)), 'foo', [INT])
  end

  it "parses constructor signatures like 'public Foo(int, String)'" do
    signature('Foo(int, String)').should have_constructor_signature('Foo', [INT, :String])
  end

  it "parses simple marker annotations" do
    signature('@Override void foo(float)').should have_signature(['@Override'], VOID, 'foo', [FLOAT])
    # Notice this strips off syntax but semantically is still correct
    signature('@Override void foo()').should have_signature(['@Override'], VOID, 'foo', [])
  end

  it "parses simple default annotations" do
    signature('@Cook(@Porridge) void foo()').should have_signature(['@Cook(@Porridge)'], VOID, 'foo', [])
    signature('@Cook("sausage") void foo()').should have_signature(['@Cook("sausage")'], VOID, 'foo', [])
    signature('@Cook(\'s\') void foo()').should have_signature(['@Cook(\'s\')'], VOID, 'foo', [])
  end

  it "parses simple named annotations" do
    signature('@Cook(food=@Porridge) void foo()').should have_signature(['@Cook(food=@Porridge)'], VOID, 'foo', [])
    signature('@Cook(food=\'s\') void foo()').should have_signature(["@Cook(food='s')"], VOID, 'foo', [])
    signature('@Cook(food="sausage") void foo()').should have_signature(['@Cook(food="sausage")'], VOID, 'foo', [])
    signature('@Cook(food=Porridge.Cold) void foo()').should have_signature(['@Cook(food=Porridge.Cold)'], VOID, 'foo', [])
    signature('@Cook(food=Porridge.Cold, drink=@Wine) void foo()').should have_signature(['@Cook(food=Porridge.Cold, drink=@Wine)'], VOID, 'foo', [])
  end

  it "parses simple list annotations" do
    signature('@Cook(food={@Porridge}) void foo()').should have_signature(['@Cook(food={@Porridge})'], VOID, 'foo', [])
    signature('@Cook(food={}) void foo()').should have_signature(['@Cook(food={})'], VOID, 'foo', [])
    signature('@Cook(food={"apple"}) void foo()').should have_signature(['@Cook(food={"apple"})'], VOID, 'foo', [])
    signature('@Cook(food={"apple", "orange"}) void foo()').should have_signature(['@Cook(food={"apple", "orange"})'], VOID, 'foo', [])
    signature('@Cook(food={\'a\', \'b\'}) void foo()').should have_signature(['@Cook(food={\'a\', \'b\'})'], VOID, 'foo', [])
  end
end
