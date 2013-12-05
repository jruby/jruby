require File.expand_path('../spec_helper', __FILE__)
require "stringio"

load_extension("globals")

describe "CApiGlobalSpecs" do
  before :each do
    @f = CApiGlobalSpecs.new
  end

  it "correctly gets global values" do
    @f.sb_gv_get("$BLAH").should == nil
    @f.sb_gv_get("$\\").should == nil
    @f.sb_gv_get("\\").should == nil # rb_gv_get should change \ to $\
  end

  it "returns $~" do
    'a' =~ /a/
    @f.sb_gv_get("$~").to_a.should == ['a']
    @f.sb_gv_get("~").to_a.should == ['a']
  end

  it "correctly sets global values" do
    @f.sb_gv_get("$BLAH").should == nil
    @f.sb_gv_set("$BLAH", 10)
    @f.sb_gv_get("$BLAH").should == 10
  end

  it "lists all global variables" do
    @f.rb_f_global_variables.should == Kernel.global_variables
  end

  not_supported_on :rubinius, :jruby do
    it "rb_define_variable should define a new global variable" do
      @f.rb_define_variable("my_gvar", "ABC")
      $my_gvar.should == "ABC"
      $my_gvar = "XYZ"
      @f.sb_get_global_value.should == "XYZ"
    end
  end

  it "rb_define_readonly_variable should define a new readonly global variable" do
    @f.rb_define_readonly_variable("ro_gvar", 15)
    $ro_gvar.should == 15
    lambda { $ro_gvar = 10 }.should raise_error(NameError)
  end

  not_supported_on :rubinius, :jruby do
    it "rb_define_hooked_variable should define a C hooked global variable" do
      @f.rb_define_hooked_variable_2x("$hooked_gvar")
      $hooked_gvar = 2
      $hooked_gvar.should == 4
    end
  end

  ruby_version_is ""..."1.9" do
    describe "rb_set_kcode" do
      before :each do
        @kcode = $KCODE
      end

      after :each do
        $KCODE = @kcode
      end

      it "sets the value of $KCODE" do
        @f.rb_set_kcode("U")
        $KCODE.should == "UTF8"
      end
    end
  end

  describe "rb_rs" do
    before :each do
      @dollar_slash = $/
    end

    after :each do
      $/ = @dollar_slash
    end

    it "returns \\n by default" do
      @f.rb_rs.should == "\n"
    end

    it "returns the value of $/" do
      $/ = "foo"
      @f.rb_rs.should == "foo"
    end
  end

  describe "rb_default_rs" do
    it "returns \\n" do
      @f.rb_default_rs.should == "\n"
    end
  end

  describe "rb_output_rs" do
    before :each do
      @dollar_backslash = $\
    end

    after :each do
      $\ = @dollar_backslash
    end

    it "returns nil by default" do
      @f.rb_output_rs.should be_nil
    end

    it "returns the value of $\\" do
      $\ = "foo"
      @f.rb_output_rs.should == "foo"
    end
  end

  describe "rb_output_fs" do
    before :each do
      @dollar_comma = $,
    end

    after :each do
      $, = @dollar_comma
    end

    it "returns nil by default" do
      @f.rb_output_fs.should be_nil
    end

    it "returns the value of $\\" do
      $, = "foo"
      @f.rb_output_fs.should == "foo"
    end
  end

  describe "rb_lastline_set" do
    it "sets the value of $_" do
      @f.rb_lastline_set("last line")
      $_.should == "last line"
    end

    it "sets a Thread-local value" do
      $_ = nil
      running = false

      thr = Thread.new do
        @f.rb_lastline_set("last line")
        $_.should == "last line"
        running = true
      end

      Thread.pass until running
      $_.should be_nil

      thr.join
    end
  end

  describe "rb_lastline_get" do
    before do
      @io = StringIO.new("last line")
    end

    it "gets the value of $_" do
      @io.gets
      @f.rb_lastline_get.should == "last line"
    end

    it "gets a Thread-local value" do
      $_ = nil
      running = false

      thr = Thread.new do
        @io.gets
        @f.rb_lastline_get.should == "last line"
        running = true
      end

      Thread.pass until running
      $_.should be_nil

      thr.join
    end
  end
end
