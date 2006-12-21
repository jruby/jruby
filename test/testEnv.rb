require 'test/minirunit'

test_ok(ENV['HOME'] || ENV['APPDATA'], "Reading external environment")

#test_check('test_bracket')
test_equal(nil, ENV['test'])
test_equal(nil, ENV['TEST'])

ENV['test'] = 'foo'

test_equal('foo', ENV['test'])
test_equal(nil, ENV['TEST'])

ENV['TEST'] = 'bar'
test_equal('bar', ENV['TEST'])
test_equal('foo', ENV['test'])

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

#nil values are ok
#nil keys are not
test_exception(TypeError) {ENV[nil]}
test_exception(TypeError) {ENV[nil] = "foo"}

ENV['test'] = nil
test_equal(nil, ENV['test'])
