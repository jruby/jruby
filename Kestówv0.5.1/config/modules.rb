# frozen_string_literal: true

# Kestówv 0.5.1 — config/modules.rb
#
# Module registry with:
# - Auto-discovery via Boot.load_directory + byte_dispatch
# - Explicit dependency declarations
# - Topological load ordering (no circular deps)
# - Failure isolation during boot
# - Hot-reload with cascade support

module Kestowv
  module Config
    module Modules

      @modules = {}
      @deps    = {}
      @mutex   = Mutex.new

      class << self

        # --------------------------------------------------------
        # REGISTRATION
        # --------------------------------------------------------

        # Register a module explicitly.
        # depends_on: load these modules before this one.
        def register(name, path, feature: nil, depends_on: [])
          key = name.to_sym
          @mutex.synchronize do
            next if @modules.key?(key)  # idempotent

            feat = (feature || key).to_sym
            @modules[key] = {
              path:    path,
              feature: feat,
              loaded:  false,
              failed:  false
            }
            @deps[key] = depends_on.map(&:to_sym)
          end
          self
        end

        # --------------------------------------------------------
        # AUTO-DISCOVERY
        # --------------------------------------------------------

        def discover(base_path)
          log "Discovering under #{base_path}"

          Boot.load_directory(base_path, recursive: true, auto_version: true) do |path|
            next false unless File.extname(path) == ".rb"

            bc = Boot.byte_dispatch(path)
            next false if bc.skip?

            name = File.basename(path, ".rb").to_sym
            register(name, path) unless registered?(name)
            false  # discovery only — Boot.load_directory must NOT require the file
          end

          self
        end

        # --------------------------------------------------------
        # LOADING
        # --------------------------------------------------------

        # Load a single module, resolving dependencies first.
        # Guards against circular deps via a visited set.
        def load_module(name, visited: Set.new)
          key = name.to_sym

          entry = @mutex.synchronize { @modules[key]&.dup }
          return false unless entry
          return true  if entry[:loaded]
          return false if entry[:failed]

          if visited.include?(key)
            warn "[Modules] Circular dependency detected at :#{key}"
            return false
          end

          visited.add(key)

          # Resolve dependencies first
          deps = @mutex.synchronize { @deps[key].dup }
          deps.each do |dep|
            unless load_module(dep, visited: visited)
              warn "[Modules] Dependency :#{dep} failed — skipping :#{key}"
              mark_failed(key)
              return false
            end
          end

          # Load the module itself
          result = Boot.safe_require(entry[:path])

          if result
            mark_loaded(key, entry[:feature])
            log "✓ #{key} loaded"
          else
            mark_failed(key)
            Boot.handle_error(
              RuntimeError.new("Failed to load module :#{key}"),
              { path: entry[:path], feature: entry[:feature] }
            )
          end

          result
        end

        def load_all
          keys = @mutex.synchronize { @modules.keys.dup }
          keys.each { |name| load_module(name) }
          self
        end

        # --------------------------------------------------------
        # HOT-RELOAD
        # --------------------------------------------------------

        def hotload_module(name)
          key   = name.to_sym
          entry = @mutex.synchronize { @modules[key]&.dup }
          return false unless entry

          Boot.invalidate(entry[:feature])

          result = Boot.hotload(entry[:path])
          mark_loaded(key, entry[:feature]) if result
          result
        end

        # --------------------------------------------------------
        # INTROSPECTION
        # --------------------------------------------------------

        def registered?(name)
          @mutex.synchronize { @modules.key?(name.to_sym) }
        end

        def to_a
          @mutex.synchronize do
            @modules.map do |name, info|
              {
                name:    name,
                path:    info[:path],
                feature: info[:feature],
                loaded:  info[:loaded],
                failed:  info[:failed],
                deps:    @deps[name]
              }
            end
          end
        end

        def stats
          @mutex.synchronize do
            {
              total:   @modules.size,
              loaded:  @modules.count { |_, i| i[:loaded] },
              failed:  @modules.count { |_, i| i[:failed] }
            }
          end
        end

        # Reconcile loaded state against $LOADED_FEATURES.
        # Modules loaded via Boot.safe_require bypass load_module, so
        # mark_loaded is never called. This method closes that gap.
        def sync_loaded_state
          loaded_set = $LOADED_FEATURES.to_set
          @mutex.synchronize do
            @modules.each do |_, info|
              next if info[:loaded] || info[:failed]
              info[:loaded] = true if loaded_set.include?(info[:path])
            end
          end
          self
        end

        private

        def mark_loaded(key, feature)
          @mutex.synchronize do
            @modules[key][:loaded] = true
            @modules[key][:failed] = false
          end
        end

        def mark_failed(key)
          @mutex.synchronize { @modules[key][:failed] = true }
        end

        def log(msg)
          puts "  [Modules] #{msg}" unless Boot.config.quiet
        end
      end
    end
  end
end
