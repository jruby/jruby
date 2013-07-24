namespace :spec do
  # This set of tasks will run ci and retrieve the proper version of specs.
  # The specs below this section will run specs, but they will not retrieve
  # the specs they run against.  This is so we can run the similiar mspec
  # runs against a stable and head version of the rubyspecs.
  
  desc "Run rubyspecs expected to pass in interpreted mode (version-frozen)"
  task :ruby => :ci_interpreted_18
  desc "Run rubyspecs expected to pass in interpreted mode (version-frozen)"
  task :'ruby:int' => :ci_interpreted_18
  desc "Run rubyspecs expected to pass in interpreted 1.9 mode (version-frozen)"
  task :ruby19 => :ci_interpreted_19
  desc "Run rubyspecs expected to pass in interpreted 1.9 mode (version-frozen)"
  task :'ruby19:int' => :ci_interpreted_19
  desc "Run rubyspecs expected to pass in compiled mode (version-frozen)"
  task :'ruby:jit' => :ci_compiled_18
  desc "Run rubyspecs expected to pass in compiled mode (version-frozen)"
  task :'ruby19:jit' => :ci_compiled_19
  desc "Run rubyspecs expected to pass in precompiled mode (version-frozen)"
  task :'ruby:aot' => :ci_precompiled_18
  desc "Run rubyspecs expected to pass in precompiled mode (version-frozen)"
  task :'ruby19:aot' => :ci_precompiled_19

  desc "Run simple set of tests over both 1.8 and 1.9 modes"
  task :short => 'spec:ci_interpreted_18_19'

  desc "Run rubyspecs expected to pass (version-frozen)"
  task :ci => ['spec:tagged_18']
  task :ci_18 => :ci

  desc "Run rubyspecs expected to pass (version-frozen)"
  task :ci_19 => ['spec:tagged_19']

  task :ci_interpreted_18 => ['spec:interpreted_18']
  task :ci_interpreted_19 => ['spec:interpreted_19']
  task :ci_compiled_18 => ['spec:compiled_18']
  task :ci_compiled_19 => ['spec:compiled_19']
  task :ci_precompiled_18 => ['spec:precompiled_18']
  task :ci_precompiled_19 => ['spec:precompiled_19']

  desc "Run rubyspecs expected to pass in interpreted mode (version-frozen, both 1.8 and 1.9)"
  task :ci_interpreted_18_19 => ['spec:interpreted_18', 'spec:interpreted_19']

  desc "Run all the specs including failures (version-frozen)"
  task :ci_all => ['spec:all_18']

  desc "Run all the specs including failures (version-frozen)"
  task :ci_all_19 => ['spec:all_19']

  desc "Run all the specs in precompiled mode (version-frozen)"
  task :ci_all_precompiled_18 => ['spec:all_precompiled_18']

  desc "Run rubyspecs expected to pass (against latest rubyspec version)"
  task :ci_latest => ['spec:fast_forward_to_rubyspec_head', 'spec:tagged_18']

  desc "Run rubyspecs expected to pass (against latest rubyspec version)"
  task :ci_latest_19 => ['spec:fast_forward_to_rubyspec_head', 'spec:tagged_19']

  desc "Run optional C API rubyspecs"
  task :ci_cext => ['spec:fast_forward_to_rubyspec_head'] do
    mspec :spec_config => CEXT_MSPEC_FILE
    fail "One or more Ruby spec runs have failed" if spec_run_error
  end

  # Note: For this point below it is your reponsibility to make sure specs
  # are checked out.

  desc "Run 1.8 tagged specs in interpreted, JIT, and pre-compiled modes"
  task :tagged_18 => [:interpreted_18, :compiled_18, :precompiled_18] do
    fail "One or more Ruby spec runs have failed" if spec_run_error
  end

  desc "Run all 1.8 specs in interpreted, JIT, and pre-compiled modes"
  task :all_18 => [:all_interpreted_18, :all_compiled_18, :all_precompiled_18] do
    fail "One or more Ruby spec runs have failed" if spec_run_error
  end

  desc "Run 1.9 tagged specs in interpreted, JIT, and pre-compiled modes"
  task :tagged_19 => [:interpreted_19, :compiled_19, :precompiled_19] do
    fail "One or more Ruby spec runs have failed" if spec_run_error
  end

  desc "Run all 1.9 specs in interpreted, JIT, and pre-compiled modes"
  task :all_19 => [:all_interpreted_19, :all_compiled_19, :all_precompiled_19] do
    fail "One or more Ruby spec runs have failed" if spec_run_error
  end

  desc "Tagged 1.8 specs in interpreted mode only"
  task :interpreted_18 do
    mspec :compile_mode => "OFF", :spec_config => RUBY18_MSPEC_FILE, 
       :compat => "1.8", :format => 'd'
  end

  desc "Tagged 1.8 specs in interpreted (IR) mode only"
  task :interpreted_ir_18 do
    mspec :compile_mode => "OFFIR", :spec_config => RUBY18_MSPEC_FILE, 
       :compat => "1.8", :format => 'd'
  end

  desc "Tagged 1.8 specs in JIT mode only (threshold=0)"
  task :compiled_18 do
    mspec :compile_mode => "JIT", :spec_config => RUBY18_MSPEC_FILE, 
       :jit_threshold => 0, 
       :compat => "1.8", :format => 'd'
  end

  desc "Tagged 1.8 specs in AOT mode only"
  task :precompiled_18 do
    mspec :compile_mode => "FORCE", :spec_config => RUBY18_MSPEC_FILE, 
       :jit_threshold => 0, 
       :compat => "1.8", :format => 'd'
  end

  desc "All 1.8 specs in interpreted mode only"
  task :all_interpreted_18 do
    mspec :compile_mode => "OFF", 
       :compat => "1.8", :format => 'd'
  end

  desc "All 1.8 specs in interpreted IR mode only"
  task :all_interpreted_ir_18 do
    mspec :compile_mode => "OFFIR", 
       :compat => "1.8", :format => 'd'
  end

  desc "All 1.8 specs in JIT mode only (threshold=0)"
  task :all_compiled_18 do
    mspec :compile_mode => "JIT", :jit_threshold => 0, 
       :compat => "1.8", :format => 'd'
  end

  desc "All 1.8 specs in AOT mode only"
  task :all_precompiled_18 do
    mspec :compile_mode => "FORCE", :jit_threshold => 0, 
       :compat => "1.8", :format => 'd'
  end

  desc "Tagged 1.9 specs in interpreted mode only"
  task :interpreted_19 do
    mspec :compile_mode => "OFF", :spec_config => RUBY19_MSPEC_FILE, 
       :compat => "1.9", :format => 'd'
  end

  desc "Tagged 1.9 specs in JIT mode only (threshold=0)"
  task :compiled_19 do
    mspec :compile_mode => "JIT", :spec_config => RUBY19_MSPEC_FILE, 
       :jit_threshold => 0, :compat => "1.9", :format => 'd'
  end

  desc "Tagged 1.9 specs in AOT mode only"
  task :precompiled_19 do
    mspec :compile_mode => "FORCE", :spec_config => RUBY19_MSPEC_FILE, 
       :jit_threshold => 0, :compat => "1.9", :format => 'd'
  end

  desc "All 1.9 specs in interpreted mode only"
  task :all_interpreted_19 do
    mspec :compile_mode => "OFF", :compat => "1.9", :format => 'd'
  end

  desc "All 1.9 specs in interpreted IR mode only"
  task :all_interpreted_ir_19 do
    mspec :compile_mode => "OFFIR", :compat => "1.9", :format => 'd'
  end

  desc "All 1.9 specs in JIT mode only (threshold=0)"
  task :all_compiled_19 do
    mspec :compile_mode => "JIT", :jit_threshold => 0, :compat => "1.9", :format => 'd'
  end

  # Parameterized rubyspec runs for e.g. TravisCI
  desc "Run RubySpec in interpreted mode under the language compat version ENV['RUBYSPEC_LANG_VER']"
  task :ci_interpreted_via_env do
    ENV['RUBYSPEC_LANG_VER'] ||= '1.9'
    case
    when ENV['RUBYSPEC_LANG_VER'] == '1.8'
      spec_config_file = RUBY18_MSPEC_FILE
    else
      spec_config_file = RUBY19_MSPEC_FILE
    end
    mspec :compile_mode => 'OFF', :compat => ENV['RUBYSPEC_LANG_VER'],
      :spec_config => spec_config_file, :format => 's'
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
