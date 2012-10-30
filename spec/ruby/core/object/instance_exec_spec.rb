require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.7" do

  require File.expand_path('../fixtures/classes', __FILE__)

  describe "Object#instance_exec" do
    it "raises a LocalJumpError unless given a block" do
      lambda { "hola".instance_exec }.should raise_error(LocalJumpError)
    end

    it "has an arity of -1" do
      Object.new.method(:instance_exec).arity.should == -1
    end

    it "accepts arguments with a block" do
      lambda { "hola".instance_exec(4, 5) { |a,b| a + b } }.should_not raise_error
    end

    it "doesn't pass self to the block as an argument" do
      "hola".instance_exec { |o| o }.should be_nil
    end

    it "passes any arguments to the block" do
      Object.new.instance_exec(1,2) {|one, two| one + two}.should == 3
    end

    it "only binds the exec to the receiver" do
      f = Object.new
      f.instance_exec do
        def foo
          1
        end
      end
      f.foo.should == 1
      lambda { Object.new.foo }.should raise_error(NoMethodError)
    end

    # TODO: This should probably be replaced with a "should behave like" that uses
    # the many scoping/binding specs from kernel/eval_spec, since most of those
    # behaviors are the same for instance_exec. See also module_eval/class_eval.

    it "binds self to the receiver" do
      s = "hola"
      (s == s.instance_exec { self }).should == true
    end
    
    it "binds the block's binding self to the receiver" do
      s = "hola"
      (s == s.instance_exec { eval "self", binding }).should == true
    end

    it "executes in the context of the receiver" do
      "Ruby-fu".instance_exec { size }.should == 7
      Object.class_eval { "Ruby-fu".instance_exec{ to_s } }.should == "Ruby-fu"
    end

    it "has access to receiver's instance variables" do
      ObjectSpecs::IVars.new.instance_exec { @secret }.should == 99
    end

    it "invokes Method objects without rebinding self" do
      3.instance_exec(4, &5.method(:+)).should == 9
    end

    ruby_version_is ""..."1.9" do
      it "sets class variables in the receiver" do
        ObjectSpecs::InstExec.class_variables.should include("@@count")
        ObjectSpecs::InstExec.send(:class_variable_get, :@@count).should == 2
      end
    end

    ruby_version_is "1.9" do
       it "sets class variables in the receiver" do
        ObjectSpecs::InstExec.class_variables.should include(:@@count)
        ObjectSpecs::InstExec.send(:class_variable_get, :@@count).should == 2
      end
    end

    it "raises a TypeError when defining methods on an immediate" do
      lambda do
        1.instance_exec { def foo; end }
      end.should raise_error(TypeError)
      lambda do
        :foo.instance_exec { def foo; end }
      end.should raise_error(TypeError)
    end

  quarantine! do # Not clean, leaves cvars lying around to break other specs
    it "scopes class var accesses in the caller when called on a Fixnum" do
      # Fixnum can take instance vars
      Fixnum.class_eval "@@__tmp_instance_exec_spec = 1"
      (defined? @@__tmp_instance_exec_spec).should == nil

      @@__tmp_instance_exec_spec = 2
      1.instance_exec { @@__tmp_instance_exec_spec }.should == 2
      Fixnum.__send__(:remove_class_variable, :@@__tmp_instance_exec_spec)
    end
  end

    it "raises a TypeError when defining methods on numerics" do
      lambda do
        (1.0).instance_exec { def foo; end }
      end.should raise_error(TypeError)
      lambda do
        (1 << 64).instance_exec { def foo; end }
      end.should raise_error(TypeError)
    end
  end
end
