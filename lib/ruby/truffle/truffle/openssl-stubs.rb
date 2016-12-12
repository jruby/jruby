dir = File.join File.dirname(__FILE__),
                File.basename(__FILE__, '.*')
$LOAD_PATH.unshift(dir)
require "#{dir}/openssl"
