# frozen_string_literal: true

require 'set'

# Boot - Reusable Bootloader with Real Bit Vector Logic (v0.6)
#
# - Thread-local integer bit vectors
# - Explicit bitwise operations (set, clear, test, toggle)
# - Feature flag support
# - Dynamic arrays + hotload + StringIO
# - load_directory with rich extension filtering & conditional dispatch
# - Advanced version system with auto-detection (directory + file header/shebang)
# - Unified Boot.load entry point (flexible input + smart block filter)
# - Boot::Config + error handling + ByteClass dispatch

module Boot
  VERSION = '0.6.0'

  $boot_loaded   ||= Set.new
  $boot_features ||= Set.new
  $boot_versions ||= []

  @bit_registry      = {}
  @next_bit_position = 0
  @registry_mutex    = Mutex.new

  # ============================================================
  # VERSION CONSTANTS (defaults — can be overridden via config)
  # ============================================================
  CURRENT_VERSION = '0.5.1'
  MIN_VERSION     = '0.4.0'

  $boot_version_order  ||= []
  $boot_active_version ||= nil

  # ============================================================
  # Boot::Config — Central Configuration (Struct + extended key support)
  # ============================================================
  Config = Struct.new(
    :min_version,
    :auto_version,
    :quiet,
    :on_error,
    keyword_init: true
  ) do
    def self.defaults
      new(
        min_version:  MIN_VERSION,
        auto_version: false,
        quiet:        false,
        on_error:     :warn
      )
    end

    # Extended key-value store for keys not in the Struct
    @extended = {}

    class << self
      def extended
        @extended
      end

      def config_set(key, value)
        key = key.to_sym
        if members.include?(key)
          # Direct struct field
          instance = Boot.instance_variable_get(:@config)
          instance[key] = value if instance
        else
          @extended[key] = value
        end
      end

      def config_get(key)
        key = key.to_sym
        if members.include?(key)
          instance = Boot.instance_variable_get(:@config)
          instance ? instance[key] : nil
        else
          @extended[key]
        end
      end
    end

    def to_h
      super.compact.merge(self.class.extended)
    end
  end

  @config = Config.defaults

  def self.config
    @config
  end

  def self.on_error=(handler)
    @config.on_error = handler
  end

  # ============================================================
  # ERROR HANDLING
  # ============================================================

  def self.handle_error(error, context = {})
    case @config.on_error
    in :raise               then raise error
    in :warn                then warn "[Boot] #{error.class}: #{error.message} | #{context}"
    in :quiet               then nil
    in Proc => handler      then handler.call(error, context)
    else                         warn "[Boot] Unhandled error: #{error.message}"
    end

    false
  end

  # ============================================================
  # Boot::ByteClass (with deconstruct_keys for pattern matching)
  # ============================================================

  ByteClass = Struct.new(:kind, :confidence, :metadata, keyword_init: true) do
    def to_s
      "##{kind} (#{(confidence * 100).round}%)"
    end

    def skip?
      confidence < 0.5 || kind == :deprecated
    end

    def loadable?
      !skip?
    end

    # Enables `in { kind:, confidence: }` pattern matching
    def deconstruct_keys(keys)
      { kind: kind, confidence: confidence, metadata: metadata }
    end
  end

  # ============================================================
  # BYTE DISPATCHER (rich kinds + accurate path checking)
  # ============================================================

  def self.byte_dispatch(path)
    return ByteClass.new(kind: :unknown, confidence: 0.0) unless File.exist?(path)

    ext      = File.extname(path).downcase
    basename = File.basename(path).downcase
    fullpath = File.expand_path(path).downcase

    case
    # === Ruby Source ===
    when ext == ".rb" && basename.include?("syscall")
      ByteClass.new(kind: :syscall,        confidence: 0.95)
    when ext == ".rb" && basename.match?(/init|boot/)
      ByteClass.new(kind: :kernel_module,  confidence: 0.90)
    when ext == ".rb"
      ByteClass.new(kind: :ruby_source,    confidence: 0.85)

    # === Native ===
    when ext == ".so"
      ByteClass.new(kind: :native_extension, confidence: 1.0)

    # === Config ===
    when ext == ".json"
      ByteClass.new(kind: :config_json,    confidence: 0.95)
    when ext == ".yaml" || ext == ".yml"
      ByteClass.new(kind: :config_yaml,    confidence: 0.95)
    when ext == ".conf" || ext == ".cfg"
      ByteClass.new(kind: :config_file,    confidence: 0.90)
    when basename.match?(/params|settings|defaults/)
      ByteClass.new(kind: :config_file,    confidence: 0.85)

    # === Documentation ===
    when ext == ".md"
      ByteClass.new(kind: :documentation,  confidence: 0.95)
    when ext == ".txt"
      ByteClass.new(kind: :text_file,      confidence: 0.80)

    # === Kernel / HAL / Low-level ===
    when fullpath.include?("/hal/")
      ByteClass.new(kind: :hal_driver,          confidence: 0.92)
    when basename.match?(/interrupt|driver/)
      ByteClass.new(kind: :hal_driver,          confidence: 0.88)
    when fullpath.include?("/fs/")
      ByteClass.new(kind: :fs_driver,           confidence: 0.90)
    when basename.match?(/inode/)
      ByteClass.new(kind: :fs_driver,           confidence: 0.85)
    when fullpath.include?("/ipc/")
      ByteClass.new(kind: :ipc_message,         confidence: 0.90)
    when basename.match?(/message|queue/)
      ByteClass.new(kind: :ipc_message,         confidence: 0.85)
    when fullpath.include?("/mm/")
      ByteClass.new(kind: :memory_management,   confidence: 0.90)
    when basename.match?(/memory|page/)
      ByteClass.new(kind: :memory_management,   confidence: 0.83)

    # === Deprecated ===
    when basename.match?(/deprecated|legacy|old/)
      ByteClass.new(kind: :deprecated,     confidence: 0.70)
    when fullpath.match?(%r{/0\.[0-3]\.})
      ByteClass.new(kind: :deprecated,     confidence: 0.60)

    # === Executables ===
    when ext == ".sh"
      ByteClass.new(kind: :shell_script,   confidence: 0.90)
    when ext == "" && File.executable?(path)
      ByteClass.new(kind: :binary,         confidence: 0.75)

    # === Tests ===
    when basename.match?(/spec|test/)
      ByteClass.new(kind: :test_file,      confidence: 0.80)

    # === Fallback ===
    else
      ByteClass.new(kind: :unknown,        confidence: 0.30)
    end
  end

  class << self
    # ============================================================
    # REGISTRATION
    # ============================================================

    def register(name)
      key = name.to_sym
      @registry_mutex.synchronize do
        return @bit_registry[key] if @bit_registry.key?(key)
        pos = @next_bit_position
        @bit_registry[key] = pos
        @next_bit_position += 1
        pos
      end
    end

    def bit_position(name)
      @bit_registry[name.to_sym]
    end

    # ============================================================
    # THREAD-LOCAL BIT VECTOR
    # ============================================================

    def current_vector
      tc = Thread.current
      tc[:boot_bit_vector] ||= 0
    end

    # ============================================================
    # EXPLICIT BITWISE OPERATIONS
    # ============================================================

    def set_bit(name)
      pos = register(name)
      Thread.current[:boot_bit_vector] = current_vector | (1 << pos)
      mark_array(name)
    end

    def clear_bit(name)
      pos = bit_position(name)
      return false unless pos
      Thread.current[:boot_bit_vector] = current_vector & ~(1 << pos)
      invalidate_array(name)
      true
    end

    def bit_set?(name)
      pos = bit_position(name)
      return false unless pos
      (current_vector & (1 << pos)) != 0
    end

    def toggle_bit(name)
      if bit_set?(name)
        clear_bit(name)
      else
        set_bit(name)
      end
    end

    # Raw bitwise operations (by bit position)
    def set_bit_raw(pos)
      tc = Thread.current
      tc[:boot_bit_vector] = (tc[:boot_bit_vector] || 0) | (1 << pos)
    end

    def clear_bit_raw(pos)
      tc = Thread.current
      tc[:boot_bit_vector] = (tc[:boot_bit_vector] || 0) & ~(1 << pos)
    end

    def test_bit_raw(pos)
      tc = Thread.current
      ((tc[:boot_bit_vector] || 0) & (1 << pos)) != 0
    end

    # ============================================================
    # FEATURE FLAG HELPERS
    # ============================================================

    def enabled_features
      @bit_registry.keys.select { |k| bit_set?(k) }
    end

    def loaded?(name)
      bit_set?(name)
    end

    def mark(name)
      set_bit(name)
    end

    def invalidate(name)
      clear_bit(name)
    end

    # ============================================================
    # DYNAMIC ARRAYS (kept in sync)
    # ============================================================

    def mark_array(name)
      str = name.to_s
      $boot_loaded.add(str)
      $boot_features.add(str)
    end

    def invalidate_array(name)
      str = name.to_s
      $boot_loaded.delete(str)
      $boot_features.delete(str)
    end

    # ============================================================
    # LOADING METHODS
    # ============================================================

    def load_files(paths)
      paths.map { |p| [p, load(p)] }
    end

    def load(path, version: nil)
      puts "  [Boot] load: #{path}"
      begin
        require path
        mark(File.basename(path, '.rb'))
        $boot_versions << { file: path, version: version } if version
        true
      rescue => e
        handle_error(e, { path: path, version: version })
      end
    end

    def load_stringio(stringio, name:)
      return false if loaded?(name)
      content = stringio.read
      eval(content, TOPLEVEL_BINDING, name.to_s, 1)
      mark(name)
      true
    rescue => e
      handle_error(e, { name: name })
      false
    end

    def hotload(path)
      name = File.basename(path, '.rb').to_sym
      puts "  [Boot] HOTLOAD: #{path}"
      invalidate(name)
      load(path)
    end

    def load_versioned(entries)
      entries.map do |e|
        { path: e[:path], loaded: load(e[:path], version: e[:version]) }
      end
    end

    # ============================================================
    # UNIFIED ENTRY POINT: Boot.load
    # ============================================================

    def load(target, recursive: false, auto_version: false, &block)
      case target
      in String then load_single(target, recursive: recursive, auto_version: auto_version, &block)
      in Array  then load_multi(target,  recursive: recursive, auto_version: auto_version, &block)
      end
    end

    def load_single(target, recursive:, auto_version:, &block)
      path, version = resolve_target(target, auto_version: auto_version)

      if File.directory?(path)
        load_directory(path, recursive: recursive, auto_version: auto_version, filter: block)
      else
        return if block && !block.call(target)
        dispatch(path, version: version)
      end
    end

    def load_multi(targets, recursive:, auto_version:, &block)
      targets.each do |target|
        load_single(target, recursive: recursive, auto_version: auto_version, &block)
      end
    end

    def resolve_target(target, auto_version:)
      if target =~ /kest[oó]w?v[\._-]?(\d+\.\d+(?:\.\d+)?)/i
        version = $1
        path    = locate_versioned_root(version)
        return [path, version]
      end

      version = nil
      if auto_version
        version = extract_version_from_file_header(target) || extract_version_from_path(target)
      end

      [target, version]
    end

    def locate_versioned_root(version)
      candidates = ["Kestówv#{version}", "Kestowv#{version}", "kestowv-#{version}", "kestowv_#{version}"]
      candidates.find { |c| File.directory?(c) } || "Kestówv#{version}"
    end

    # ============================================================
    # load_directory (now supports auto_version)
    # ============================================================

    def load_directory(dir,
                        recursive:    false,
                        extensions:   nil,
                        pattern:      nil,
                        filter:       nil,
                        on_load:      nil,
                        on_skip:      nil,
                        auto_version: false,
                        &block)

      filter ||= block

      files = collect_files(dir, recursive: recursive, pattern: pattern,
                            extensions: normalize_extensions(extensions))

      files.each do |path|
        if filter && !filter.call(path)
          on_skip&.call(path)
          next
        end

        version = nil
        if auto_version
          version = extract_version_from_file_header(path) || extract_version_from_path(path)
        end

        dispatch(path, version: version)
        on_load&.call(path)
      end
    end

    def collect_files(dir, recursive: false, pattern: nil, extensions: nil)
      glob = recursive ? File.join(dir, "**/*") : File.join(dir, "*")
      files = Dir.glob(glob).select { |f| File.file?(f) }

      if extensions
        files.select! { |f| extensions.include?(File.extname(f)) }
      end

      if pattern
        files.select! { |f| File.fnmatch(pattern, f) || File.fnmatch(pattern, File.basename(f)) }
      end

      files
    end

    def normalize_extensions(ext)
      case ext
      in Array  then ext.map { |e| e.start_with?(".") ? e : ".#{e}" }
      in /[*?{]/ then ext
      in String then [ext.start_with?(".") ? ext : ".#{ext}"]
      in nil    then nil
      end
    end

    # ============================================================
    # CENTRAL DISPATCHER (uses new ByteClass logic)
    # ============================================================

    def dispatch(path, version: nil)
      bc = byte_dispatch(path)

      if bc.skip?
        if bc.kind == :deprecated
          warn "[Boot] Skipping deprecated: #{path}" unless config.quiet
          mark("skipped_deprecated_#{File.basename(path)}")
        else
          mark("unclassified_#{File.extname(path)}")
        end
        return false
      end

      case bc.kind
      when :ruby_source, :kernel_module, :syscall,
           :hal_driver, :fs_driver, :ipc_message, :memory_management
        safe_require(path, version: version)
      when :native_extension
        safe_require(path, version: version)
      when :config_json
        load_json(path)
      when :config_yaml
        load_yaml(path)
      when :config_file, :documentation, :text_file
        load_text(path)
      when :test_file
        warn "[Boot] Skipping test file at boot: #{path}" unless config.quiet
        false
      else
        mark("unclassified_#{File.extname(path)}")
        false
      end
    rescue => e
      handle_error(e, { path: path, version: version, kind: bc&.kind })
    end

    def load_text(path)
      content = File.read(path)
      mark(File.basename(path))
      content
    end

    def load_json(path)
      require 'json' unless defined?(JSON)
      data = JSON.parse(File.read(path))
      mark(File.basename(path, '.json'))
      data
    end

    def safe_require(path, version: nil)
      require path
    rescue => e
      handle_error(e, { path: path, version: version })
    end

    # ============================================================
    # ADVANCED VERSION SYSTEM
    # ============================================================

    def current_version
      CURRENT_VERSION
    end

    def set_current_version(ver)
      $boot_active_version = ver.to_s
    end

    def activate_version(ver)
      $boot_active_version = ver.to_s
      set_bit("version_#{ver}")
    end

    def version_active?(ver)
      $boot_active_version == ver.to_s || bit_set?("version_#{ver}".to_sym)
    end

    def extract_version_from_file_header(path)
      return nil unless File.exist?(path)

      File.open(path, "r") do |f|
        5.times do
          line = f.gets
          break unless line
          return $1 if line =~ /#!.*kestowv.*version[:\s=]+(\d+\.\d+(?:\.\d+)?)/i
          return $1 if line =~ /version[:\s=]+["']?(\d+\.\d+(?:\.\d+)?)["']?/i
        end
      end
      nil
    rescue
      nil
    end

    def extract_version_from_path(path)
      path.split("/").reverse_each do |part|
        return $1 if part =~ /kest[oó]w?v[\._-]?(\d+\.\d+(?:\.\d+)?)/i
        return $1 if part =~ /\A(\d+\.\d+(?:\.\d+)?)\z/
      end
      nil
    end

    def load_versioned_smart(entries, current: CURRENT_VERSION)
      set_current_version(current)

      ordered = generate_version_order(entries, current: current)

      ordered.each do |entry|
        result = load(entry[:path], version: entry[:version])

        $boot_version_order << {
          path:             entry[:path],
          detected_version: entry[:version],
          loaded:           result,
          active:           entry[:version] == current
        }
      end

      $boot_version_order
    end

    def generate_version_order(entries, current: CURRENT_VERSION)
      current_prefix = current.split(".").first(2).join(".")

      local_first = []
      current_ver = []
      older       = []

      entries.each do |entry|
        path           = entry[:path]
        header_version = extract_version_from_file_header(path)
        dir_version    = extract_version_from_path(path)
        final_version  = header_version || entry[:version] || dir_version || current

        resolved = entry.merge(version: final_version)

        case path
        when /\/(local|bin)\//
          local_first << resolved
        else
          next if Gem::Version.new(final_version) < Gem::Version.new(MIN_VERSION)

          if final_version == current || final_version.start_with?(current_prefix)
            current_ver << resolved
          else
            older << resolved
          end
        end
      end

      local_first + current_ver + older
    end

    # ============================================================
    # INTROSPECTION
    # ============================================================

    def to_a
      $boot_loaded.to_a
    end

    def features
      $boot_features.to_a
    end

    def bit_stats
      {
        registered_features: @bit_registry.size,
        bits_set_in_current_thread: current_vector.to_s(2).count('1')
      }
    end
  end
end
