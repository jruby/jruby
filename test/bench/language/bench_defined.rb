require 'benchmark'

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


TIMES = 10_000

puts "number (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { number }}}
puts "plus (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { plus }}}
puts "unexisting_call (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_call }}}
puts "existing_call (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_call }}}
puts "existing_call2 (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_call2 }}}
puts "existing_call3 (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_call3 }}}
puts "existing_call4 (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_call4 }}}
puts "unexisting_var (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_var }}}
puts "attr_assignment (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { attr_assignment }}}
puts "assignment (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { assignment }}}
puts "unexisting_const_method (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_const_method }}}
puts "unexisting_const (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_const }}}
puts "existing_const (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_const }}}
puts "existing_var (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_var }}}
puts "unexisting_global (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_global }}}
puts "existing_global (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_global }}}
puts "unexisting_member (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_member }}}
puts "existing_member (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_member }}}
puts "unexisting_class_var (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_class_var }}}
puts "existing_class_var (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_class_var }}}
puts "unexisting_zsuper (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_zsuper }}}
puts "unexisting_super (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_super }}}
puts "unexisting_super_with_args (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_super_with_args }}}
puts "test_existing_super (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { test_existing_super }}}
puts "existing_colon3 (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_colon3 }}}
puts "unexisting_colon3 (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_colon3 }}}
puts "class_var_assign (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { class_var_assign }}}
puts "unexisting_block_local_variable (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_block_local_variable }}}
puts "existing_block_local_variable (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_block_local_variable }}}
puts "global_assign (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { global_assign }}}
puts "unexisting_dollar_special (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_dollar_special }}}
puts "unexisting_dollar_number (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_dollar_number }}}
puts "unexisting_dollar_number2 (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { unexisting_dollar_number2 }}}
puts "existing_dollar_special (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_dollar_special }}}
puts "existing_dollar_number (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { existing_dollar_number }}}
puts "test_true (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { test_true }}}
puts "test_false (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { test_false }}}
puts "test_self (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { test_self }}}
puts "test_nil (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { test_nil }}}
puts "test_yield (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { test_yield }}}
puts "test_yield { } (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { test_yield { } }}}
puts "test_match (500 000 times):"
5.times { puts Benchmark.measure{ TIMES.times { test_match }}}
