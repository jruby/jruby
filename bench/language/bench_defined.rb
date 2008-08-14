require 'benchmark'

class BenchDefined
  def foo
    nil
  end
  def foo=(val)
    val
  end

  def number
    # 50 times
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
    defined?(1); defined?(1); defined?(1); defined?(1); defined?(1); 
  end
  def plus
    # 50 times
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
    defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1); defined?(1+1);
  end
  def unexisting_call
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
    defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); defined?(gegege()); 
  end
  def existing_call
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
    defined?(number); defined?(number); defined?(number); defined?(number); defined?(number); 
  end
  def existing_call2
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
    defined?(number()); defined?(number()); defined?(number()); defined?(number()); defined?(number()); 
  end
  def existing_call3
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
    defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); defined?(self.number()); 
  end
  def existing_call4
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
    defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); defined?(self.number); 
  end
  def unexisting_var
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
    defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); defined?(gegege); 
  end
  def attr_assignment
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
    defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); defined?(foo = 1+1); 
  end
  def assignment
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
    defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); defined?(foo2 = 1+1); 
  end
  def unexisting_const_method
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
    defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); defined?(A.bar("haha")); 
  end
  def unexisting_const
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
    defined?(A); defined?(A); defined?(A); defined?(A); defined?(A); 
  end
  module B;end
  def existing_const
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
    defined?(B); defined?(B); defined?(B); defined?(B); defined?(B); 
  end
  def existing_var
    v = 1
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
    defined?(v); defined?(v); defined?(v); defined?(v); defined?(v); 
  end
  def unexisting_global
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
    defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); defined?($UNEXISTING_GLOBAL); 
  end
  def existing_global
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
    defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); defined?($stderr); 
  end
  def unexisting_member
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
    defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); defined?(@unexisting); 
  end
  def existing_member
    @foo1 = 1
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
    defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); defined?(@foo1); 
  end
  def unexisting_class_var
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
    defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); defined?(@@unexisting); 
  end
  def existing_class_var
    @@foo1 = 1
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
    defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); defined?(@@foo1); 
  end
  def unexisting_zsuper
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
    defined?(super); defined?(super); defined?(super); defined?(super); defined?(super); 
  end
  def unexisting_super
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
    defined?(super()); defined?(super()); defined?(super()); defined?(super()); defined?(super()); 
  end
  def unexisting_super_with_args
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
    defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); defined?(super(1,2,3)); 
  end
  class TestExistingSuper
    def initialize(*args)
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
      @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))];   @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]; 
    end
    def val; @val; end
  end
  def test_existing_super
    TestExistingSuper.new.val
  end
  def existing_colon3
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
    defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); defined?(File::Stat); 
  end
  def unexisting_colon3
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
    defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); defined?(File::State); 
  end
  def class_var_assign
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
    defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); defined?(@@a = 123); 
  end
  def unexisting_block_local_variable
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
    proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; proc{ defined?(a) }.call; 
  end
  def existing_block_local_variable
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
    proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; proc{ aaa=1; defined?(aaa) }.call; 
  end
  def global_assign
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
    defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); defined?($global_assign = 1); 
  end
  def unexisting_dollar_special
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
  end
  def unexisting_dollar_number
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
  end
  def unexisting_dollar_number2
    /a/=~"a"
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
  end
  def existing_dollar_special
    /a/=~"a"
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
    defined?($`); defined?($`); defined?($`); defined?($`); defined?($`); 
  end
  def existing_dollar_number
    /(a)/=~"a"
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
    defined?($1); defined?($1); defined?($1); defined?($1); defined?($1); 
  end
  def test_true
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
    defined?(true); defined?(true); defined?(true); defined?(true); defined?(true); 
  end
  def test_false
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
    defined?(false); defined?(false); defined?(false); defined?(false); defined?(false); 
  end
  def test_self
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
    defined?(self); defined?(self); defined?(self); defined?(self); defined?(self); 
  end
  def test_nil
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
    defined?(nil); defined?(nil); defined?(nil); defined?(nil); defined?(nil); 
  end
  def test_yield
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
    defined?(yield); defined?(yield); defined?(yield); defined?(yield); defined?(yield); 
  end
  def test_match
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
    defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); defined?(/a/=~"a"); 
  end
end

def bench_defined(bm)
  times = 10_000
  bd = BenchDefined.new

  bm.report "number (500 000 times):" do
    times.times { bd.number }
  end
  bm.report "plus (500 000 times):" do
    times.times { bd.plus }
  end
  bm.report "unexisting_call (500 000 times):" do
    times.times { bd.unexisting_call }
  end
  bm.report "existing_call (500 000 times):" do
    times.times { bd.existing_call }
  end
  bm.report "existing_call2 (500 000 times):" do
    times.times { bd.existing_call2 }
  end
  bm.report "existing_call3 (500 000 times):" do
    times.times { bd.existing_call3 }
  end
  bm.report "existing_call4 (500 000 times):" do
    times.times { bd.existing_call4 }
  end
  bm.report "unexisting_var (500 000 times):" do
    times.times { bd.unexisting_var }
  end
  bm.report "attr_assignment (500 000 times):" do
    times.times { bd.attr_assignment }
  end
  bm.report "assignment (500 000 times):" do
    times.times { bd.assignment }
  end
  bm.report "unexisting_const_method (500 000 times):" do
    times.times { bd.unexisting_const_method }
  end
  bm.report "unexisting_const (500 000 times):" do
    times.times { bd.unexisting_const }
  end
  bm.report "existing_const (500 000 times):" do
    times.times { bd.existing_const }
  end
  bm.report "existing_var (500 000 times):" do
    times.times { bd.existing_var }
  end
  bm.report "unexisting_global (500 000 times):" do
    times.times { bd.unexisting_global }
  end
  bm.report "existing_global (500 000 times):" do
    times.times { bd.existing_global }
  end
  bm.report "unexisting_member (500 000 times):" do
    times.times { bd.unexisting_member }
  end
  bm.report "existing_member (500 000 times):" do
    times.times { bd.existing_member }
  end
  bm.report "unexisting_class_var (500 000 times):" do
    times.times { bd.unexisting_class_var }
  end
  bm.report "existing_class_var (500 000 times):" do
    times.times { bd.existing_class_var }
  end
  bm.report "unexisting_zsuper (500 000 times):" do
    times.times { bd.unexisting_zsuper }
  end
  bm.report "unexisting_super (500 000 times):" do
    times.times { bd.unexisting_super }
  end
  bm.report "unexisting_super_with_args (500 000 times):" do
    times.times { bd.unexisting_super_with_args }
  end
  bm.report "test_existing_super (500 000 times):" do
    times.times { bd.test_existing_super }
  end
  bm.report "existing_colon3 (500 000 times):" do
    times.times { bd.existing_colon3 }
  end
  bm.report "unexisting_colon3 (500 000 times):" do
    times.times { bd.unexisting_colon3 }
  end
  bm.report "class_var_assign (500 000 times):" do
    times.times { bd.class_var_assign }
  end
  bm.report "unexisting_block_local_variable (500 000 times):" do
    times.times { bd.unexisting_block_local_variable }
  end
  bm.report "existing_block_local_variable (500 000 times):" do
    times.times { bd.existing_block_local_variable }
  end
  bm.report "global_assign (500 000 times):" do
    times.times { bd.global_assign }
  end
  bm.report "unexisting_dollar_special (500 000 times):" do
    times.times { bd.unexisting_dollar_special }
  end
  bm.report "unexisting_dollar_number (500 000 times):" do
    times.times { bd.unexisting_dollar_number }
  end
  bm.report "unexisting_dollar_number2 (500 000 times):" do
    times.times { bd.unexisting_dollar_number2 }
  end
  bm.report "existing_dollar_special (500 000 times):" do
    times.times { bd.existing_dollar_special }
  end
  bm.report "existing_dollar_number (500 000 times):" do
    times.times { bd.existing_dollar_number }
  end
  bm.report "test_true (500 000 times):" do
    times.times { bd.test_true }
  end
  bm.report "test_false (500 000 times):" do
    times.times { bd.test_false }
  end
  bm.report "test_self (500 000 times):" do
    times.times { bd.test_self }
  end
  bm.report "test_nil (500 000 times):" do
    times.times { bd.test_nil }
  end
  bm.report "test_yield (500 000 times):" do
    times.times { bd.test_yield }
  end
  bm.report "test_yield { } (500 000 times):" do
    times.times { bd.test_yield { } }
  end
  bm.report "test_match (500 000 times):" do
    times.times { bd.test_match }
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_defined(bm)} }
end
