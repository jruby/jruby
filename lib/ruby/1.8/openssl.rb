=begin
= $RCSfile$ -- Loader for all OpenSSL C-space and Ruby-space definitions

= Info
  'OpenSSL for Ruby 2' project
  Copyright (C) 2002  Michal Rokos <m.rokos@sh.cvut.cz>
  All rights reserved.

= Licence
  This program is licenced under the same licence as Ruby.
  (See the file 'LICENCE'.)

= Version
  $Id$
=end

# Attempt to load the gem first
begin
  require 'jruby-openssl'
rescue LoadError
  # Not available, use built-in
  require 'openssl.jar'

  require 'openssl/bn'
  require 'openssl/cipher'
  require 'openssl/config'
  require 'openssl/digest'
  require 'openssl/ssl-internal'
  require 'openssl/x509-internal'
end
