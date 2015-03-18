namespace :spec do
  # This set of tasks will run ci and retrieve the proper version of specs.
  # The specs below this section will run specs, but they will not retrieve
  # the specs they run against.  This is so we can run the similiar mspec
  # runs against a stable and head version of the rubyspecs.
  
  desc "Run rubyspecs expected to pass in interpreted mode"
  task :ruby => :ci_interpreted
  desc "Run rubyspecs expected to pass in interpreted mode"
  task :'ruby:int' => :ci_interpreted
  desc "Run rubyspecs expected to pass in compiled mode"
  task :'ruby:jit' => :ci_compiled
  desc "Run rubyspecs expected to pass in precompiled mode"
  task :'ruby:aot' => :ci_precompiled

  desc "Run fast specs that do not spawn many subprocesses"
  task :'ruby:fast' do
    mspec :compile_mode => "OFF",
          :format => 'd',
          :spec_target => ":fast",
          :jruby_opts => "--dev"
  end

  desc "Run fast specs that do not spawn many subprocesses"
  task :'ruby:fast:jit' do
    mspec :compile_mode => "JIT",
          :jit_threshold => 0,
          :format => 'd',
          :spec_target => ":fast"
  end

  desc "Run rubyspecs expected to pass"
  task :ci => ['spec:tagged']

  task :ci_interpreted => ['spec:interpreted']
  task :ci_compiled => ['spec:compiled']
  task :ci_precompiled => ['spec:precompiled']

  desc "Run all the specs including failures"
  task :ci_all => ['spec:all']

  desc "Run all the specs in precompiled mode"
  task :ci_all_precompiled => ['spec:all_precompiled']

  desc "Run rubyspecs expected to pass (against latest rubyspec version)"
  task :ci_latest => ['spec:fast_forward_to_rubyspec_head', 'spec:tagged']

  # Note: For this point below it is your reponsibility to make sure specs
  # are checked out.

  desc "Run tagged specs in interpreted, JIT, and pre-compiled modes"
  task :tagged => [:interpreted, :compiled, :precompiled] do
    fail "One or more Ruby spec runs have failed" if spec_run_error
  end

  desc "Run all specs in interpreted, JIT, and pre-compiled modes"
  task :all => [:all_interpreted, :all_compiled, :all_precompiled] do
    fail "One or more Ruby spec runs have failed" if spec_run_error
  end

  desc "Tagged specs in interpreted mode only"
  task :interpreted do
    mspec :compile_mode => "OFF",
       :format => 'd'
  end

  desc "Tagged specs in JIT mode only (threshold=0)"
  task :compiled do
    mspec :compile_mode => "JIT",
       :jit_threshold => 0, 
       :format => 'd'
  end

  desc "Tagged specs in AOT mode only"
  task :precompiled do
    mspec :compile_mode => "FORCE",
       :jit_threshold => 0, 
       :format => 'd'
  end

  desc "All specs in interpreted mode only"
  task :all_interpreted do
    mspec :compile_mode => "OFF", 
       :format => 'd'
  end

  desc "All specs in JIT mode only (threshold=0)"
  task :all_compiled_18 do
    mspec :compile_mode => "JIT", :jit_threshold => 0, 
       :format => 'd'
  end

  desc "All specs in AOT mode only"
  task :all_precompiled do
    mspec :compile_mode => "FORCE", :jit_threshold => 0, 
       :format => 'd'
  end

  # Parameterized rubyspec runs for e.g. TravisCI
  desc "Run RubySpec on Travis in interpreted mode"
  task :ci_interpreted_travis do
    mspec :compile_mode => 'OFF',
     :format => 's'
  end

  # Complimentary tasks for running specs

  task :fetch_latest_specs => [:install_build_gems, :fetch_latest_rubyspec_repo, :fetch_latest_mspec_repo]

  task :fetch_stable_specs => :install_build_gems do
    puts "Rolling rubyspec to stable version"
    git_submodule_update('spec/ruby')

    puts "Rolling mspec to stable version"
    git_submodule_update('spec/mspec')
  end
  
  task :fast_forward_to_rubyspec_head => :fetch_latest_specs do
    puts "Rolling to rubyspec to latest version"
    git_checkout('rubyspec', 'origin/HEAD', RUBYSPEC_DIR)
    git_move_to_head_detached('rubyspec', RUBYSPEC_GIT_REPO, RUBYSPEC_DIR)
  end

  desc "Retrieve latest tagged rubyspec git repository"
  task :fetch_latest_rubyspec_repo do
    unless git_repo_exists? RUBYSPEC_DIR
      clean_spec_dirs
      git_clone('rubyspec', RUBYSPEC_GIT_REPO, RUBYSPEC_DIR)
    else
      git_fetch('rubyspec', RUBYSPEC_DIR, ignore_error = true)
    end
  end

  desc "Retrieve latest tagged mspec git repository"
  task :fetch_latest_mspec_repo do
    unless git_repo_exists? MSPEC_DIR
      git_clone('mspec', MSPEC_GIT_REPO, MSPEC_DIR)
    else
      git_fetch('mspec', MSPEC_DIR, ignore_error = true)
    end
  end

  desc "Clean up spec dirs"
  task :clean_specs do
    clean_spec_dirs(true)
  end

  def clean_spec_dirs(wipe_spec_dir = false)
    rm_rf RUBYSPEC_DIR if wipe_spec_dir
    rm_rf MSPEC_DIR
    rm_f RUBYSPEC_TAR_FILE
    rm_f MSPEC_TAR_FILE
    rm_f File.join(SPEC_DIR, "rubyspecs.current.revision")
  end

  def spec_run_error
    # Obtuseriffic - If any previous spec runs were non-zero return we failed
    ['OFF', 'JIT', 'FORCE'].any? {|n| ant.properties["spec.status.#{n}"] != "0"}
  end
end
