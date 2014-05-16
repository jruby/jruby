package org.jruby.util.io;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyString;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;

import java.util.Arrays;

/**
 * Port of MRI's popen+exec logic.
 */
public class PopenExecutor {
    // rb_io_s_popen
    public static IRubyObject popen(ThreadContext context, IRubyObject[] argv, RubyClass klass, Block block) {
        Ruby runtime = context.runtime;
        String modestr;
        IRubyObject pname, pmode = context.nil, port, tmp, opt = context.nil, env = context.nil;
        ExecArg eargp;
        int oflags = 0, fmode = 0;
        IOEncodable.ConvConfig convconfig = new IOEncodable.ConvConfig();
        int argc = argv.length;

        if (argc > 1 && !(opt = TypeConverter.checkHashType(runtime, argv[argc - 1])).isNil()) --argc;
        if (argc > 1 && !(env = TypeConverter.checkHashType(runtime, argv[0])).isNil()) {
            --argc;
            argv = Arrays.copyOfRange(argv, 1, argv.length);
        }
        switch (argc) {
            case 2:
                pmode = argv[1];
            case 1:
                pname = argv[0];
                break;
            default: {
                int ex = opt.isNil() ? 0 : 1;
                Arity.raiseArgumentError(runtime, argc + ex, 1 + ex, 2 + ex);
                return null; // not reached
            }
        }

        tmp = TypeConverter.checkArrayType(runtime, pname);
        if (!tmp.isNil()) {
//            int len = ((RubyArray)tmp).size();
//            #if SIZEOF_LONG > SIZEOF_INT
//            if (len > INT_MAX) {
//                rb_raise(rb_eArgError, "too many arguments");
//            }
//            #endif
            tmp = ((RubyArray)tmp).aryDup();
            //            RBASIC_CLEAR_CLASS(tmp);
            eargp = execargNew(context, ((RubyArray)tmp).toJavaArray(), false);
            ((RubyArray)tmp).clear();
        } else {
            pname = pname.convertToString();
            eargp = null;
            // UNIMPLEMENTED
            if (!isPopenFork(pname))
                eargp = execargNew(context, new IRubyObject[]{pname}, true);
        }
        if (eargp != null) {
            if (!opt.isNil())
                // UNIMPLEMENTED
                opt = execargExtractOptions(context, eargp, opt);
            if (!env.isNil())
                // UNIMPLEMENTED
                execargSetenv(context, eargp, env);
        }
        IRubyObject[] valPtr = {null, null};
        int[] oflags_p = {oflags};
        int[] fmode_p = {fmode};
        EncodingUtils.extractModeEncoding(context, convconfig, valPtr, opt, oflags_p, fmode_p);
        // UNIMPLEMENTED
        modestr = OpenFile.oflagsModestr(oflags);

        port = pipeOpen(context, eargp, modestr, fmode, convconfig);
//        This is cleanup for failure to exec in the child.
//        if (port.isNil()) {
//            /* child */
//            if (rb_block_given_p()) {
//                rb_yield(Qnil);
//                rb_io_flush(rb_stdout);
//                rb_io_flush(rb_stderr);
//                _exit(0);
//            }
//            return Qnil;
//        }
        ((RubyBasicObject)port).setMetaClass(klass);
        RubyIO.ensureYieldClose(context, port, block);
        return port;
    }

    private static IRubyObject pipeOpen(ThreadContext context, ExecArg eargp, String modestr, int fmode, IOEncodable.ConvConfig convconfig) {
        // TODO
        return null;
    }

    private static IRubyObject execargExtractOptions(ThreadContext context, ExecArg eargp, IRubyObject opt) {
        // TODO
        return null;
    }

    private static void execargSetenv(ThreadContext context, ExecArg eargp, IRubyObject env) {
        // TODO
    }

    private static boolean isPopenFork(IRubyObject pname) {
        return false; // TODO
    }

    // rb_execarg_new
    private static ExecArg execargNew(ThreadContext context, IRubyObject[] argv, boolean accept_shell) {
        ExecArg eargp = new ExecArg();
        execargInit(context, argv, accept_shell, eargp);
        return eargp;
    }

    // rb_execarg_init
    private static RubyString execargInit(ThreadContext context, IRubyObject[] argv, boolean accept_shell, ExecArg eargp) {
        RubyString prog, ret;
        IRubyObject[] env_opt = {context.nil, context.nil};
        IRubyObject[][] argv_p = {argv};
        prog = execGetargs(context, argv_p, accept_shell, env_opt);
        execFillarg(context, prog, argv_p[0], env_opt[0], env_opt[1], eargp);
        ret = eargp.use_shell ? eargp.shell_script : eargp.command_name;
        return ret;
    }

    // rb_exec_getargs
    private static RubyString execGetargs(ThreadContext context, IRubyObject[][] argv_p, boolean accept_shell, IRubyObject[] env_opt) {
        Ruby runtime = context.runtime;
        IRubyObject hash;
        RubyString prog;
        int beg = 0;
        int end = argv_p[0].length;

        if (end >= 1) {
            hash = TypeConverter.checkHashType(runtime, argv_p[0][end - 1]);
            if (!hash.isNil()) {
                env_opt[1] = hash;
                end--;
            }
        }

        if (end >= 1) {
            hash = TypeConverter.checkHashType(runtime, argv_p[0][0]);
            if (!hash.isNil()) {
                env_opt[0] = hash;
                beg++;
            }
        }
        argv_p[0] = Arrays.copyOfRange(argv_p[0], beg, end);
        prog = checkArgv(context, argv_p[0]);
        if (prog != null) {
            prog = (RubyString)argv_p[0][0];
            if (accept_shell && (end - beg) == 1) {
                argv_p[0] = null;
            }
        }
        return prog;
    }

    // rb_check_argv
    public static RubyString checkArgv(ThreadContext context, IRubyObject[] argv) {
        Ruby runtime = context.runtime;
        IRubyObject tmp;
        RubyString prog;
        int i;

        Arity.checkArgumentCount(runtime, argv, 1, Integer.MAX_VALUE);

        prog = null;
        tmp = TypeConverter.checkArrayType(runtime, argv[0]);
        if (!tmp.isNil()) {
            if (((RubyArray)tmp).size() != 2) {
                throw runtime.newArgumentError("wrong first argument");
            }
            prog = ((RubyArray)tmp).eltOk(0).convertToString();
            argv[0] = ((RubyArray)tmp).eltOk(1);
            StringSupport.checkEmbeddedNulls(runtime, prog);
            prog = prog.strDup(runtime);
            prog.setFrozen(true);
        }
        for (i = 0; i < argv.length; i++) {
            argv[i] = argv[i].convertToString();
            argv[i] = ((RubyString)argv[i]).newFrozen();
            StringSupport.checkEmbeddedNulls(runtime, argv[i]);
        }
        //        security(name ? name : RSTRING_PTR(argv[0]));
        return prog;
    }

    private static final String posix_sh_cmds[] = {
            "!",		/* reserved */
            ".",		/* special built-in */
            ":",		/* special built-in */
            "break",		/* special built-in */
            "case",		/* reserved */
            "continue",		/* special built-in */
            "do",		/* reserved */
            "done",		/* reserved */
            "elif",		/* reserved */
            "else",		/* reserved */
            "esac",		/* reserved */
            "eval",		/* special built-in */
            "exec",		/* special built-in */
            "exit",		/* special built-in */
            "export",		/* special built-in */
            "fi",		/* reserved */
            "for",		/* reserved */
            "if",		/* reserved */
            "in",		/* reserved */
            "readonly",		/* special built-in */
            "return",		/* special built-in */
            "set",		/* special built-in */
            "shift",		/* special built-in */
            "then",		/* reserved */
            "times",		/* special built-in */
            "trap",		/* special built-in */
            "unset",		/* special built-in */
            "until",		/* reserved */
            "while",		/* reserved */
    };

    private static final byte[] DUMMY_ARRAY = {};
    private static void execFillarg(ThreadContext context, RubyString prog, IRubyObject[] argv, IRubyObject env, IRubyObject opthash, ExecArg eargp) {
        Ruby runtime = context.runtime;
        int argc = argv.length;
        byte[] fbuf = new byte[1024]; // MAXPATHLEN default from MRI's process.c

        if (!opthash.isNil()) {
            RubyIO.checkExecOptions(opthash);
        }

        if (!env.isNil()) {
            env = RubyIO.checkExecEnv(context, (RubyHash)env);
            eargp.env_modification = env;
        }

        prog = prog.export(context);
        eargp.use_shell = argc == 0;
        if (eargp.use_shell)
            eargp.shell_script = prog;
        else
            eargp.command_name = prog;

        if (!Platform.IS_WINDOWS) {
            if (eargp.use_shell) {
                byte[] pBytes;
                int p;
                ByteList first = new ByteList(DUMMY_ARRAY, false);
                boolean has_meta = false;
                /*
                 * meta characters:
                 *
                 * *    Pathname Expansion
                 * ?    Pathname Expansion
                 * {}   Grouping Commands
                 * []   Pathname Expansion
                 * <>   Redirection
                 * ()   Grouping Commands
                 * ~    Tilde Expansion
                 * &    AND Lists, Asynchronous Lists
                 * |    OR Lists, Pipelines
                 * \    Escape Character
                 * $    Parameter Expansion
                 * ;    Sequential Lists
                 * '    Single-Quotes
                 * `    Command Substitution
                 * "    Double-Quotes
                 * \n   Lists
                 *
                 * #    Comment
                 * =    Assignment preceding command name
                 * %    (used in Parameter Expansion)
                 */
                ByteList progByteList = prog.getByteList();
                pBytes = progByteList.unsafeBytes();
                for (p = progByteList.begin();p < progByteList.begin() + progByteList.length();p++){
                    if (pBytes[p] == ' ' || pBytes[p] == '\t'){
                        if (first.unsafeBytes() != DUMMY_ARRAY && first.length() == 0) first.setRealSize(p - first.begin());
                    }
                    else{
                        if (first.unsafeBytes() == DUMMY_ARRAY) { first.setUnsafeBytes(pBytes); first.setBegin(p); }
                    }
                    if (!has_meta && "*?{}[]<>()~&|\\$;'`\"\n#".indexOf(pBytes[p]) != -1)
                        has_meta = true;
                    if (first.length() == 0) {
                        if (first.get(p) == '='){
                            has_meta = true;
                        }
                        else if (first.get(p) == '/'){
                            first.setRealSize(0x100); /* longer than any posix_sh_cmds */
                        }
                    }
                    if (has_meta)
                        break;
                }
                if (!has_meta && first.getUnsafeBytes() != DUMMY_ARRAY) {
                    if (first.length() == 0) first.setRealSize(p - first.getBegin());
                    if (first.length() > 0 && first.length() <= posix_sh_cmds[0].length() &&
                            Arrays.binarySearch(posix_sh_cmds, first.toString()) != -1)
                        has_meta = true;
                }
                if (!has_meta) {
                               /* avoid shell since no shell meta character found. */
                    eargp.use_shell = false;
                }
                if (!eargp.use_shell) {
                    RubyString argv_buf;
                    //                argv_buf = hide_obj(rb_str_buf_new(0));
                    argv_buf = RubyString.newStringLight(runtime, 0);
                    pBytes = prog.getByteList().unsafeBytes();
                    p = prog.getByteList().begin();
                    while (pBytes[p] != 0 && p < pBytes.length){
                        while (pBytes[p] == ' ' || pBytes[p] == '\t')
                            p++;
                        if (pBytes[p] != 0){
                            int w = p;
                            while (pBytes[p] != 0 && pBytes[p] != ' ' && pBytes[p] != '\t')
                                p++;
                            argv_buf.cat(pBytes, w, p - w);
                            //                        rb_str_buf_cat(argv_buf, "", 1); /* append '\0' */
                        }
                    }
                    eargp.argv_buf = argv_buf;
                    eargp.command_name = ((RubyString)argv_buf).newFrozen();
                }
            }
        }

        if (!eargp.use_shell) {
            RubyString abspath;
            // UNIMPLEMENTED
            abspath = dlnFindExeR(eargp.command_name.getByteList().getUnsafeBytes(), 0, fbuf, fbuf.length);
            if (abspath == null)
                eargp.command_abspath = StringSupport.checkEmbeddedNulls(runtime, abspath);
            else
                eargp.command_abspath = null;
        }

        if (!eargp.use_shell && eargp.argv_buf != null) {
            int i;
            RubyString argv_buf;
            argv_buf = RubyString.newStringLight(runtime, 0);
//            hide_obj(argv_buf);
            for (i = 0; i < argc; i++) {
                IRubyObject arg = argv[i];
                RubyString argStr = StringSupport.checkEmbeddedNulls(runtime, arg);
                ByteList argBL = argStr.getByteList();
                byte[] sBytes = argBL.getUnsafeBytes();
                int s = argBL.begin();
//                #ifdef DEFAULT_PROCESS_ENCODING
//                arg = EXPORT_STR(arg);
//                s = RSTRING_PTR(arg);
//                #endif
                argv_buf.cat(sBytes, s, argStr.size());
//                rb_str_buf_cat(argv_buf, s, RSTRING_LEN(arg) + 1); /* include '\0' */
            }
            eargp.argv_buf = argv_buf;
        }

        if (!eargp.use_shell) {
            byte[] pBytes2;
            int p2, ep;
            byte[] nil = DUMMY_ARRAY;
            RubyString argv_str;
//            argv_str = hide_obj(rb_str_buf_new(sizeof(char*)*(argc + 2)));
            argv_str = RubyString.newStringLight(runtime, argc + 2);
            argv_str.cat(0); /* place holder for /bin/sh of try_with_sh. */
            pBytes2 = eargp.argv_buf.getByteList().unsafeBytes();
            p2 = eargp.argv_buf.getByteList().begin();
            ep = p2 + eargp.argv_buf.size();
            while (p2 < ep) {
                argv_str.cat(pBytes2[p2]);
                p2 += pBytes2.length; //strlen(p) + 1;
            }
            argv_str.cat(0); /* terminator for execve.  */
            eargp.argv_str = argv_str;
        }
//        RB_GC_GUARD(execarg_obj);
    }

    private static RubyString dlnFindExeR(byte[] unsafeBytes, int i, byte[] fbuf, int length) {
        // TODO
        return null;
    }

    private static class ExecArg {
        boolean use_shell;
        RubyString shell_script;
        RubyString command_name;
        RubyString command_abspath; /* full path string or nil */
        RubyString argv_str;
        RubyString argv_buf;
        IRubyObject redirect_fds;
        RubyString envp_str;
        RubyString envp_buf;
        RubyString dup2_tmpbuf;
        int flags = 0xFFFFFFFF;
        int pgroup_pgid; /* asis(-1), new pgroup(0), specified pgroup (0<V). */
        IRubyObject rlimit_limits; /* Qfalse or [[rtype, softlim, hardlim], ...] */
        int umask_mask;
        int uid;
        int gid;
        IRubyObject fd_dup2;
        IRubyObject fd_close;
        IRubyObject fd_open;
        IRubyObject fd_dup2_child;
        int close_others_maxhint;
        IRubyObject env_modification; /* Qfalse or [[k1,v1], ...] */
        IRubyObject chdir_dir;

        boolean pgroup_given() {
            return (flags & 0x1) != 0;
        }

        boolean umask_given() {
            return (flags & 0x2) != 0;
        }

        boolean unsetenv_others_given() {
            return (flags & 0x4) != 0;
        }

        boolean unsetenv_others_do() {
            return (flags & 0x8) != 0;
        }

        boolean close_others_given() {
            return (flags & 0x10) != 0;
        }

        boolean close_others_do() {
            return (flags & 0x20) != 0;
        }

        boolean chdir_given() {
            return (flags & 0x40) != 0;
        }

        boolean new_pgroup_given() {
            return (flags & 0x80) != 0;
        }

        boolean new_pgroup_flag() {
            return (flags & 0x100) != 0;
        }

        boolean uid_given() {
            return (flags & 0x200) != 0;
        }

        boolean gid_given() {
            return (flags & 0x400) != 0;
        }

        void pgroup_given_set() {
            flags |= 0x1;
        }

        void umask_given_set() {
            flags |= 0x2;
        }

        void unsetenv_others_given_set() {
            flags |= 0x4;
        }

        void unsetenv_others_do_set() {
            flags |= 0x8;
        }

        void close_others_given_set() {
            flags |= 0x10;
        }

        void close_others_do_set() {
            flags |= 0x20;
        }

        void chdir_given_set() {
            flags |= 0x40;
        }

        void new_pgroup_given_set() {
            flags |= 0x80;
        }

        void new_pgroup_flag_set() {
            flags |= 0x100;
        }

        void uid_given_set() {
            flags |= 0x200;
        }

        void gid_given_set() {
            flags |= 0x400;
        }

        void pgroup_given_clear() {
            flags &= ~0x1;
        }

        void umask_given_clear() {
            flags &= ~0x2;
        }

        void unsetenv_others_given_clear() {
            flags &= ~0x4;
        }

        void unsetenv_others_do_clear() {
            flags &= ~0x8;
        }

        void close_others_given_clear() {
            flags &= ~0x10;
        }

        void close_others_do_clear() {
            flags &= ~0x20;
        }

        void chdir_given_clear() {
            flags &= ~0x40;
        }

        void new_pgroup_given_clear() {
            flags &= ~0x80;
        }

        void new_pgroup_flag_clear() {
            flags &= ~0x100;
        }

        void uid_given_clear() {
            flags &= ~0x200;
        }

        void gid_given_clear() {
            flags &= ~0x400;
        }
    }
}
