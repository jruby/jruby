require 'test/minirunit'
test_check "kernel"

test_ok(! eval("defined? some_unknown_variable"))
x = 1
test_equal(1, eval("x"))
eval("x = 2")
test_equal(2, x)
eval("unknown = 3")
test_equal(2, x)     # Make sure eval() didn't destroy locals
test_ok(! defined? unknown)
test_equal(nil, true && defined?(Bogus))

# JRUBY-117 - to_a should be public
test_equal(["to_a"], Object.public_instance_methods.grep(/to_a/))
# JRUBY-117 - remove_instance_variable should be private
test_equal(["remove_instance_variable"], Object.private_instance_methods.grep(/remove_instance_variable/))

# JRUBY-116 (Array())
class A1; def to_ary; [1]; end; end
class A2; def to_a  ; [2]; end; end
class A3; def to_ary;   3; end; end
class A4; def to_a  ;   4; end; end
class A5; def to_ary; [5]; end; def to_a  ; [:no]; end; end
class A6; def to_ary; :no; end; def to_a  ;   [6]; end; end
class A7; end
class A8; def to_ary; nil; end; end
class A9; def to_a  ; nil; end; end


test_equal([], Array(nil))
# No warning for this first case either
test_equal([1], Array(1))
test_equal([1], Array(A1.new))
test_equal([2], Array(A2.new))
test_exception(TypeError) { Array(A3.new) }
test_exception(TypeError) { Array(A4.new) }
test_equal([5], Array(A5.new))
test_exception(TypeError) { Array(A6.new) }
a = A7.new
test_equal([a], Array(a))
a = A8.new
test_equal([a], Array(a))
test_exception(TypeError) { Array(A9.new) }

test_equal(10,Integer("0xA"))
test_equal(8,Integer("010"))
test_equal(2,Integer("0b10"))

test_equal(1.0,Float("1"))
test_equal(10.0,Float("1e1"))

test_exception(ArgumentError) { Integer("abc") }
test_exception(ArgumentError) { Integer("x10") }
test_exception(ArgumentError) { Integer("xxxx10000000000000000000000000000000000000000000000000000") }

test_exception(ArgumentError) { Float("abc") }
test_exception(ArgumentError) { Float("x10") }
test_exception(ArgumentError) { Float("xxxx10000000000000000000000000000000000000000000000000000") }

# JRUBY-214 - load should call to_str on arg 0
class Foo
  def to_str
    "test/requireTarget.rb"
  end
end

test_no_exception { load Foo.new }
test_exception(TypeError) { load Object.new }


#Previously Kernel.raise, Kernel.sprintf, Kernel.iterator? & Kernel.exec were all made private
#as they were aliased rather than defined. Checking that this is no longer the case
test_exception(RuntimeError) { Kernel.raise }
test_no_exception { Kernel.sprintf "Helllo" }
test_no_exception { Kernel.iterator? }
if File.exists?("/bin/true")
  test_no_exception { Kernel.exec "/bin/true" }
end

test_no_exception {
    catch :fred do
        throw :fred
    end
}

test_exception(NameError) {
    catch :fred do
        throw :wilma
    end
}

# test that NameError is raised at the throw, not at the catch
test_no_exception {
    catch :fred do
        begin
            throw :wilma
            test_fail("NameError should have been raised")
        rescue NameError => e
            test_ok(true)
        end
    end
}

test_no_exception {
    catch :fred1 do
        catch :fred2 do
            catch :fred3 do
                throw :fred1
                test_fail("should have jumped to fred1 catch")
            end
            test_fail("should have jumped to fred1 catch")
        end
    end
}    

test_no_exception {
    catch :fred1 do
        catch :fred2 do
            catch :fred3 do
                throw :fred2
                test_fail("should have jumped to fred2 catch")
            end
        end
    end
}

test_exception(NameError) {
    catch :fred1 do
        catch :fred2 do
            catch :fred1 do
                throw :fred2
                test_fail("should have jumped to after fred2 catch")
            end
            test_fail("should have jumped to after fred2 catch")
        end
        test_ok(true)
        throw :wilma
    end
}

test_exception(NameError) {
    throw :wilma
}

test_exception(NameError) {
    catch :fred1 do
        catch :fred2 do
            catch :fred3 do
            end
        end
    end
    throw :fred2
    test_fail("catch stack should have been cleaned up")
}
