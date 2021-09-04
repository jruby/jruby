exclude :test_exception_overflow, "runs forever"

exclude :test_BigMath_exp_under_gc_stress, "needs investigation"
exclude :test_BigMath_log_under_gc_stress, "needs investigation"
exclude :test_div, "does not pass due precision differences (ported to test/jruby/test_big_decimal.rb)"

exclude :test_limit, "needs investigation"
exclude :test_marshal, "needs investigation"

exclude :test_power_of_three, "pow's precision isn't calculated the same as in MRI (for 1/81)"
exclude :test_power_with_Bignum, "needs investigation"
exclude :test_power_with_prec, "needs investigation"
exclude :test_power_without_prec, "needs investigation"

exclude :test_round_up, "needs investigation"
exclude :test_sqrt_bigdecimal, "needs investigation"
exclude :test_thread_local_mode, "needs investigation"
exclude :test_to_f, "needs investigation"
