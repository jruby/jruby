package org.jruby.util.io;

import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.constants.platform.OpenFlags;
import jnr.constants.platform.Signal;
import jnr.posix.SpawnAttribute;
import jnr.posix.SpawnFileAction;
import org.jcodings.transcode.EConvFlags;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyProcess;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ShellLauncher;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Pipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of MRI's popen+exec logic.
 */
public class PopenExecutor {
    // MRI: check_pipe_command
    public static IRubyObject checkPipeCommand(ThreadContext context, IRubyObject filenameOrCommand) {
        RubyString filenameStr = filenameOrCommand.convertToString();
        ByteList filenameByteList = filenameStr.getByteList();
        int[] chlen = {0};

        if (EncodingUtils.encAscget(
                filenameByteList.getUnsafeBytes(),
                filenameByteList.getBegin(),
                filenameByteList.getBegin() + filenameByteList.getRealSize(),
                chlen,
                filenameByteList.getEncoding()) == '|') {
            return filenameStr.makeShared19(context.runtime, chlen[0], filenameByteList.length() - 1).infectBy(filenameOrCommand);
        }
        return context.nil;
    }

    public static RubyFixnum spawn(ThreadContext context, IRubyObject[] argv) {
        Ruby runtime = context.runtime;
        long pid = 0;
        String[] errmsg = { null };
        ExecArg eargp;
        IRubyObject fail_str;

        eargp = execargNew(context, argv, true);
        execargFixup(context, runtime, eargp);
        fail_str = eargp.use_shell ? eargp.shell_script : eargp.command_name;

        PopenExecutor executor = new PopenExecutor();
        pid = executor.spawnProcess(context, runtime, eargp, errmsg);

        if (pid == -1) {
            if (errmsg[0] == null) {
                throw runtime.newSystemCallError(fail_str.toString());
            }
            throw runtime.newErrnoFromErrno(executor.errno, errmsg[0]);
        }
        return runtime.newFixnum(pid);
    }

    long spawnProcess(ThreadContext context, Ruby runtime, ExecArg eargp, String[] errmsg) {
        long pid;
        RubyString prog;
        ExecArg sarg = new ExecArg();

        prog = eargp.use_shell ? eargp.shell_script : eargp.command_name;

        if (execargRunOptions(context, runtime, eargp, sarg, errmsg) < 0) {
            return -1;
        }

        if (prog != null && !eargp.use_shell) {
            String[] argv = ARGVSTR2ARGV(eargp.argv_str.argv);
            argv[0] = prog.toString();
        }
        if (eargp.use_shell) {
            pid = procSpawnSh(runtime, prog.toString());
        }
        else {
            String[] argv = ARGVSTR2ARGV(eargp.argv_str.argv);
            pid = procSpawnCmd(runtime, argv, prog.toString(), eargp);
        }
        if (pid == -1)
            context.setLastExitStatus(new RubyProcess.RubyStatus(runtime, runtime.getProcStatus(), 0x7f << 8, 0));

        execargRunOptions(context, runtime, sarg, null, errmsg);

        return pid;
    }

    // TODO: win32
//    #if defined(_WIN32)
//    #define proc_spawn_cmd_internal(argv, prog) rb_w32_uaspawn(P_NOWAIT, (prog), (argv))
//            #else
    long procSpawnCmdInternal(Ruby runtime, String[] argv, String prog) {
        long status;

        if (prog == null)
            prog = argv[0];
//        security(prog);
        prog = dlnFindExeR(runtime, prog, null);
        if (prog == null)
            return -1;

        // TODO?
//        beforeExec();
        status = runtime.getPosix().posix_spawnp(prog, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Arrays.asList(argv), Collections.EMPTY_LIST);
        if (status == -1 && runtime.getPosix().errno() == Errno.ENOEXEC.intValue()) {
            String[] newArgv = new String[argv.length + 1];
            newArgv[1] = prog;
            newArgv[0] = "sh";
            status = runtime.getPosix().posix_spawnp("/bin/sh", Collections.EMPTY_LIST, Collections.EMPTY_LIST, Arrays.asList(argv), Collections.EMPTY_LIST);
            // TODO?
//            afterExec();
            if (status == -1) errno = Errno.ENOEXEC;
        }
        return status;
    }


    long procSpawnCmd(Ruby runtime, String[] argv, String prog, ExecArg eargp) {
        long pid = -1;

        if (argv.length > 0 && argv[0] != null) {
//            #if defined(_WIN32)
//            DWORD flags = 0;
//            if (eargp->new_pgroup_given && eargp->new_pgroup_flag) {
//                flags = CREATE_NEW_PROCESS_GROUP;
//            }
//            pid = rb_w32_uaspawn_flags(P_NOWAIT, prog ? RSTRING_PTR(prog) : 0, argv, flags);
//            #else
            pid = procSpawnCmdInternal(runtime, argv, prog);
        }
        return pid;
    }

    // TODO: win32 version
//    #if defined(_WIN32)
//    #define proc_spawn_sh(str) rb_w32_uspawn(P_NOWAIT, (str), 0)
//            #else
    long procSpawnSh(Ruby runtime, String str) {
        long status;

        String shell = dlnFindExeR(runtime, "sh", null);
        // TODO? Stops some threads and signals
//        before_exec();
        status = runtime.getPosix().posix_spawnp(shell != null ? shell : "/bin/sh", Collections.EMPTY_LIST, Collections.EMPTY_LIST, Arrays.asList("sh", "-c", str), Collections.EMPTY_LIST);
        if (status == -1) errno = Errno.valueOf(runtime.getPosix().errno());
        // TODO?
//        after_exec();
        return status;
    }

    // pipe_open_s
    public static IRubyObject pipeOpen(ThreadContext context, IRubyObject prog, String modestr, int fmode, IOEncodable convconfig) {
        IRubyObject[] argv = {prog};
        ExecArg execArg = null;

        if (!isPopenFork(context.runtime, (RubyString)prog))
            execArg = execargNew(context, argv, true);
        return new PopenExecutor().pipeOpen(context, execArg, modestr, fmode, convconfig);
    }

    // rb_io_s_popen
    public static IRubyObject popen(ThreadContext context, IRubyObject[] argv, RubyClass klass, Block block) {
        Ruby runtime = context.runtime;
        String modestr;
        IRubyObject pname, pmode[] = {null, null}, port, tmp, opt = context.nil, env = context.nil;
        ExecArg eargp;
        int[] oflags_p = {0}, fmode_p = {0};
        IOEncodable.ConvConfig convconfig = new IOEncodable.ConvConfig();
        int argc = argv.length;

        if (argc > 1 && !(opt = TypeConverter.checkHashType(runtime, argv[argc - 1])).isNil()) --argc;
        if (argc > 1 && !(env = TypeConverter.checkHashType(runtime, argv[0])).isNil()) {
            --argc;
            argv = Arrays.copyOfRange(argv, 1, argc + 1);
        }
        switch (argc) {
            case 2:
                pmode[0] = argv[1];
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
//                throw runtime.newArgumentError("too many arguments");
//            }
//            #endif
            tmp = ((RubyArray)tmp).aryDup();
            //            RBASIC_CLEAR_CLASS(tmp);
            eargp = execargNew(context, ((RubyArray)tmp).toJavaArray(), false);
            ((RubyArray)tmp).clear();
        } else {
            pname = pname.convertToString();
            eargp = null;
            if (!isPopenFork(runtime, (RubyString)pname)) {
                IRubyObject[] pname_p = {pname};
                eargp = execargNew(context, pname_p, true);
                pname = pname_p[0];
            }
        }
        if (eargp != null) {
            if (!opt.isNil())
                opt = execargExtractOptions(context, runtime, eargp, (RubyHash)opt);
            if (!env.isNil())
                execargSetenv(context, runtime, eargp, env);
        }
        EncodingUtils.extractModeEncoding(context, convconfig, pmode, opt, oflags_p, fmode_p);
        modestr = OpenFile.ioOflagsModestr(runtime, oflags_p[0]);

        port = new PopenExecutor().pipeOpen(context, eargp, modestr, fmode_p[0], convconfig);
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

    static void execargSetenv(ThreadContext context, Ruby runtime, ExecArg eargp, IRubyObject env) {
        env = !env.isNil() ? checkExecEnv(context, runtime, (RubyHash)env) : runtime.getFalse();
        eargp.env_modification = env;
    }

    static IRubyObject checkExecEnv(ThreadContext context, Ruby runtime, RubyHash hash) {
        RubyArray env;

        env = runtime.newArray();
        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)hash.directEntrySet()) {
            IRubyObject key = entry.getKey();
            IRubyObject val = entry.getValue();
            String k;

            k = StringSupport.checkEmbeddedNulls(runtime, key).toString();
            if (k.indexOf('=') != -1)
                throw runtime.newArgumentError("environment name contains a equal : " + k);

            if (!val.isNil())
                val = StringSupport.checkEmbeddedNulls(runtime, val);

            key = key.convertToString().export(context);
            if (!val.isNil()) val = val.convertToString().export(context);

            env.push(runtime.newArray(key, val));
        }

        return env;
    }

    static IRubyObject execargExtractOptions(ThreadContext context, Ruby runtime, ExecArg eargp, RubyHash opthash) {
        if (opthash.isEmpty())
            return context.nil;
        IRubyObject nonopts = context.nil;
        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)opthash.directEntrySet()) {
            IRubyObject key = entry.getKey();
            IRubyObject val = entry.getValue();
            if (execargAddopt(context, runtime, eargp, key, val) != ST_CONTINUE) {
                if (nonopts.isNil()) nonopts = RubyHash.newHash(runtime);
                ((RubyHash)nonopts).op_aset(context, key, val);
            }
        }
        return nonopts;
    }

    static void checkExecOptions(ThreadContext context, Ruby runtime, RubyHash opthash, ExecArg eargp) {
        if (opthash.isEmpty())
            return;
        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)opthash.directEntrySet()) {
            IRubyObject key = entry.getKey();
            IRubyObject val = entry.getValue();
            if (execargAddopt(context, runtime, eargp, key, val) != ST_CONTINUE) {
                if (key instanceof RubySymbol)
                    throw runtime.newArgumentError("wrong exec option symbol: " + key);
                throw runtime.newArgumentError("wrong exec option");
            }
        }
    }

    static boolean isPopenFork(Ruby runtime, RubyString prog)
    {
        if (prog.size() == 1 && prog.getByteList().get(0) == '-') {
            throw runtime.newNotImplementedError("fork() function is unimplemented on JRuby");
        }
        return false;
    }

    private static long DO_SPAWN(Ruby runtime, ExecArg eargp, String cmd, String[] args, String[] envp) {
//        System.out.println("fd_dup2: " + eargp.fd_dup2);
//        System.out.println("fd_close: " + eargp.fd_close);
//        System.out.println("fd_dup2_child: " + eargp.fd_dup2_child);
//        System.out.println("fd_open: " + eargp.fd_open);
        return runtime.getPosix().posix_spawnp(
                cmd,
                eargp.fileActions,
                eargp.attributes,
                args == null ? Collections.EMPTY_LIST : Arrays.asList(args),
                envp == null ? Collections.EMPTY_LIST : Arrays.asList(envp));
    }

    private static class PopenArg {
        ExecArg eargp;
        int modef;
        Channel[] pair = {null, null};
        Channel[] write_pair = {null, null};
    }

    private static String[] ARGVSTR2ARGV(byte[][] argv_str) {
        String[] argv = new String[argv_str.length];
        for (int i = 0; i < argv_str.length; i++) {
            // FIXME: probably should be using a specific encoding
            if (argv_str[i] == null) continue; // placeholder for /bin/sh, but unsure where it's supposed to be added
            argv[i] = new String(argv_str[i]);
        }
        return argv;
    }

    private int pipe(Channel[] channels) {
        try {
            Pipe pipe = Pipe.open();
            channels[0] = pipe.source();
            channels[1] = pipe.sink();
            return 0;
        } catch (IOException ioe) {
            errno = Helpers.errnoFromException(ioe);
            return -1;
        }
    }

    private Errno errno = null;

    // MRI: pipe_open
    private IRubyObject pipeOpen(ThreadContext context, ExecArg eargp, String modestr, int fmode, IOEncodable convconfig) {
        final Ruby runtime = context.runtime;
        IRubyObject prog = eargp != null ? (eargp.use_shell ? eargp.shell_script : eargp.command_name) : runtime.getFalse();
        long pid = 0;
        OpenFile fptr;
        IRubyObject port;
        OpenFile write_fptr;
        IRubyObject write_port;

        PopenArg arg = new PopenArg();
        Errno e = null;

        String[] args = null;
        String[] envp = null;

        ExecArg sargp = new ExecArg();
        Channel fd;
        Channel write_fd = null;
        String cmd = null;

        if (prog.isTrue())
            cmd = StringSupport.checkEmbeddedNulls(runtime, prog).toString();

        arg.eargp = eargp;
        arg.modef = fmode;
        arg.pair[0] = arg.pair[1] = null;
        arg.write_pair[0] = arg.write_pair[1] = null;
        if (eargp != null && !eargp.use_shell) {
            args = ARGVSTR2ARGV(eargp.argv_str.argv);
        }
        switch (fmode & (OpenFile.READABLE|OpenFile.WRITABLE)) {
            case OpenFile.READABLE | OpenFile.WRITABLE:
                if (pipe(arg.write_pair) < 0)
                    throw runtime.newErrnoFromErrno(errno, prog.toString());
                if (pipe(arg.pair) < 0) {
                    e = errno;
                    try {arg.write_pair[0].close();} catch (IOException ioe) {}
                    try {arg.write_pair[1].close();} catch (IOException ioe) {}
                    errno = e;
                    throw runtime.newErrnoFromErrno(errno, prog.toString());
                }
                if (eargp != null) {
                    execargAddopt(context, runtime, eargp, RubyFixnum.zero(runtime), RubyFixnum.newFixnum(runtime, ChannelDescriptor.getFilenoFromChannel(arg.write_pair[0])));
                    execargAddopt(context, runtime, eargp, RubyFixnum.one(runtime), RubyFixnum.newFixnum(runtime, ChannelDescriptor.getFilenoFromChannel(arg.pair[1])));
                }
                break;
            case OpenFile.READABLE:
                if (pipe(arg.pair) < 0)
                    throw runtime.newErrnoFromErrno(errno, prog.toString());
                if (eargp != null)
                    execargAddopt(context, runtime, eargp, RubyFixnum.one(runtime), RubyFixnum.newFixnum(runtime, ChannelDescriptor.getFilenoFromChannel(arg.pair[1])));
                break;
            case OpenFile.WRITABLE:
                if (pipe(arg.pair) < 0)
                    throw runtime.newErrnoFromErrno(errno, prog.toString());
                if (eargp != null)
                    execargAddopt(context, runtime, eargp, RubyFixnum.zero(runtime), RubyFixnum.newFixnum(runtime, ChannelDescriptor.getFilenoFromChannel(arg.pair[0])));
                break;
            default:
                throw runtime.newErrnoFromErrno(errno, prog.toString());
        }
        if (eargp != null) {
            execargFixup(context, runtime, eargp);
            execargRunOptions(context, runtime, eargp, sargp, null);
            if (eargp.envp_str != null) envp = eargp.envp_str;
            while ((pid = DO_SPAWN(runtime, eargp, cmd, args, envp)) == -1) {
	            /* exec failed */
                switch (e = errno) {
                    case EAGAIN:
                    case EWOULDBLOCK:
                        try {Thread.sleep(1000);} catch (InterruptedException ie) {}
                        continue;
                }
                break;
            }
            if (eargp != null)
                execargRunOptions(context, runtime, sargp, null, null);
        }
        else {
            throw runtime.newNotImplementedError("spawn without exec args (probably a bug)");
        }

        /* parent */
        if (pid == -1) {
            try {arg.pair[0].close();} catch (IOException ioe) {}
            try {arg.pair[1].close();} catch (IOException ioe) {}
            if ((fmode & (OpenFile.READABLE|OpenFile.WRITABLE)) == (OpenFile.READABLE|OpenFile.WRITABLE)) {
                try {arg.write_pair[0].close();} catch (IOException ioe) {}
                try {arg.write_pair[1].close();} catch (IOException ioe) {}
            }
            errno = e;
            throw runtime.newErrnoFromErrno(errno, prog.toString());
        }
        if ((fmode & OpenFile.READABLE) != 0 && (fmode & OpenFile.WRITABLE) != 0) {
            try {arg.pair[1].close();} catch (IOException ioe) {}
            fd = arg.pair[0];
            try {arg.write_pair[0].close();} catch (IOException ioe) {}
            write_fd = arg.write_pair[1];
        }
        else if ((fmode & OpenFile.READABLE) != 0) {
            try {arg.pair[1].close();} catch (IOException ioe) {}
            fd = arg.pair[0];
        }
        else {
            try {arg.pair[0].close();} catch (IOException ioe) {}
            fd = arg.pair[1];
        }

        port = runtime.getIO().allocate();
        fptr = ((RubyIO)port).MakeOpenFile();
        fptr.setFD(fd);
        fptr.setMode(fmode | (OpenFile.SYNC|OpenFile.DUPLEX));
        if (convconfig != null) {
            fptr.encs.copy(convconfig);
            if (Platform.IS_WINDOWS) {
                if ((fptr.encs.ecflags & EncodingUtils.ECONV_DEFAULT_NEWLINE_DECORATOR) != 0) {
                    fptr.encs.ecflags |= EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
                }
            }
        }
        else {
            if (fptr.NEED_NEWLINE_DECORATOR_ON_READ()) {
                fptr.encs.ecflags |= EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
            }
            if (EncodingUtils.TEXTMODE_NEWLINE_DECORATOR_ON_WRITE != 0) {
                if (fptr.NEED_NEWLINE_DECORATOR_ON_WRITE()) {
                    fptr.encs.ecflags |= EncodingUtils.TEXTMODE_NEWLINE_DECORATOR_ON_WRITE;
                }
            }
        }
        final long finalPid = pid;
        fptr.process = new Process() {
            @Override
            public OutputStream getOutputStream() {
                return null;
            }

            @Override
            public InputStream getInputStream() {
                return null;
            }

            @Override
            public InputStream getErrorStream() {
                return null;
            }

            @Override
            public int waitFor() throws InterruptedException {
                int[] stat_loc = {0};
                int result = runtime.getPosix().waitpid((int)finalPid, stat_loc, 0);
                if (result == -1) {
                    if (runtime.getPosix().errno() == Errno.EINTR.intValue()) {
                        throw new InterruptedException();
                    } else {
                        throw new RuntimeException("unexpected waitpid errno: " + Errno.valueOf(runtime.getPosix().errno()));
                    }
                }
                return stat_loc[0];
            }

            @Override
            public int exitValue() {
                try {
                    return waitFor();
                } catch (InterruptedException ie) {
                    throw new IllegalThreadStateException();
                }
            }

            @Override
            public void destroy() {
                runtime.getPosix().kill((int)finalPid, Signal.SIGTERM.intValue());
            }
        };

        if (write_fd != null) {
            write_port = runtime.getIO().allocate();
            write_fptr = ((RubyIO)write_port).MakeOpenFile();
            write_fptr.setFD(write_fd);
            write_fptr.setMode((fmode & ~OpenFile.READABLE)| OpenFile.SYNC|OpenFile.DUPLEX);
            fptr.setMode(fptr.getMode() & ~OpenFile.WRITABLE);
            fptr.tiedIOForWriting = (RubyIO)write_port;
            ((RubyIO)port).setInstanceVariable("@tied_io_for_writing", write_port);
        }

        // TODO?
//        fptr.setFinalizer(pipe_finalize);
//        pipeAddFptr(fptr);
        return port;
    }

    static int run_exec_pgroup(Ruby runtime, ExecArg eargp, ExecArg sargp, String[] errmsg) {
    /*
     * If FD_CLOEXEC is available, rb_fork waits the child's execve.
     * So setpgid is done in the child when rb_fork is returned in the parent.
     * No race condition, even without setpgid from the parent.
     * (Is there an environment which has setpgid but no FD_CLOEXEC?)
     */
        int ret;
        long pgroup;

        pgroup = eargp.pgroup_pgid;
        if (pgroup == -1)
            return 0;

        if (sargp != null) {
        /* maybe meaningless with no fork environment... */
            sargp.pgroup_given_set();
            sargp.pgroup_pgid = runtime.getPosix().getpgrp();
        }

        if (pgroup == 0) {
            pgroup = runtime.getPosix().getpid(); /* async-signal-safe */
        }
        // we can't setpgid in the parent
//        ret = setpgid(getpid(), pgroup); /* async-signal-safe */
//        if (ret == -1) ERRMSG("setpgid");
        return 0; // ret
    }

    static int run_exec_rlimit(Ruby runtime, RubyArray ary, ExecArg sargp, String[] errmsg) {
        long i;
        for (i = 0; i < ary.size(); i++) {
            IRubyObject elt = ary.eltOk(i);
            int rtype = RubyNumeric.num2int(((RubyArray)elt).eltOk(0));

            /* // TODO
            struct rlimit rlim;
            if (sargp != null) {
                IRubyObject tmp, newary;
                if (runtime.getPosix().getrlimit(rtype, &rlim) == -1) {
                    if (errmsg != null) errmsg[0] = "getrlimit";
                    return -1;
                }
                tmp = runtime.newArray(((RubyArray)elt).eltOk(0),
                        runtime.newFixnum(rlim.rlim_cur),
                        runtime.newFixnum(rlim.rlim_max));
                if (sargp.rlimit_limits == null)
                    newary = sargp.rlimit_limits = runtime.newArray();
                else
                    newary = sargp.rlimit_limits;
                ((RubyArray)newary).push(tmp);
            }
            rlim.rlim_cur = RubyNumeric.num2int(((RubyArray)elt).eltOk(1));
            rlim.rlim_max = RubyNumeric.num2int(((RubyArray)elt).eltOk(2));
            */

            // we can't setrlimit in parent
//            if (runtime.getPosix().setrlimit(rtype, &rlim) == -1) { /* hopefully async-signal-safe */
//                if (errmsg != null) errmsg[0] = "setrlimit";
//                return -1;
//            }
        }
        return 0;
    }

    static void saveEnv(ThreadContext context, Ruby runtime, ExecArg sargp) {
        if (sargp == null)
            return;
        if (sargp.env_modification == null) {
            RubyHash env = runtime.getENV();
            if (!env.isNil()) {
                final RubyArray ary = runtime.newArray();
                BlockCallback SaveEnvBody = new BlockCallback() {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                        ary.push(args[0].dup());
                        return context.nil;
                    }
                };
                env.each(context, CallBlock.newCallClosure(env, runtime.getHash(),
                        Arity.OPTIONAL, SaveEnvBody, context));
                sargp.env_modification = ary;
            }
            sargp.unsetenv_others_given_set();
            sargp.unsetenv_others_do_set();
        }
    }

    static int run_exec_dup2(Ruby runtime, RubyArray ary, ExecArg eargp, ExecArg sargp, String[] errmsg) {
        int n, i;
        int ret;
        int extra_fd = -1;
        run_exec_dup2_fd_pair[] pairs = eargp.dup2_tmpbuf;


        n = ary.size();

        /* initialize oldfd and newfd: O(n) */
        for (i = 0; i < n; i++) {
            IRubyObject elt = ary.eltOk(i);
            pairs[i].oldfd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(1));
            pairs[i].newfd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(0)); /* unique */
            pairs[i].older_index = -1;
        }

        /* sort the table by oldfd: O(n log n) */
        if (sargp == null)
            Arrays.sort(pairs, intcmp); /* hopefully async-signal-safe */
        else
            Arrays.sort(pairs, intrcmp);

        /* initialize older_index and num_newer: O(n log n) */
        for (i = 0; i < n; i++) {
            int newfd = pairs[i].newfd;
            run_exec_dup2_fd_pair key = new run_exec_dup2_fd_pair();
            int found;
            key.oldfd = newfd;
            found = Arrays.binarySearch(pairs, key, intcmp); /* hopefully async-signal-safe */
            pairs[i].num_newer = 0;
            if (found != -1) {
                while (found > 0 && pairs[found-1].oldfd == newfd)
                    found--;
                while (found < n && pairs[found].oldfd == newfd) {
                    pairs[i].num_newer++;
                    pairs[found].older_index = i;
                    found++;
                }
            }
        }

        /* non-cyclic redirection: O(n) */
        for (i = 0; i < n; i++) {
            int j = i;
            while (j != -1 && pairs[j].oldfd != -1 && pairs[j].num_newer == 0) {
                if (saveRedirectFd(runtime, pairs[j].newfd, sargp, errmsg) < 0) /* async-signal-safe */
                    return -1;
                ret = redirectDup2(eargp, pairs[j].oldfd, pairs[j].newfd); /* async-signal-safe */
//                if (ret == -1) {
//                    if (errmsg != null) errmsg[0] = "dup2";
//                    return -1;
//                }
//                rb_update_max_fd(pairs[j].newfd); /* async-signal-safe but don't need to call it in a child process. */
                pairs[j].oldfd = -1;
                j = (int)pairs[j].older_index;
                if (j != -1)
                    pairs[j].num_newer--;
            }
        }

        /* cyclic redirection: O(n) */
        for (i = 0; i < n; i++) {
            int j;
            if (pairs[i].oldfd == -1)
                continue;
            if (pairs[i].oldfd == pairs[i].newfd) { /* self cycle */
//                #ifdef F_GETFD
                int fd = pairs[i].oldfd;
                ret = runtime.getPosix().fcntl(fd, Fcntl.F_GETFD); /* async-signal-safe */
                if (ret == -1) {
                    if (errmsg != null) errmsg[0] = "fcntl(F_GETFD)";
                    return -1;
                }
                if ((ret & FcntlLibrary.FD_CLOEXEC) != 0) {
                    ret &= ~FcntlLibrary.FD_CLOEXEC;
                    ret = runtime.getPosix().fcntl(fd, Fcntl.F_SETFD, ret); /* async-signal-safe */
                    if (ret == -1) {
                        if (errmsg != null) errmsg[0] = "fcntl(F_SETFD)";
                        return -1;
                    }
                }
//                #endif
                pairs[i].oldfd = -1;
                continue;
            }
            if (extra_fd == -1) {
                extra_fd = redirectDup(runtime, pairs[i].oldfd); /* async-signal-safe */
                if (extra_fd == -1) {
                    if (errmsg != null) errmsg[0] = "dup";
                    return -1;
                }
//                rb_update_max_fd(extra_fd);
            }
            else {
                ret = redirectDup2(eargp, pairs[i].oldfd, extra_fd); /* async-signal-safe */
//                if (ret == -1) {
//                    if (errmsg != null) errmsg[0] = "dup2";
//                    return -1;
//                }
//                rb_update_max_fd(extra_fd);
            }
            pairs[i].oldfd = extra_fd;
            j = pairs[i].older_index;
            pairs[i].older_index = -1;
            while (j != -1) {
                ret = redirectDup2(eargp, pairs[j].oldfd, pairs[j].newfd); /* async-signal-safe */
//                if (ret == -1) {
//                    if (errmsg != null) errmsg[0] = "dup2";
//                    return -1;
//                }
//                rb_update_max_fd(ret);
                pairs[j].oldfd = -1;
                j = pairs[j].older_index;
            }
        }
        if (extra_fd != -1) {
            ret = redirectClose(eargp, extra_fd); /* async-signal-safe */
            if (ret == -1) {
                if (errmsg != null) errmsg[0] = "close";
                return -1;
            }
        }

        return 0;
    }

    static int redirectDup(Ruby runtime, int oldfd)
    {
        int ret;
        ret = runtime.getPosix().dup(oldfd);
//        ttyprintf("dup(%d) => %d\n", oldfd, ret);
        return ret;
    }

    static int redirectDup2(ExecArg eargp, int oldfd, int newfd)
    {
        eargp.fileActions.add(SpawnFileAction.dup(oldfd, newfd));
        return 0;
    }

    static int redirectClose(ExecArg eargp, int fd)
    {
        eargp.fileActions.add(SpawnFileAction.close(fd));
        return 0;
    }

    static int redirectOpen(Ruby runtime, String pathname, int flags, int perm)
    {
        int ret;
        ret = runtime.getPosix().open(pathname, flags, perm);
//        ttyprintf("open(\"%s\", 0x%x, 0%o) => %d\n", pathname, flags, perm, ret);
        return ret;
    }

    static int saveRedirectFd(Ruby runtime, int fd, ExecArg sargp, String[] errmsg) {
        if (sargp != null) {
            IRubyObject newary;
            int save_fd = redirectDup(runtime, fd);
            if (save_fd == -1) {
                if (runtime.getPosix().errno() == Errno.EBADF.intValue())
                    return 0;
                if (errmsg != null) errmsg[0] = "dup";
                return -1;
            }
//            rb_update_max_fd(save_fd);
            newary = sargp.fd_dup2;
            if (newary == null) {
                newary = runtime.newArray();
                sargp.fd_dup2 = newary;
            }
            ((RubyArray)newary).push(runtime.newArray(runtime.newFixnum(fd), runtime.newFixnum(save_fd)));

            newary = sargp.fd_close;
            if (newary == null) {
                newary = runtime.newArray();
                sargp.fd_close = newary;
            }
            ((RubyArray)newary).push(runtime.newArray(runtime.newFixnum(save_fd), runtime.getNil()));
        }

        return 0;
    }

    int execargRunOptions(ThreadContext context, Ruby runtime, ExecArg eargp, ExecArg sargp, String[] errmsg) {
        IRubyObject obj;

        if (sargp != null) {
            /* assume that sargp is always NULL on fork-able environments */
            sargp.redirect_fds = context.nil;
        }

//        #ifdef HAVE_SETPGID
        if (eargp.pgroup_given()) {
            if (run_exec_pgroup(runtime, eargp, sargp, errmsg) == -1) /* async-signal-safe */
                return -1;
        }
//        #endif

//        #if defined(HAVE_SETRLIMIT) && defined(RLIM2NUM)
        obj = eargp.rlimit_limits;
        if (obj != null) {
            if (run_exec_rlimit(runtime, (RubyArray)obj, sargp, errmsg) == -1) /* hopefully async-signal-safe */
                return -1;
        }
//        #endif

//        #if !defined(HAVE_FORK)
        if (eargp.unsetenv_others_given() && eargp.unsetenv_others_do()) {
            saveEnv(context, runtime, sargp);
            // we can't clear env in parent process
//            runtime.getENV().clear();
        }

        obj = eargp.env_modification;
        if (obj != null) {
//            long i;
            saveEnv(context, runtime, sargp);
            // we can't setenv in parent process
//            for (i = 0; i < ((RubyArray)obj).size(); i++) {
//                IRubyObject pair = ((RubyArray)obj).eltOk(i);
//                IRubyObject key = ((RubyArray)pair).eltOk(0);
//                IRubyObject val = ((RubyArray)pair).eltOk(1);
//                if (val.isNil())
//                    ruby_setenv(StringValueCStr(key), 0);
//                else
//                    ruby_setenv(StringValueCStr(key), StringValueCStr(val));
//            }
        }
//        #endif

        if (eargp.umask_given()) {
            int mask = eargp.umask_mask;
            int oldmask = runtime.getPosix().umask(mask); /* never fail */ /* async-signal-safe */
            if (sargp != null) {
                sargp.umask_given_set();
                sargp.umask_mask = oldmask;
            }
        }

        obj = eargp.fd_dup2;
        if (obj != null) {
            if (run_exec_dup2(runtime, (RubyArray)obj, eargp, sargp, errmsg) == -1) /* hopefully async-signal-safe */
                return -1;
        }

        obj = eargp.fd_close;
        if (obj != null) {
            if (sargp != null)
                runtime.getWarnings().warn("cannot close fd before spawn");
            else {
                if (run_exec_close(runtime, (RubyArray)obj, eargp, errmsg) == -1) /* async-signal-safe */
                    return -1;
            }
        }

        obj = eargp.fd_open;
        if (obj != null) {
            if (run_exec_open(runtime, (RubyArray)obj, eargp, sargp, errmsg) == -1) /* async-signal-safe */
                return -1;
        }

        obj = eargp.fd_dup2_child;
        if (obj != null) {
            if (run_exec_dup2_child(runtime, (RubyArray)obj, eargp, sargp, errmsg) == -1) /* async-signal-safe */
                return -1;
        }

        if (eargp.chdir_given()) {
            if (sargp != null) {
                String cwd = runtime.getCurrentDirectory();
                sargp.chdir_given_set();
                sargp.chdir_dir = cwd;
            }
            // we can't chdir in the parent
//            if (chdir(RSTRING_PTR(eargp.chdir_dir)) == -1) { /* async-signal-safe */
//                ERRMSG("chdir");
//                return -1;
//            }
        }

//        #ifdef HAVE_SETGID
        if (eargp.gid_given()) {
            // we can't setgid in the parent
//            if (setgid(eargp.gid) < 0) {
//                ERRMSG("setgid");
//                return -1;
//            }
        }
//        #endif
//        #ifdef HAVE_SETUID
        if (eargp.uid_given()) {
            // we can't setuid in the parent
//            if (setuid(eargp.uid) < 0) {
//                ERRMSG("setuid");
//                return -1;
//            }
        }
//        #endif

        if (sargp != null) {
            IRubyObject ary = sargp.fd_dup2;
            if (ary != null) {
                int len = runExecDup2TmpbufSize(((RubyArray)ary).size());
                run_exec_dup2_fd_pair[] tmpbuf = new run_exec_dup2_fd_pair[len];
                for (int i = 0; i < tmpbuf.length; i++) tmpbuf[i] = new run_exec_dup2_fd_pair();
                sargp.dup2_tmpbuf = tmpbuf;
            }
        }

        return 0;
    }
    /* This function should be async-signal-safe.  Actually it is. */
    static int run_exec_close(Ruby runtime, RubyArray ary, ExecArg eargp, String[] errmsg) {
        long i;
        int ret;

        for (i = 0; i < ary.size(); i++) {
            RubyArray elt = (RubyArray)ary.eltOk(i);
            int fd = RubyNumeric.fix2int(elt.eltOk(0));
            ret = redirectClose(eargp, fd); /* async-signal-safe */
            if (ret == -1) {
                if (errmsg != null) errmsg[0] = "close";
                return -1;
            }
        }
        return 0;
    }

    /* This function should be async-signal-safe when sargp is NULL.  Actually it is. */
    static int run_exec_open(Ruby runtime, RubyArray ary, ExecArg eargp, ExecArg sargp, String[] errmsg) {
        int i;
        int ret;

        for (i = 0; i < ary.size();) {
            RubyArray elt = (RubyArray)ary.eltOk(i);
            int fd;
            RubyArray param = (RubyArray)elt.eltOk(1);
            String path = param.eltOk(0).toString();
            int flags = RubyNumeric.num2int(param.eltOk(1));
            int perm = RubyNumeric.num2int(param.eltOk(2));
            boolean need_close = true;
            int fd2 = redirectOpen(runtime, path, flags, perm); /* async-signal-safe */
            if (fd2 == -1) {
                if (errmsg != null) errmsg[0] = "open";
                return -1;
            }
//            rb_update_max_fd(fd2);
            while (i < ary.size() &&
                    elt.eltOk(1) == param) {
                elt = (RubyArray)ary.eltOk(i);
                fd = RubyNumeric.fix2int(elt.eltOk(0));
                if (fd == fd2) {
                    need_close = false;
                }
                else {
                    if (saveRedirectFd(runtime, fd, sargp, errmsg) < 0) /* async-signal-safe */
                        return -1;
                    ret = redirectDup2(eargp, fd2, fd); /* async-signal-safe */
                    if (ret == -1) {
                        if (errmsg != null) errmsg[0] = "dup2";
                        return -1;
                    }
//                    rb_update_max_fd(fd);
                }
                i++;
            }
            if (need_close) {
                ret = redirectClose(eargp, fd2); /* async-signal-safe */
                if (ret == -1) {
                    if (errmsg != null) errmsg[0] = "close";
                    return -1;
                }
            }
        }
        return 0;
    }

    /* This function should be async-signal-safe when sargp is NULL.  Actually it is. */
    static int run_exec_dup2_child(Ruby runtime, RubyArray ary, ExecArg eargp, ExecArg sargp, String[] errmsg) {
        long i;
        int ret;

        for (i = 0; i < ary.size(); i++) {
            RubyArray elt = (RubyArray)ary.eltOk(i);
            int newfd = RubyNumeric.fix2int(elt.eltOk(0));
            int oldfd = RubyNumeric.fix2int(elt.eltOk(0));

            if (saveRedirectFd(runtime, newfd, sargp, errmsg) < 0) /* async-signal-safe */
                return -1;
            ret = redirectDup2(eargp, oldfd, newfd); /* async-signal-safe */
            if (ret == -1) {
                if (errmsg != null) errmsg[0] = "dup2";
                return -1;
            }
//            rb_update_max_fd(newfd);
        }
        return 0;
    }

    private static class run_exec_dup2_fd_pair {
        int oldfd;
        int newfd;
        int older_index;
        int num_newer;
    };

    static int runExecDup2TmpbufSize(int n) {
        return n;
    }

    static void execargFixup(ThreadContext context, Ruby runtime, ExecArg eargp) {
        boolean unsetenv_others;
        IRubyObject envopts;
        IRubyObject ary;

        eargp.redirect_fds = checkExecFds(context, runtime, eargp);

        ary = eargp.fd_dup2;
        if (ary != null) {
            int len = runExecDup2TmpbufSize(((RubyArray)ary).size());
            run_exec_dup2_fd_pair[] tmpbuf = new run_exec_dup2_fd_pair[len];
            for (int i = 0; i < tmpbuf.length; i++) tmpbuf[i] = new run_exec_dup2_fd_pair();
            eargp.dup2_tmpbuf = tmpbuf;
        }

        unsetenv_others = eargp.unsetenv_others_given() && eargp.unsetenv_others_do();
        envopts = eargp.env_modification;
        if (unsetenv_others || envopts != null) {
            IRubyObject envtbl;
            String[] envp_str;
            List<String> envp_buf;
            if (unsetenv_others) {
                envtbl = RubyHash.newHash(runtime);
            }
            else {
                envtbl = runtime.getObject().getConstant("ENV");
                envtbl = TypeConverter.convertToType(envtbl, runtime.getHash(), "to_hash");
            }
            if (envopts != null) {
                RubyHash stenv = (RubyHash)envtbl;
                long i;
                for (i = 0; i < ((RubyArray)envopts).size(); i++) {
                    IRubyObject pair = ((RubyArray)envopts).eltOk(i);
                    IRubyObject key = ((RubyArray)pair).eltOk(0);
                    IRubyObject val = ((RubyArray)pair).eltOk(1);
                    if (val.isNil()) {
                        IRubyObject stkey = key;
                        stenv.fastDelete(stkey);
                    }
                    else {
                        stenv.op_aset(context, key, val);
                    }
                }
            }
            envp_buf = new ArrayList();
            for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)((RubyHash)envtbl).directEntrySet()) {
                IRubyObject key = entry.getKey();
                IRubyObject val = entry.getValue();

                envp_buf.add(StringSupport.checkEmbeddedNulls(runtime, key).toString()
                        + "="
                        + StringSupport.checkEmbeddedNulls(runtime, val));
            }
            envp_str = new String[envp_buf.size()];
            envp_buf.toArray(envp_str);
            eargp.envp_str = envp_str;
            eargp.envp_buf = envp_buf;

        /*
        char **tmp_envp = (char **)RSTRING_PTR(envp_str);
        while (*tmp_envp) {
            printf("%s\n", *tmp_envp);
            tmp_envp++;
        }
        */
        }
//        RB_GC_GUARD(execarg_obj);
    }

    static int checkExecFds1(ThreadContext context, Ruby runtime, ExecArg eargp, RubyHash h, int maxhint, IRubyObject ary) {
        long i;

        if (ary != null) {
            for (i = 0; i < ((RubyArray)ary).size(); i++) {
                IRubyObject elt = ((RubyArray)ary).eltOk(i);
                int fd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(0));
                if (h.fastARef(runtime.newFixnum(fd)) != null) {
                    throw runtime.newArgumentError("fd " + fd + " specified twice");
                }
                if (ary == eargp.fd_open || ary == eargp.fd_dup2)
                    h.op_aset(context, runtime.newFixnum(fd), runtime.getTrue());
                else if (ary == eargp.fd_dup2_child)
                    h.op_aset(context, runtime.newFixnum(fd), ((RubyArray)elt).eltOk(1));
                else /* ary == eargp.fd_close */
                    h.op_aset(context, runtime.newFixnum(fd), runtime.newFixnum(-1));
                if (maxhint < fd)
                    maxhint = fd;
                if (ary == eargp.fd_dup2 || ary == eargp.fd_dup2_child) {
                    fd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(1));
                    if (maxhint < fd)
                        maxhint = fd;
                }
            }
        }
        return maxhint;
    }

    static IRubyObject checkExecFds(ThreadContext context, Ruby runtime, ExecArg eargp) {
        RubyHash h = RubyHash.newHash(runtime);
        IRubyObject ary;
        int maxhint = -1;
        long i;

        maxhint = checkExecFds1(context, runtime, eargp, h, maxhint, eargp.fd_dup2);
        maxhint = checkExecFds1(context, runtime, eargp, h, maxhint, eargp.fd_close);
        maxhint = checkExecFds1(context, runtime, eargp, h, maxhint, eargp.fd_open);
        maxhint = checkExecFds1(context, runtime, eargp, h, maxhint, eargp.fd_dup2_child);

        if (eargp.fd_dup2_child != null) {
            ary = eargp.fd_dup2_child;
            for (i = 0; i < ((RubyArray)ary).size(); i++) {
                IRubyObject elt = ((RubyArray)ary).eltOk(i);
                int newfd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(0));
                int oldfd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(1));
                int lastfd = oldfd;
                IRubyObject val = h.fastARef(runtime.newFixnum(lastfd));
                long depth = 0;
                while (val instanceof RubyFixnum && 0 <= ((RubyFixnum)val).getIntValue()) {
                    lastfd = RubyNumeric.fix2int(val);
                    val = h.fastARef(val);
                    if (((RubyArray)ary).size() < depth)
                        throw runtime.newArgumentError("cyclic child fd redirection from " + oldfd);
                    depth++;
                }
                if (val != runtime.getTrue())
                    throw runtime.newArgumentError("child fd " + oldfd + " is not redirected");
                if (oldfd != lastfd) {
                    IRubyObject val2;
                    ((RubyArray)elt).store(1, runtime.newFixnum(lastfd));
                    h.op_aset(context, runtime.newFixnum(newfd), runtime.newFixnum(lastfd));
                    val = runtime.newFixnum(oldfd);
                    while ((val2 = h.fastARef(val)) instanceof RubyFixnum) {
                        h.op_aset(context, val, runtime.newFixnum(lastfd));
                        val = val2;
                    }
                }
            }
        }

        eargp.close_others_maxhint = maxhint;
        return h;
    }

    static int execargAddopt(ThreadContext context, Ruby runtime, ExecArg eargp, IRubyObject key, IRubyObject val) {
        String id;
//        #if defined(HAVE_SETRLIMIT) && defined(NUM2RLIM)
        int rtype;
//        #endif

//        rb_secure(2);

        boolean redirect = false;
        switch (key.getMetaClass().getRealClass().getClassIndex()) {
            case SYMBOL:
                id = key.toString();
//                #ifdef HAVE_SETPGID
                if (id.equals("pgroup")) {
                    long pgroup;
                    if (eargp.pgroup_given()) {
                        throw runtime.newArgumentError("pgroup option specified twice");
                    }
                    if (val == null || val.isNil())
                        pgroup = -1; /* asis(-1) means "don't call setpgid()". */
                    else if (val == runtime.getTrue())
                        pgroup = 0; /* new process group. */
                    else {
                        pgroup = val.convertToInteger().getLongValue();
                        if (pgroup < 0) {
                            throw runtime.newArgumentError("negative process group ID : " + pgroup);
                        }
                    }
                    eargp.pgroup_given_set();
                    eargp.pgroup_pgid = pgroup;
                }
                else
//                #ifdef _WIN32
//                if (id.equals("new_pgroup")) {
//                    if (eargp.new_pgroup_given) {
//                        throw runtime.newArgumentError("new_pgroup option specified twice");
//                    }
//                    eargp.new_pgroup_given = 1;
//                    eargp.new_pgroup_flag = RTEST(val) ? 1 : 0;
//                }
//                else
//                #endif
//                #if defined(HAVE_SETRLIMIT) && defined(NUM2RLIM)
                if (id.startsWith("rlimit_") &&  // TODO
                        false) {
//                        (rtype = rlimitTypeByLname(id.substring(7)) != -1)) {
                    IRubyObject ary = eargp.rlimit_limits;
                    IRubyObject tmp, softlim, hardlim;
                    if (eargp.rlimit_limits == null)
                        ary = eargp.rlimit_limits = runtime.newArray();
                    else
                        ary = eargp.rlimit_limits;
                    tmp = TypeConverter.checkArrayType(runtime, val);
                    if (!tmp.isNil()) {
                        if (((RubyArray)tmp).size() == 1)
                            softlim = hardlim = ((RubyArray)tmp).eltOk(0).convertToInteger();
                        else if (((RubyArray)tmp).size() == 2) {
                            softlim = ((RubyArray)tmp).eltOk(0).convertToInteger();
                            hardlim = ((RubyArray)tmp).eltOk(1).convertToInteger();
                        }
                        else {
                            throw runtime.newArgumentError("wrong exec rlimit option");
                        }
                    }
                    else {
                        softlim = hardlim = val.convertToInteger();
                    }
                    tmp = runtime.newArray(runtime.newFixnum(rtype), softlim, hardlim);
                    ((RubyArray)ary).push(tmp);
                }
                else
//                #endif
                if (id.equals("unsetenv_others")) {
                    if (eargp.unsetenv_others_given()) {
                        throw runtime.newArgumentError("unsetenv_others option specified twice");
                    }
                    eargp.unsetenv_others_given_set();
                    if (!val.isNil()) {
                        eargp.unsetenv_others_do_set();
                    } else {
                        eargp.unsetenv_others_do_clear();
                    }
                }
                else if (id.equals("chdir")) {
                    if (eargp.chdir_given()) {
                        throw runtime.newArgumentError("chdir option specified twice");
                    }
                    RubyString valTmp = RubyFile.get_path(context, val);
                    eargp.chdir_given_set();
                    eargp.chdir_dir = valTmp.toString();
                }
                else if (id.equals("umask")) {
                    int cmask = val.convertToInteger().getIntValue();
                    if (eargp.umask_given()) {
                        throw runtime.newArgumentError("umask option specified twice");
                    }
                    eargp.umask_given_set();
                    eargp.umask_mask = cmask;
                }
                else if (id.equals("close_others")) {
                    if (eargp.close_others_given()) {
                        throw runtime.newArgumentError("close_others option specified twice");
                    }
                    eargp.close_others_given_set();
                    if (!val.isNil()) {
                        eargp.close_others_do_set();
                    } else {
                        eargp.close_others_do_clear();
                    }
                }
                else if (id.equals("in")) {
                    key = RubyFixnum.zero(runtime);
                    checkExecRedirect(context, runtime, key, val, eargp);
                }
                else if (id.equals("out")) {
                    key = RubyFixnum.one(runtime);
                    checkExecRedirect(context, runtime, key, val, eargp);
                }
                else if (id.equals("err")) {
                    key = RubyFixnum.two(runtime);
                    checkExecRedirect(context, runtime, key, val, eargp);
                }
                else if (id.equals("uid") && false) { // TODO
//                    #ifdef HAVE_SETUID
                    if (eargp.uid_given()) {
                        throw runtime.newArgumentError("uid option specified twice");
                    }
//                    checkUidSwitch();
                    {
//                        PREPARE_GETPWNAM;
                        eargp.uid = val.convertToInteger().getIntValue();
                        eargp.uid_given_set();
                    }
//                    #else
//                    rb_raise(rb_eNotImpError,
//                            "uid option is unimplemented on this machine");
//                    #endif
                }
                else if (id.equals("gid") && false) { // TODO
//                    #ifdef HAVE_SETGID
                    if (eargp.gid_given()) {
                        throw runtime.newArgumentError("gid option specified twice");
                    }
//                    checkGidSwitch();
                    {
//                        PREPARE_GETGRNAM;
                        eargp.gid = val.convertToInteger().getIntValue();
                        eargp.gid_given_set();
                    }
//                    #else
//                    rb_raise(rb_eNotImpError,
//                            "gid option is unimplemented on this machine");
//                    #endif
                }
                else {
                    return ST_STOP;
                }
                break;

            case FIXNUM:
            case FILE:
            case IO:
            case ARRAY:
                checkExecRedirect(context, runtime, key, val, eargp);
                break;

            default:
                return ST_STOP;
        }

//        RB_GC_GUARD(execarg_obj);

        return ST_CONTINUE;
    }

    static void checkExecRedirect(ThreadContext context, Ruby runtime, IRubyObject key, IRubyObject val, ExecArg eargp) {
        IRubyObject param;
        IRubyObject path, flags, perm;
        IRubyObject tmp;
        String id;

        switch (val.getMetaClass().getRealClass().getClassIndex()) {
            case SYMBOL:
                id = val.toString();
                if (id.equals("close")) {
                    param = context.nil;
                    eargp.fd_close = checkExecRedirect1(runtime, eargp.fd_close, key, param);
                }
                else if (id.equals("in")) {
                    param = runtime.newFixnum(0);
                    eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                }
                else if (id.equals("out")) {
                    param = runtime.newFixnum(1);
                    eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                }
                else if (id.equals("err")) {
                    param = runtime.newFixnum(2);
                    eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                }
                else {
                    throw runtime.newArgumentError("wrong exec redirect symbol: " + id);
                }
                break;

            case FILE:
            case IO:
                val = checkExecRedirectFd(runtime, val, 0);
        /* fall through */
            case FIXNUM:
                param = val;
                eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                break;

            case ARRAY:
                path = ((RubyArray)val).eltOk(0);
                if (((RubyArray)val).size() == 2 && path instanceof RubySymbol &&
                        path.toString().equals("child")) {
                    param = checkExecRedirectFd(runtime, ((RubyArray)val).eltOk(1), 0);
                    eargp.fd_dup2_child = checkExecRedirect1(runtime, eargp.fd_dup2_child, key, param);
                }
                else {
                    path = RubyFile.get_path(context, path);
                    flags = ((RubyArray)val).eltOk(1);
                    int intFlags;
                    if (flags.isNil())
                        intFlags = OpenFlags.O_RDONLY.intValue();
                    else if (flags instanceof RubyString)
                        intFlags = OpenFile.ioModestrOflags(runtime, flags.toString());
                    else
                        intFlags = flags.convertToInteger().getIntValue();
                    flags = runtime.newFixnum(intFlags);
                    perm = ((RubyArray)val).eltOk(2);
                    perm = perm.isNil() ? runtime.newFixnum(0644) : perm.convertToInteger();
                    param = runtime.newArray(((RubyString)path).strDup(runtime).export(context),
                            flags, perm);
                    eargp.fd_open = checkExecRedirect1(runtime, eargp.fd_open, key, param);
                }
                break;

            case STRING:
                path = val;
                path = RubyFile.get_path(context, path);
                if (key instanceof RubyIO)
                    key = checkExecRedirectFd(runtime, key, 1);
                if (key instanceof RubyFixnum && (((RubyFixnum)key).getIntValue() == 1 || ((RubyFixnum)key).getIntValue() == 2))
                    flags = runtime.newFixnum(OpenFlags.O_WRONLY.intValue()|OpenFlags.O_CREAT.intValue()|OpenFlags.O_TRUNC.intValue());
                else
                    flags = runtime.newFixnum(OpenFlags.O_RDONLY.intValue());
                perm = runtime.newFixnum(0644);
                param = runtime.newArray(((RubyString)path).strDup(runtime).export(context),
                        flags, perm);
                eargp.fd_open = checkExecRedirect1(runtime, eargp.fd_open, key, param);
                break;

            default:
                tmp = val;
                val = TypeConverter.ioCheckIO(runtime, tmp);
                if (!val.isNil()) {
                    val = checkExecRedirectFd(runtime, val, 0);
                    param = val;
                    eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                }
                throw runtime.newArgumentError("wrong exec redirect action");
        }

    }

    static IRubyObject checkExecRedirectFd(Ruby runtime, IRubyObject v, int iskey) {
        IRubyObject tmp;
        int fd;
        if (v instanceof RubyFixnum) {
            fd = RubyNumeric.fix2int(v);
        }
        else if (v instanceof RubySymbol) {
            String id = v.toString();
            if (id.equals("in"))
                fd = 0;
            else if (id.equals("out"))
                fd = 1;
            else if (id.equals("err"))
                fd = 2;
            else
                throw runtime.newArgumentError("wrong exec redirect");
        }
        else if (!(tmp = TypeConverter.convertToTypeWithCheck(v, runtime.getIO(), "to_io")).isNil()) {
            OpenFile fptr;
            fptr = ((RubyIO)tmp).getOpenFileChecked();
            if (fptr.tiedIOForWriting != null)
                throw runtime.newArgumentError("duplex IO redirection");
            fd = ChannelDescriptor.getFilenoFromChannel(fptr.getFd());
        }
        else {
            throw runtime.newArgumentError("wrong exec redirect");
        }
        if (fd < 0) {
            throw runtime.newArgumentError("negative file descriptor");
        }
//        #ifdef _WIN32
//        else if (fd >= 3 && iskey) {
//        throw runtime.newArgumentError("wrong file descriptor (%d)", fd);
//    }
//        #endif
        return runtime.newFixnum(fd);
    }

    static IRubyObject checkExecRedirect1(Ruby runtime, IRubyObject ary, IRubyObject key, IRubyObject param) {
        if (ary == null) {
            ary = runtime.newArray();
        }
        if (!(key instanceof RubyArray)) {
            IRubyObject fd = checkExecRedirectFd(runtime, key, param.isNil() ? 0 : 1);
            ((RubyArray)ary).push(runtime.newArray(fd, param));
        }
        else {
            int i, n=0;
            for (i = 0 ; i < ((RubyArray)key).size(); i++) {
                IRubyObject v = ((RubyArray)key).eltOk(i);
                IRubyObject fd = checkExecRedirectFd(runtime, v, param.isNil() ? 0 : 1);
                ((RubyArray)ary).push(runtime.newArray(fd, param));
                n++;
            }
        }
        return ary;
    }

    private static final int ST_CONTINUE = 0;
    private static final int ST_STOP = 1;

    // rb_execarg_new
    public static ExecArg execargNew(ThreadContext context, IRubyObject[] argv, boolean accept_shell) {
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
        if (prog == null) {
            prog = (RubyString)argv_p[0][0];
            if (accept_shell && (end - beg) == 1) {
                argv_p[0] = IRubyObject.NULL_ARRAY;
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
                    List<byte[]> argv_buf = new ArrayList<>();
                    pBytes = prog.getByteList().unsafeBytes();
                    p = prog.getByteList().begin();
                    while (p < pBytes.length){
                        while (pBytes[p] == ' ' || pBytes[p] == '\t')
                            p++;
                        if (p < pBytes.length){
                            int w = p;
                            while (p < pBytes.length && pBytes[p] != ' ' && pBytes[p] != '\t')
                                p++;
                            argv_buf.add(Arrays.copyOfRange(pBytes, w, p));
                        }
                    }
                    eargp.argv_buf = argv_buf;
                    eargp.command_name = RubyString.newStringNoCopy(runtime, argv_buf.get(0));
                }
            }
        }

        if (!eargp.use_shell) {
            RubyString abspath;
            abspath = runtime.newString(dlnFindExeR(runtime, eargp.command_name.toString(), null));
            if (abspath == null)
                eargp.command_abspath = StringSupport.checkEmbeddedNulls(runtime, abspath);
            else
                eargp.command_abspath = null;
        }

        if (!eargp.use_shell && eargp.argv_buf != null) {
            int i;
            List<byte[]> argv_buf;
            argv_buf = new ArrayList();
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
                argv_buf.add(Arrays.copyOfRange(sBytes, s, argStr.size() + s));
            }
            eargp.argv_buf = argv_buf;
        }

        if (!eargp.use_shell) {
            ArgvStr argv_str = new ArgvStr();
            argv_str.argv = new byte[argc + 1][];
            int i = 0;
            argv_str.argv[i++] = null; /* place holder for /bin/sh of try_with_sh. */
            for (byte[] bytes : eargp.argv_buf) {
                argv_str.argv[i++] = bytes;
            }
            eargp.argv_str = argv_str;
        }
    }

    private static String dlnFindExeR(Ruby runtime, String fname, String path) {
        if (path != null) throw new RuntimeException("BUG: dln_find_exe_r with path is not supported yet");
        // FIXME: need to reencode path as same
        File exePath = ShellLauncher.findPathExecutable(runtime, fname);
        return exePath.getAbsolutePath();
    }

    private static class ArgvStr {
        byte[][] argv;
    }

    public static class ExecArg {
        boolean use_shell;
        RubyString shell_script;
        RubyString command_name;
        RubyString command_abspath; /* full path string or nil */
        ArgvStr argv_str;
        List<byte[]> argv_buf;
        IRubyObject redirect_fds;
        String[] envp_str;
        List<String> envp_buf;
        run_exec_dup2_fd_pair[] dup2_tmpbuf;
        int flags = 0xFFFFFFFF;
        long pgroup_pgid; /* asis(-1), new pgroup(0), specified pgroup (0<V). */
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
        String chdir_dir;
        List<SpawnFileAction> fileActions = new ArrayList();
        List<SpawnAttribute> attributes = new ArrayList();

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

    private static final Comparator<run_exec_dup2_fd_pair> intcmp = new Comparator<run_exec_dup2_fd_pair>() {
        @Override
        public int compare(run_exec_dup2_fd_pair o1, run_exec_dup2_fd_pair o2) {
            return Integer.compare(o1.oldfd, o2.oldfd);
        }
    };

    private static final Comparator<run_exec_dup2_fd_pair> intrcmp = new Comparator<run_exec_dup2_fd_pair>() {
        @Override
        public int compare(run_exec_dup2_fd_pair o1, run_exec_dup2_fd_pair o2) {
            return Integer.compare(o2.oldfd, o1.oldfd);
        }
    };
}
