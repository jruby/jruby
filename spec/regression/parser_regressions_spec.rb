require 'rspec'

describe "The Parsing experience" do
  it "parses a cond which used to get confused as a tLABEL" do
    cond = true
    x = '('
    a = cond ? x + ')': ''
    a.should eq('()')
  end

  it "parse the stabby lambda even if args params separated by space" do
    a = -> () { 1 }
    a.call.should eq(1)
  end

  it "parses a jumbled mess of nestng" do
    self.class.send(:define_method, :foo) { |a| a[:key].call[:key] }
    var = 1
    res = foo key: (proc do
                      {
                        key: "#{var}#{var}",
                        other_key: proc do
                        end
                      }
                    end)

    res.should eq("11")
  end

  it "parses a block do inside of a call arg list" do
    self.class.send(:define_method, :foo) { |a| a }
    res = foo (10.times.to_a.map do |i|
                 7 + i
               end)
    res.should eq([7,8,9,10,11,12,13,14,15,16])
  end

  it "parses a do block with magical combo of stuff before it" do
    class FFFFFFFFF
      private def f
        [].each do |i|
        end
      end
    end
    # No expect...parsing is good enough
  end

  it "parses weird embexpr bug GH #1887" do
    Class.new do
      include Module.new{
        def a
          "#{b}"
        end
  
        def c
          d.e do
          end
        end
      }

      def b
        1
      end
    end.new.a.should eq("1")
  end

  it "parses method with block in embedded hash/kwarg. GH #3085." do
    Class.new do
      def foo(r)
        yield r
      end
    end.new.foo one: proc {}, two: 1 do |s|
      s[:two].should eq(1)
    end
  end
end

