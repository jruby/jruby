dir = __FILE__[0...-3]
$LOAD_PATH.unshift(dir)
require "#{dir}/openssl"
