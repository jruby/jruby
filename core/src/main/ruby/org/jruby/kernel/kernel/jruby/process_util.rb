module JRuby
  module ProcessUtil
    def self.exec_args(args)
      env, prog, opts = nil

      if args.size < 1
        raise ArgumentError, 'wrong number of arguments'
      end

      # peel off options
      if args.size >= 1
        maybe_hash = args[args.size - 1]
        if maybe_hash.respond_to?(:to_hash) && maybe_hash = maybe_hash.to_hash
          opts = maybe_hash
          args.pop
        end
      end

      # peel off env
      if args.size >= 1
        maybe_hash = args[0]
        if maybe_hash.respond_to?(:to_hash) && maybe_hash = maybe_hash.to_hash
          env = maybe_hash
          args.shift
        end
      end

      if args.size < 1
        raise ArgumentError, 'wrong number of arguments'
      end

      # if Array, pull out prog and insert command back into args list
      if Array === args[0]
        tmp_ary = args[0]

        if tmp_ary.size != 2
          raise ArgumentError, 'wrong number of arguments'
        end
        # scrub
        prog = String(tmp_ary[0].to_str)

        args[0] = tmp_ary[1]
      end

      # convert and scrub remaining args
      args.size.times do |i|
        # scrub
        args[i] = String(args[i].to_str)
      end

      prog = args[0] unless prog

      return env, prog, opts, args
    end
  end
end
