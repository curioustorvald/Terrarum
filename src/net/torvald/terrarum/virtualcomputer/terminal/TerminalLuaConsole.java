package net.torvald.terrarum.virtualcomputer.terminal;

import li.cil.repack.com.naef.jnlua.LuaException;
import li.cil.repack.com.naef.jnlua.LuaRuntimeException;
import li.cil.repack.com.naef.jnlua.LuaState;

import java.io.*;

/**
 * Created by minjaesong on 16-09-10.
 */
public class TerminalLuaConsole {
    // -- Static
    private static final String[] EMPTY_ARGS = new String[0];

    /**
     * Main routine.
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        LuaConsole luaConsole = new LuaConsole(args);
        luaConsole.run();
        System.exit(0);
    }

    private PrintStream out;
    
    // -- State
    private LuaState luaState;

    // -- Construction
    /**
     * Creates a new instance.
     */
    public TerminalLuaConsole(Terminal term) {
        this(EMPTY_ARGS, term);
    }

    /**
     * Creates a new instance with the specified command line arguments. The
     * arguments are passed to Lua as the <code>argv</code> global variable.
     *
     * @param args
     */
    public TerminalLuaConsole(String[] args, Terminal term) {
        out = new TerminalPrintStream(term);
        
        luaState = new LuaState();

        // Process arguments
        luaState.newTable(args.length, 0);
        for (int i = 0; i < args.length; i++) {
            luaState.pushString(args[i]);
            luaState.rawSet(-2, i + 1);
        }
        luaState.setGlobal("argv");

        // Open standard libraries
        luaState.openLibs();

        // Set buffer mode
        luaState.load("io.stdout:setvbuf(\"no\")", "=consoleInitStdout");
        luaState.call(0, 0);
        luaState.load("io.stderr:setvbuf(\"no\")", "=consoleInitStderr");
        luaState.call(0, 0);
    }

    // -- Properties
    /**
     * Returns the Lua state of this console.
     *
     * @return the Lua state
     */
    public LuaState getLuaState() {
        return luaState;
    }

    // -- Operations
    /**
     * Runs the console.
     */
    public void run() {
        // Banner
        out.println(String.format("JNLua %s Console using Lua %s.",
                LuaState.VERSION, LuaState.LUA_VERSION));
        out.print("Type 'go' on an empty line to evaluate a chunk. ");
        out.println("Type =<expression> to print an expression.");

        // Prepare reader
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(System.in));
        try {
            // Process chunks
            chunk: while (true) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                OutputStreamWriter outWriter = new OutputStreamWriter(out,
                        "UTF-8");
                boolean firstLine = true;

                // Process lines
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break chunk;
                    }
                    if (line.equals("go")) {
                        outWriter.flush();
                        InputStream in = new ByteArrayInputStream(out
                                .toByteArray());
                        runChunk(in);
                        continue chunk;
                    }
                    if (firstLine && line.startsWith("=")) {
                        outWriter.write("return " + line.substring(1));
                        outWriter.flush();
                        InputStream in = new ByteArrayInputStream(out
                                .toByteArray());
                        runChunk(in);
                        continue chunk;
                    }
                    outWriter.write(line);
                    outWriter.write('\n');
                    firstLine = false;
                }
            }
        } catch (IOException e) {
            out.print("IO error: ");
            out.print(e.getMessage());
            out.println();
        }
    }

    /**
     * Runs a chunk of Lua code from an input stream.
     */
    protected void runChunk(InputStream in) throws IOException {
        try {
            long start = System.nanoTime();
            luaState.setTop(0);
            luaState.load(in, "=console", "t");
            luaState.call(0, LuaState.MULTRET);
            long stop = System.nanoTime();
            for (int i = 1; i <= luaState.getTop(); i++) {
                if (i > 1) {
                    out.print(", ");
                }
                switch (luaState.type(i)) {
                    case BOOLEAN:
                        out.print(Boolean.valueOf(luaState.toBoolean(i)));
                        break;
                    case NUMBER:
                    case STRING:
                        out.print(luaState.toString(i));
                        break;
                    default:
                        out.print(luaState.typeName(i));
                }
            }
            out.print("\t#msec=");
            out.print(String.format("%.3f", (stop - start) / 1000000.0));
            out.println();
        } catch (LuaRuntimeException e) {
            e.printLuaStackTrace();
        } catch (LuaException e) {
            System.err.println(e.getMessage());
        }
    }
}
