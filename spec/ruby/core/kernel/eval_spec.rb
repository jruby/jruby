require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

EvalSpecs::A.new.c

describe "Kernel#eval" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:eval)
  end

  it "is a module function" do
    Kernel.respond_to?(:eval).should == true
  end

  it "evaluates the code within" do
    eval("2 + 3").should == 5
  end

  it "evaluates within the scope of the eval" do
    EvalSpecs::A::B.name.should == "EvalSpecs::A::B"
  end

  it "evaluates such that consts are scoped to the class of the eval" do
    EvalSpecs::A::C.name.should == "EvalSpecs::A::C"
  end

  it "finds a local in an enclosing scope" do
    a = 1
    eval("a").should == 1
  end

  it "updates a local in an enclosing scope" do
    a = 1
    eval("a = 2")
    a.should == 2
  end

  it "updates a local in a surrounding block scope" do
    EvalSpecs.new.f do
      a = 1
      eval("a = 2")
      a.should == 2
    end
  end

  it "updates a local in a scope above a surrounding block scope" do
    a = 1
    EvalSpecs.new.f do
      eval("a = 2")
      a.should == 2
    end
    a.should == 2
  end

  it "updates a local in a scope above when modified in a nested block scope" do
    a = 1
    es = EvalSpecs.new
    eval("es.f { es.f { a = 2 } }")
    a.should == 2
  end

  it "finds locals in a nested eval" do
    eval('test = 10; eval("test")').should == 10
  end

  ruby_version_is ""..."1.9" do
    it "updates a local at script scope" do
      code = fixture __FILE__, "eval_locals.rb"
      ruby_exe(code).chomp.should == "2"
    end

    it "accepts a Proc object as a binding" do
      x = 1
      bind = proc {}

      eval("x", bind).should == 1
      eval("y = 2", bind)
      eval("y", bind).should == 2

      eval("z = 3")
      eval("z", bind).should == 3
    end
  end

  ruby_version_is "1.9" do
    it "does not share locals across eval scopes" do
      code = fixture __FILE__, "eval_locals.rb"
      ruby_exe(code).chomp.should == "NameError"
    end

    it "doesn't accept a Proc object as a binding" do
      x = 1
      bind = proc {}

      lambda { eval("x", bind) }.should raise_error(TypeError)
    end
  end

  it "does not make Proc locals visible to evaluated code" do
    bind = proc { inner = 4 }
    lambda { eval("inner", bind.binding) }.should raise_error(NameError)
  end

  ruby_version_is ""..."1.9" do
    it "stores all locals of nested eval bindings in the first non-eval binding" do
      non_eval = binding
      eval1 = eval("binding", non_eval)
      eval2 = eval("binding", eval1)

      # Set locals an variables depths of nested eval bindings
      eval("w = 1")
      eval("x = 2", non_eval)
      eval("y = 3", eval1)
      eval("z = 4", eval2)

      # Now read them back and show that they're all accessible via
      # the toplevel binding.
      eval("w").should == 1
      eval("x").should == 2
      eval("y").should == 3
      eval("z").should == 4
    end
  end

  ruby_version_is "1.9" do
    # This differs from the 1.8 example because 1.9 doesn't share scope across
    # sibling evals
    #
    # REWRITE ME: This obscures the real behavior of where locals are stored
    # in eval bindings.
    it "allows a binding to be captured inside an eval" do
      outer_binding = binding
      level1 = eval("binding", outer_binding)
      level2 = eval("binding", level1)

      eval("x = 2", outer_binding)
      eval("y = 3", level1)

      eval("w=1", outer_binding)
      eval("w", outer_binding).should == 1
      eval("w=1", level1).should == 1
      eval("w", level1).should == 1
      eval("w=1", level2).should == 1
      eval("w", level2).should == 1

      eval("x", outer_binding).should == 2
      eval("x=2", level1)
      eval("x", level1).should == 2
      eval("x=2", level2)
      eval("x", level2).should == 2

      eval("y=3", outer_binding)
      eval("y", outer_binding).should == 3
      eval("y=3", level1)
      eval("y", level1).should == 3
      eval("y=3", level2)
      eval("y", level2).should == 3
    end
  end

  ruby_version_is ""..."1.9" do
    it "allows a Proc invocation to terminate the eval binding chain on local creation" do
      outer_binding = binding
      proc_binding = eval("proc {binding}.call", outer_binding)
      inner_binding = eval("proc {binding}.call", proc_binding)

      eval("w = 1")

      # The proc bindings can see eval locals set above them
      eval("w", proc_binding).should == 1
      eval("w", inner_binding).should == 1

      # Show that creating the local stops at the proc because of the
      # non-eval binding introduced.
      eval("yy = 3", proc_binding)

      lambda { eval("yy") }.should raise_error(NameError)
      lambda { eval("yy", outer_binding) }.should raise_error(NameError)
      eval("yy", proc_binding).should == 3

      # Show that even though there is a non-eval binding, reading the
      # local is still possible.
      eval("yy", inner_binding).should == 3
    end

    it "can access normal locals in nested closures" do
      outer_binding = binding
      proc_binding = eval("proc {l = 5; binding}.call", outer_binding)
      inner_binding = eval("proc {k = 6; binding}.call", proc_binding)

      lambda { eval("l") }.should raise_error(NameError)
      lambda { eval("l", outer_binding) }.should raise_error(NameError)
      eval("l", proc_binding).should == 5
      eval("l", inner_binding).should == 5

      lambda { eval("k") }.should raise_error(NameError)
      lambda { eval("k", outer_binding) }.should raise_error(NameError)
      lambda { eval("k", proc_binding)  }.should raise_error(NameError)
      eval("k", inner_binding).should == 6
    end
  end

  ruby_version_is ""..."1.9" do
    it "allows creating a new class in a binding" do
      bind = proc {}
      eval "class A; end", bind.binding
      eval("A.name", bind.binding).should == "A"
    end

    it "allows creating a new class in a binding created by #eval" do
      bind = eval "binding"
      eval "class A; end", bind
      eval("A.name").should == "A"
    end
  end

  ruby_version_is "1.9" do
    it "allows creating a new class in a binding" do
      bind = proc {}
      eval("class A; end; A.name", bind.binding).should =~ /A$/
    end

    it "allows creating a new class in a binding created by #eval" do
      bind = eval "binding"
      eval("class A; end; A.name", bind).should =~ /A$/
    end
  end

  it "includes file and line information in syntax error" do
    expected = 'speccing.rb'
    lambda {
      eval('if true',TOPLEVEL_BINDING,expected)
    }.should raise_error(SyntaxError) { |e|
      e.message.should =~ /^#{expected}:1:.+/
    }
  end

  it "sets constants at the toplevel from inside a block" do
    # The class Object bit is needed to workaround some mspec oddness
    class Object
      [1].each { eval "Const = 1"}
      Const.should == 1
      remove_const :Const
    end
  end

  it "uses the filename of the binding if none is provided" do
    eval("__FILE__").should == "(eval)"
    eval("__FILE__", binding).should == __FILE__
    eval("__FILE__", binding, "success").should == "success"
    eval("eval '__FILE__', binding").should == "(eval)"
    eval("eval '__FILE__', binding", binding).should == __FILE__
    eval("eval '__FILE__', binding", binding, 'success').should == 'success'
  end

  # Found via Rubinius bug github:#149
  it "does not alter the value of __FILE__ in the binding" do
    first_time =  EvalSpecs.call_eval
    second_time = EvalSpecs.call_eval

    # This bug is seen by calling the method twice and comparing the values
    # of __FILE__ each time. If the bug is present, calling eval will set the
    # value of __FILE__ to the eval's "filename" argument.

    second_time.should_not == "(eval)"
    first_time.should == second_time
  end

  deviates_on "jruby" do
    it "can be aliased" do
      alias aliased_eval eval
      x = 2
      aliased_eval('x += 40')
      x.should == 42
    end
  end

  # See http://jira.codehaus.org/browse/JRUBY-5163
  it "uses the receiver as self inside the eval" do
    eval("self").should equal(self)
    Kernel.eval("self").should equal(Kernel)
  end

  it "does not pass the block to the method being eval'ed" do
    lambda {
      eval('KernelSpecs::EvalTest.call_yield') { "content" }
    }.should raise_error(LocalJumpError)
  end

  it "returns from the scope calling #eval when evaluating 'return'" do
    lambda { eval("return :eval") }.call.should == :eval
  end

  ruby_bug "#", "1.9" do
    # TODO: investigate this further on 1.8.7. This is one oddity:
    #
    # In a script body:
    #
    #   lambda { return }
    #     works as expected
    #
    #   def quix; yield; end
    #   lambda { quix { return } }
    #     raises a LocalJumpError

    it "unwinds through a Proc-style closure and returns from a lambda-style closure in the closure chain" do
      code = fixture __FILE__, "eval_return_with_lambda.rb"
      ruby_exe(code).chomp.should == "a,b,c,eval,f"
    end
  end

  it "raises a LocalJumpError if there is no lambda-style closure in the chain" do
    code = fixture __FILE__, "eval_return_without_lambda.rb"
    ruby_exe(code).chomp.should == "a,b,c,e,LocalJumpError,f"
  end
end
