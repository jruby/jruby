exclude :test_bug11486, "fix from https://bugs.ruby-lang.org/issues/11486 did not appear to fix it for us"
exclude :test_str_intern, "needs investigation"
exclude :test_str_count, "does not raise compatibility error"
exclude :test_str_crypt_nonstrict, "#crypt failing: Errno::EINVAL: Invalid argument"
exclude :test_str_squeeze, "fails in CI with indy, see jruby/jruby#7803" if org.jruby.util.cli.Options::COMPILE_INVOKEDYNAMIC.load
exclude :test_str_smart_chomp, "see jruby/jruby#3692"
