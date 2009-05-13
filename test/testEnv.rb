require 'test/minirunit'

is_windows = RUBY_PLATFORM =~ /mswin/i || (RUBY_PLATFORM =~ /java/i && ENV_JAVA['os.name'] =~ /windows/i)

#test_check('test_bracket')
test_equal(nil, ENV['test'])
test_equal(nil, ENV['TEST'])

ENV['test'] = 'foo'

test_equal('foo', ENV['test'])
test_equal(is_windows ? 'foo' : nil, ENV['TEST'])

ENV['TEST'] = 'bar'
test_equal('bar', ENV['TEST'])
test_equal(is_windows ? 'bar' : 'foo', ENV['test'])

test_exception(TypeError) {
  tmp = ENV[1]
}

test_exception(TypeError) {
  ENV[1] = 'foo'
}

test_exception(TypeError) {
  ENV['test'] = 0
}


#test_check('has_value')
val = 'a'
val.succ! while ENV.has_value?(val) && ENV.has_value?(val.upcase)
ENV['test'] = val[0...-1]

test_equal(false, ENV.has_value?(val))
test_equal(false, ENV.has_value?(val.upcase))

ENV['test'] = val

test_equal(true, ENV.has_value?(val))
test_equal(false, ENV.has_value?(val.upcase))

ENV['test'] = val.upcase
test_equal(false, ENV.has_value?(val))
test_equal(true, ENV.has_value?(val.upcase))


#test_check('index')
val = 'a'
val.succ! while ENV.has_value?(val) && ENV.has_value?(val.upcase)
ENV['test'] = val[0...-1]

test_equal(nil, ENV.index(val))
test_equal(nil, ENV.index(val.upcase))
ENV['test'] = val
test_equal('test', ENV.index(val))

test_equal(nil, ENV.index(val.upcase))
ENV['test'] = val.upcase
test_equal(nil, ENV.index(val))
test_equal('test', ENV.index(val.upcase))

#nil values are ok (corresponding key will be deleted) 
#nil keys are not ok
test_exception(TypeError) {ENV[nil]}
test_exception(TypeError) {ENV[nil] = "foo"}

ENV['test'] = nil
test_equal(nil, ENV['test'])
test_equal(false, ENV.has_key?('test'))

name = (ENV['OS'] =~ /\AWin/i ? '%__JRUBY_T1%' : '$__JRUBY_T1')
expected = (ENV['OS'] =~ /\AWin/i ? '%__JRUBY_T1%' : '')
v = `echo #{name}`.chomp
test_equal expected,v
ENV['__JRUBY_T1'] = "abc"
v = `echo #{name}`.chomp
test_equal "abc",v

# Disabled for shell-character fixes; see JRUBY-3097
#test_equal "abc", `jruby -e "puts ENV[%{__JRUBY_T1}]"`.chomp

# JRUBY-2393
test_ok(ENV.object_id != ENV.to_hash.object_id)
