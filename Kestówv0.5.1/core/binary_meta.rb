# frozen_string_literal: true

# Kestówv 0.5.1 — core/binary_meta.rb
#
# BinaryMeta — the contract between byte_dispatch and the loader.
# Carries enough metadata that the loader never re-reads the binary.
#
# kernel-agnostic: BinaryMeta works against any VmSpace implementation.
# verified: true means the matcher pre-validated the binary — loader
#           skips integrity checks and goes straight to mapping.

module Kestowv
  module Core

    # --------------------------------------------------------
    # COMMON FIELDS — present on all binary kinds
    # --------------------------------------------------------

    BinaryMeta = Struct.new(
      :kind,            # Symbol — :elf64, :elf32, :script, :spinel_artifact, etc.
      :confidence,      # Float  — 0.0..1.0 from byte_dispatch
      :verified,        # Bool   — true = pre-validated, skip integrity check
      :arch,            # Symbol — :x86_64, :aarch64, :riscv64, :jvm, :ruby, nil
      :endian,          # Symbol — :little, :big, nil
      :entry_point,     # Integer virtual address / iseq index / nil
      :interpreter,     # String path for scripts / ELF .interp / nil
      :requires_interp, # Bool   — needs dynamic linker or interpreter
      :min_vaddr,       # Integer — lowest VA used (address-space formats)
      :max_vaddr,       # Integer — highest VA used (address-space formats)
      :segments,        # Array  — loadable segment descriptors
      :metadata,        # Hash   — format-specific overflow fields
      keyword_init: true
    ) do
      def skip?
        confidence < 0.5
      end

      def address_space?
        %i[elf64 elf32 flat_binary].include?(kind)
      end

      def bytecode?
        %i[yarv_bytecode jvm_bytecode spinel_artifact].include?(kind)
      end

      def script?
        kind == :script
      end

      def to_s
        "#<BinaryMeta #{kind} arch=#{arch} verified=#{verified} confidence=#{confidence}>"
      end
    end

    # --------------------------------------------------------
    # SEGMENT DESCRIPTOR — used by ELF and flat binary
    # --------------------------------------------------------

    Segment = Struct.new(
      :type,        # :load, :dynamic, :interp, :note
      :offset,      # Integer file offset
      :vaddr,       # Integer virtual address
      :filesz,      # Integer bytes in file
      :memsz,       # Integer bytes in memory (memsz >= filesz)
      :flags,       # Mm::Protection bitmask
      keyword_init: true
    ) do
      def loadable? = type == :load
      def size_in_pages(page_size = 4096)
        (memsz.to_f / page_size).ceil
      end
    end

    # --------------------------------------------------------
    # KIND REGISTRY — maps kind symbol to default field set.
    # Used by byte_dispatch to construct a populated BinaryMeta.
    # --------------------------------------------------------

    BINARY_KIND_DEFAULTS = {

      elf64: {
        arch:            nil,
        endian:          :little,
        entry_point:     nil,
        interpreter:     nil,
        requires_interp: false,
        min_vaddr:       nil,
        max_vaddr:       nil,
        segments:        [],
        verified:        false,
        metadata:        {}
      },

      elf32: {
        arch:            nil,
        endian:          :little,
        entry_point:     nil,
        interpreter:     nil,
        requires_interp: false,
        min_vaddr:       nil,
        max_vaddr:       nil,
        segments:        [],
        verified:        false,
        metadata:        {}
      },

      script: {
        arch:            nil,
        endian:          nil,
        entry_point:     nil,
        interpreter:     nil,   # populated from shebang line
        requires_interp: true,  # scripts always need an interpreter
        min_vaddr:       nil,
        max_vaddr:       nil,
        segments:        [],
        verified:        false, # scripts are never pre-verified
        metadata:        { args: [] }
      },

      spinel_artifact: {
        arch:            nil,
        endian:          nil,
        entry_point:     nil,
        interpreter:     nil,
        requires_interp: false,
        min_vaddr:       nil,
        max_vaddr:       nil,
        segments:        [],
        verified:        true,  # Spinel artifacts are always pre-verified
        metadata:        {
          section_map: {},
          iseq_count:  0
        }
      },

      yarv_bytecode: {
        arch:            :ruby,
        endian:          nil,
        entry_point:     nil,   # main iseq index, not a VA
        interpreter:     nil,
        requires_interp: true,  # needs MRI/YARV runtime
        min_vaddr:       nil,
        max_vaddr:       nil,
        segments:        [],
        verified:        false,
        metadata:        {
          iseq_count:   0,
          ruby_version: nil
        }
      },

      jvm_bytecode: {
        arch:            :jvm,
        endian:          :big,  # JVM class files are big-endian
        entry_point:     nil,   # JVM entry = main method
        interpreter:     nil,
        requires_interp: true,  # needs JVM runtime
        min_vaddr:       nil,
        max_vaddr:       nil,
        segments:        [],
        verified:        false,
        metadata:        {
          main_class:      nil,
          classpath_hints: [],
          jvm_version:     nil
        }
      },

      flat_binary: {
        arch:            nil,
        endian:          :little,
        entry_point:     nil,
        interpreter:     nil,
        requires_interp: false,
        min_vaddr:       nil,
        max_vaddr:       nil,
        segments:        [],
        verified:        false,
        metadata:        {
          load_addr: nil,
          size:      0
        }
      }

    }.freeze

    # --------------------------------------------------------
    # FACTORY — build a BinaryMeta for a given kind
    # --------------------------------------------------------

    def self.binary_meta(kind, confidence: 0.0, **overrides)
      defaults = BINARY_KIND_DEFAULTS[kind]
      raise ArgumentError, "Unknown binary kind: #{kind}" unless defaults

      BinaryMeta.new(
        kind:       kind,
        confidence: confidence,
        **defaults.merge(overrides)
      )
    end

  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :core_binary_meta,
  __FILE__,
  feature:    :binary_meta,
  depends_on: [:core_kobject, :mm_protection]
)
