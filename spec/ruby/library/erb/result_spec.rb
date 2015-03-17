require 'erb'
require File.expand_path('../../../spec_helper', __FILE__)

describe "ERB#result" do


  it "return the result of compiled ruby code" do
    input = <<'END'
<ul>
<% for item in list %>
  <li><%= item %>
<% end %>
</ul>
END
    expected = <<'END'
<ul>

  <li>AAA

  <li>BBB

  <li>CCC

</ul>
END
    erb = ERB.new(input)
    list = %w[AAA BBB CCC]
    actual = erb.result(binding)
    actual.should == expected
  end


  it "share local variables" do
    input = "<% var = 456 %>"
    expected = 456
    var = 123
    ERB.new(input).result(binding)
    var.should == expected
  end


  it "is not able to h() or u() unless including ERB::Util" do
    input = "<%=h '<>' %>"
    lambda {
      ERB.new(input).result()
    }.should raise_error(NameError)
  end


  it "is able to h() or u() if ERB::Util is included" do
    class MyERB1
      include ERB::Util
      def main
        input = "<%=h '<>' %>"
        return ERB.new(input).result(binding)
      end
    end
    expected = '&lt;&gt;'
    actual = MyERB1.new.main()
    actual.should == expected
  end


  it "use TOPLEVEL_BINDING if binding is not passed" do
    class MyERB2
      include ERB::Util
      def main1
        #input = "<%= binding.to_s %>"
        input = "<%= _xxx_var_ %>"
        return ERB.new(input).result()
      end
      def main2
        input = "<%=h '<>' %>"
        return ERB.new(input).result()
      end
    end

    eval '_xxx_var_ = 123', TOPLEVEL_BINDING
    expected = '123'
    MyERB2.new.main1().should == expected

    lambda {
      MyERB2.new.main2()
    }.should raise_error(NameError)
  end


  #--
  #it "does not change current $SAFE even if safe_level is specifiled at ERB#initialize" do
  #  input = "$SAFE=<%=$SAFE.inspect%>"
  #  expected = "$SAFE=2"
  #  safe_level = 2
  #  erb = ERB.new(input, safe_level)
  #  curr_safe_level = $SAFE
  #  erb.result(binding()).should == expected
  #  $SAFE.should == curr_safe_level    # BUG: $SAFE will be changed in current Rubinius
  #end
  #++


end

