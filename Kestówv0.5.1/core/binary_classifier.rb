# frozen_string_literal: true

# Kestówv 0.5.1 — core/binary_classifier.rb
#
# Binary classification strategy for byte_dispatch.
# Reads the minimum bytes needed to identify the format,
# then returns a rich BinaryMeta so the loader never re-reads.
#
# Namespace: Kestowv::Core::BinaryClassifier
# (NOT Kestowv::Boot — that would shadow ::Boot for all kernel code.)
#
# Magic byte reference:
#   ELF:     \x7fELF  (4 bytes)
#   JVM:     \xca\xfe\xba\xbe  (4 bytes)
#   YARV:    YARV (4 bytes) — MRI compiled bytecode
#   Spinel:  SPNL (4 bytes) — Kestówv AOT artifact
#   Script:  #!   (2 bytes) — shebang
#   Flat:    fallback for position-independent blobs

module Kestowv
  module Core
    module BinaryClassifier

      # Minimum bytes to read for classification
      PEEK_SIZE = 64

      # Magic signatures — checked in priority order
      MAGIC = {
        "\x7fELF"           => :elf,
        "\xca\xfe\xba\xbe"  => :jvm_bytecode,
        "YARV"              => :yarv_bytecode,
        "SPNL"              => :spinel_artifact,
      }.freeze

      # ELF class byte (offset 4): 1 = 32-bit, 2 = 64-bit
      ELF_CLASS_32 = "\x01".b
      ELF_CLASS_64 = "\x02".b

      # ELF data byte (offset 5): 1 = little, 2 = big
      ELF_DATA_LSB = "\x01".b
      ELF_DATA_MSB = "\x02".b

      # ELF e_machine values (offset 18, 2 bytes little-endian)
      ELF_MACHINES = {
        "\x3e\x00".b => :x86_64,
        "\xb7\x00".b => :aarch64,
        "\xf3\x00".b => :riscv64,
        "\x08\x00".b => :mips,
      }.freeze

      # --------------------------------------------------------
      # REGISTRATION
      # Called once at boot to wire into the Boot feature registry.
      # --------------------------------------------------------

      def self.register_with_boot
        Boot.register(:core_binary_classifier)
        Boot.set_bit(:core_binary_classifier)
        self
      end

      # --------------------------------------------------------
      # CLASSIFY — main entry point
      # Returns a BinaryMeta or nil if not a recognized binary.
      # path:   String filesystem path
      # header: String binary header bytes (pre-peeked, optional)
      # --------------------------------------------------------

      def self.classify(path, header = nil)
        header ||= peek(path)
        return nil unless header && header.bytesize >= 4

        magic4 = header[0, 4].b
        magic2 = header[0, 2].b

        case
        when MAGIC.key?(magic4)
          kind = MAGIC[magic4]
          kind == :elf ? classify_elf(header) : classify_bytecode(kind, header, path)
        when magic2 == "#!".b
          classify_script(header, path)
        else
          classify_flat(header, path)
        end
      end

      private

      # --------------------------------------------------------
      # ELF CLASSIFICATION
      # --------------------------------------------------------

      def self.classify_elf(header)
        return nil unless header.bytesize >= 20

        elf_class  = header[4].b
        elf_data   = header[5].b
        e_machine  = header[18, 2].b

        kind   = elf_class == ELF_CLASS_64 ? :elf64 : :elf32
        endian = elf_data  == ELF_DATA_LSB ? :little : :big
        arch   = ELF_MACHINES[e_machine] || :unknown

        entry_offset = kind == :elf64 ? 24 : 20
        entry_point  = unpack_addr(header, entry_offset, kind, endian)

        phnum_offset  = kind == :elf64 ? 56 : 44
        segment_count = header[phnum_offset, 2]&.unpack1(endian == :little ? "v" : "n") || 0
        requires_interp = segment_count > 0

        Core.binary_meta(kind,
          confidence:      0.97,
          arch:            arch,
          endian:          endian,
          entry_point:     entry_point,
          requires_interp: requires_interp,
          segments:        [],
          metadata:        { segment_count: segment_count }
        )
      end

      # --------------------------------------------------------
      # BYTECODE CLASSIFICATION
      # --------------------------------------------------------

      def self.classify_bytecode(kind, header, path)
        case kind
        when :jvm_bytecode
          jvm_version = header[6, 2]&.unpack1("n")
          Core.binary_meta(:jvm_bytecode,
            confidence: 0.97,
            metadata: {
              main_class:      File.basename(path, ".*"),
              classpath_hints: [],
              jvm_version:     jvm_version
            }
          )

        when :yarv_bytecode
          ruby_version = header[4, 4]&.unpack("C4")&.first(3)&.join(".")
          Core.binary_meta(:yarv_bytecode,
            confidence: 0.95,
            metadata: {
              iseq_count:   nil,
              ruby_version: ruby_version
            }
          )

        when :spinel_artifact
          iseq_count  = header[4, 4]&.unpack1("V") || 0
          entry_point = header[8, 8]&.unpack1("Q<")
          Core.binary_meta(:spinel_artifact,
            confidence:  0.99,
            verified:    true,
            entry_point: entry_point,
            metadata: {
              section_map: {},
              iseq_count:  iseq_count
            }
          )
        end
      end

      # --------------------------------------------------------
      # SCRIPT CLASSIFICATION (shebang)
      # --------------------------------------------------------

      def self.classify_script(header, path)
        first_line = header.force_encoding("UTF-8").lines.first.to_s.chomp
        return nil unless first_line.start_with?("#!")

        shebang = first_line[2..].strip
        parts   = shebang.split(" ", 2)
        interp  = parts[0]
        args    = parts[1] ? parts[1].split(" ") : []

        script_kind = case interp
                      when /ruby/   then :ruby_script
                      when /python/ then :python_script
                      when /sh|bash|zsh/ then :shell_script
                      else :script
                      end

        Core.binary_meta(:script,
          confidence:  0.95,
          interpreter: interp,
          metadata:    { args: args, script_kind: script_kind }
        )
      end

      # --------------------------------------------------------
      # FLAT BINARY FALLBACK
      # --------------------------------------------------------

      def self.classify_flat(header, path)
        size = File.size?(path) || 0
        return nil if size == 0

        Core.binary_meta(:flat_binary,
          confidence: 0.60,
          metadata: {
            load_addr: 0x0,
            size:      size
          }
        )
      end

      # --------------------------------------------------------
      # HELPERS
      # --------------------------------------------------------

      def self.peek(path)
        return nil unless File.exist?(path) && File.file?(path)
        File.binread(path, PEEK_SIZE)
      rescue
        nil
      end

      def self.unpack_addr(header, offset, kind, endian)
        return nil unless header.bytesize >= offset + (kind == :elf64 ? 8 : 4)
        fmt = if kind == :elf64
                endian == :little ? "Q<" : "Q>"
              else
                endian == :little ? "V"  : "N"
              end
        header[offset, kind == :elf64 ? 8 : 4]&.unpack1(fmt)
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER with Config::Modules
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :core_binary_classifier,
  __FILE__,
  feature:    :binary_classifier,
  depends_on: [:core_binary_meta]
)
