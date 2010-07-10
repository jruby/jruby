# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
require 'mytest.bundle'
include MyTest
puts test1
block_given

def foo(&block)
  if block_given &block
    p rb_block_proc &block
    rb_yield &block
  end
end

foo { puts "BLOCK!" }

