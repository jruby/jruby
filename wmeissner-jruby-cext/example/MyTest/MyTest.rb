# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
require 'mytest.bundle'
include MyTest
puts test1
block_given

def foo
  if block_given
    rb_yield
  end
end

foo { puts "BLOCK!" }

