exclude :test_str_count, "does not raise compatibility error"
exclude :test_str_intern, "encoding of MBC does not matchuby#3692"
exclude :test_str_smart_chomp, "needs investigation"
if org.jruby.util.cli.Options::COMPILE_INVOKEDYNAMIC.load
  exclude :test_str_squeeze, "fails sometimes with invokedynamic enabled, https://github.com/jruby/jruby/issues/7803"
end
