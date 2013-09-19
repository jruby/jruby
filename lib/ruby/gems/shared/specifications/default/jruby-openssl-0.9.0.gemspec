# -*- encoding: utf-8 -*-
# stub: jruby-openssl 0.9.0 ruby lib/shared

Gem::Specification.new do |s|
  s.name = "jruby-openssl"
  s.version = "0.9.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Ola Bini", "JRuby contributors"]
  s.date = "2013-09-19"
  s.description = "JRuby-OpenSSL is an add-on gem for JRuby that emulates the Ruby OpenSSL native library."
  s.email = "ola.bini@gmail.com"
  s.files = ["History.txt", "License.txt", "Mavenfile", "README.txt", "Rakefile", "TODO-1_9-support.txt", "lib/1.8/openssl.rb", "lib/1.8/openssl/bn.rb", "lib/1.8/openssl/buffering.rb", "lib/1.8/openssl/cipher.rb", "lib/1.8/openssl/config.rb", "lib/1.8/openssl/digest.rb", "lib/1.8/openssl/pkcs7.rb", "lib/1.8/openssl/ssl-internal.rb", "lib/1.8/openssl/ssl.rb", "lib/1.8/openssl/x509-internal.rb", "lib/1.8/openssl/x509.rb", "lib/1.9/openssl.rb", "lib/1.9/openssl/bn.rb", "lib/1.9/openssl/buffering.rb", "lib/1.9/openssl/cipher.rb", "lib/1.9/openssl/config.rb", "lib/1.9/openssl/digest.rb", "lib/1.9/openssl/ssl-internal.rb", "lib/1.9/openssl/ssl.rb", "lib/1.9/openssl/x509-internal.rb", "lib/1.9/openssl/x509.rb", "lib/shared/jopenssl.jar", "lib/shared/jruby-openssl.rb", "lib/shared/openssl.rb", "lib/shared/jopenssl/version.rb", "lib/shared/openssl/pkcs12.rb", "test/test_java.rb", "test/ut_eof.rb", "test/java/pkcs7_mime_enveloped.message", "test/java/pkcs7_mime_signed.message", "test/java/pkcs7_multipart_signed.message", "test/java/test_java_attribute.rb", "test/java/test_java_bio.rb", "test/java/test_java_mime.rb", "test/java/test_java_pkcs7.rb", "test/java/test_java_smime.rb"]
  s.homepage = "https://github.com/jruby/jruby"
  s.require_paths = ["lib/shared"]
  s.rubyforge_project = "jruby/jruby"
  s.rubygems_version = "2.1.4"
  s.summary = "OpenSSL add-on for JRuby"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
      s.add_development_dependency(%q<rake>, ["~> 10.1"])
      s.add_development_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
    else
      s.add_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
      s.add_dependency(%q<rake>, ["~> 10.1"])
      s.add_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
    end
  else
    s.add_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
    s.add_dependency(%q<rake>, ["~> 10.1"])
    s.add_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
  end
end
