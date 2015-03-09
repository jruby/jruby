exclude :test_shared_thread, "needs investigation"
exclude :test_shared_eval, "IR already built and possibly fully compiled non-closure scope so we cannot add new flip var nor initialize its flip state by adding instructions"
