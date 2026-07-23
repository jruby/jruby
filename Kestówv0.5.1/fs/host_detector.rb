# frozen_string_literal: true

# Kestówv 0.5.1 — fs/host_detector.rb
#
# Host environment detection via Boot.byte_dispatch + JVM system properties.
# Returns a Detection result used by VFS to select the appropriate backend.
# Never uses RbConfig or RUBY_PLATFORM string matching — ByteClass only.

module Kestowv
  module Fs
    module HostDetector

      # --------------------------------------------------------
      # HOST BYTECLASS KINDS
      # Extend the existing ByteClass system with host-specific kinds.
      # These feed into byte_dispatch for VFS backend selection.
      # --------------------------------------------------------

      HOST_KINDS = %i[
        host_linux
        host_macos
        host_windows
        host_posix
        containerized
        standalone_mode
      ].freeze

      # --------------------------------------------------------
      # DETECTION RESULT
      # --------------------------------------------------------

      Detection = Struct.new(
        :os,            # :linux, :macos, :windows, :unknown
        :containerized, # boolean
        :jvm_os,        # raw JVM os.name property
        :jvm_arch,      # raw JVM os.arch property
        :byte_kind,     # ByteClass kind symbol
        :capabilities,  # Set of detected host capabilities
        keyword_init: true
      ) do
        def posix?   = %i[linux macos].include?(os)
        def windows? = os == :windows
        def linux?   = os == :linux
        def macos?   = os == :macos
        def known?   = os != :unknown

        def to_s
          "#<HostDetector::Detection os=#{os} jvm=#{jvm_os} containerized=#{containerized}>"
        end
      end

      @cached    = nil
      @mutex     = Mutex.new

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:fs_host_detector)
          Boot.set_bit(:fs_host_detector)
          self
        end

        # --------------------------------------------------------
        # DETECTION — cached after first call
        # --------------------------------------------------------

        def detect(force: false)
          @mutex.synchronize do
            return @cached if @cached && !force
            @cached = run_detection
          end
          @cached
        end

        def reset!
          @mutex.synchronize { @cached = nil }
        end

        # --------------------------------------------------------
        # INTROSPECTION
        # --------------------------------------------------------

        def stats
          d = @mutex.synchronize { @cached }
          {
            feature:      :fs_host_detector,
            detected:     !d.nil?,
            os:           d&.os,
            containerized: d&.containerized,
            byte_kind:    d&.byte_kind,
            capabilities: d&.capabilities&.to_a
          }
        end

        private

        # --------------------------------------------------------
        # DETECTION PIPELINE
        # Priority: JVM properties → byte_dispatch probes → fallback
        # --------------------------------------------------------

        def run_detection
          jvm_os   = jvm_property("os.name")  || ""
          jvm_arch = jvm_property("os.arch")  || ""

          os           = classify_jvm_os(jvm_os)
          containerized = detect_containerized(os)
          capabilities  = detect_capabilities(os)
          byte_kind     = os_to_byte_kind(os, containerized)

          # Register the detected kind in the Boot bit vector
          Boot.register(:"fs_#{byte_kind}")
          Boot.set_bit(:"fs_#{byte_kind}")

          Detection.new(
            os:           os,
            containerized: containerized,
            jvm_os:       jvm_os,
            jvm_arch:     jvm_arch,
            byte_kind:    byte_kind,
            capabilities: capabilities
          )
        end

        # --------------------------------------------------------
        # JVM-BASED OS CLASSIFICATION
        # Uses java.lang.System.getProperty — consistent across all
        # JVM platforms, no string-matching on RUBY_PLATFORM needed.
        # --------------------------------------------------------

        def classify_jvm_os(jvm_os)
          case jvm_os.downcase
          when /linux/   then :linux
          when /mac/     then :macos
          when /windows/ then :windows
          else                :unknown
          end
        end

        # --------------------------------------------------------
        # BYTE_DISPATCH PROBES
        # Used to confirm OS classification and detect capabilities.
        # Probes are ordered from most specific to least specific.
        # --------------------------------------------------------

        LINUX_PROBES = [
          "/proc/version",
          "/proc/1/cgroup",
          "/etc/os-release"
        ].freeze

        MACOS_PROBES = [
          "/System/Library/CoreServices/SystemVersion.plist",
          "/usr/bin/sw_vers"
        ].freeze

        WINDOWS_PROBES = [
          "C:\\Windows\\System32\\kernel32.dll",
          "C:\\Windows\\System32\\ntdll.dll"
        ].freeze

        def probe_exists?(path)
          bc = Boot.byte_dispatch(path)
          bc && !bc.skip? && bc.kind != :unknown
        rescue
          false
        end

        def detect_containerized(os)
          return false unless os == :linux

          # Docker/OCI: /.dockerenv exists
          return true if probe_exists?("/.dockerenv")

          # cgroup v1 docker marker
          begin
            content = ::File.read("/proc/1/cgroup", 256) rescue ""
            return true if content.include?("docker") || content.include?("kubepods")
          rescue
            nil
          end

          false
        end

        def detect_capabilities(os)
          caps = Set.new
          caps << :posix   if %i[linux macos].include?(os)
          caps << :proc_fs if os == :linux && probe_exists?("/proc/version")
          caps << :sysfs   if os == :linux && probe_exists?("/sys/kernel")
          caps << :devfs   if probe_exists?("/dev/null")
          caps << :tmpfs   if os == :linux && probe_exists?("/proc/mounts")
          caps << :uds     if os != :windows
          caps
        end

        def os_to_byte_kind(os, containerized)
          return :containerized   if containerized
          return :host_linux      if os == :linux
          return :host_macos      if os == :macos
          return :host_windows    if os == :windows
          :standalone_mode
        end

        def jvm_property(key)
          java.lang.System.getProperty(key)
        rescue
          nil
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :fs_host_detector,
  __FILE__,
  feature:    :fs_host_detector,
  depends_on: [:fs_vfs]
)
