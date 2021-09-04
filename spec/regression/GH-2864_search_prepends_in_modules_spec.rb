# The issue here stemmed from modules with prepends not being searched like classes with prepends. Instead of searching
# up the prepend hierarchy, we only ever checked the target module's method table. When a prepend was active, this
# table is empty. The modified logic now searches all modules in hierarchy below and including the original, allowing
# it to search prepended modules-in-a-module correctly. #2864
describe "A module with prepends" do
  it "is searched as a hierarchy" do
    a = Module.new do
      def foo; 1; end
    end
    b = Module.new
    a.prepend(b)
    x = Class.new do
      include a
    end
    y = Class.new(x) do
      prepend b
    end

    expect(y.new.foo).to eq(1)
  end
end