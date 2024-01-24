package org.prism;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class PrismWasmWrapper implements Closeable {
    private final Instance prism;
    private final ExportFunction calloc;
    private final ExportFunction free;
    private final ExportFunction pmSerializeParse;
    private final ExportFunction pmBufferInit;
    private final ExportFunction pmBufferSizeof;
    private final ExportFunction pmBufferValue;
    private final ExportFunction pmBufferLength;
    int sourcePointer = -1;
    int optionsPointer = -1;
    int resultPointer = -1;

    public PrismWasmWrapper() {
        // Configure Java Platform Logging
        final InputStream inputStream = this.getClass().getResourceAsStream("/logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (final IOException e) {
            Logger.getAnonymousLogger().severe("Could not load default logging.properties file");
            Logger.getAnonymousLogger().severe(e.getMessage());
        }

        this.prism = Module.build(
                this.getClass().getResourceAsStream("/prism.wasm")
        ).instantiate();

        this.calloc = prism.getExport("calloc");
        this.free = prism.getExport("free");
        this.pmSerializeParse = prism.getExport("pm_serialize_parse");
        this.pmBufferInit = prism.getExport("pm_buffer_init");
        this.pmBufferSizeof = prism.getExport("pm_buffer_sizeof");
        this.pmBufferValue = prism.getExport("pm_buffer_value");
        this.pmBufferLength = prism.getExport("pm_buffer_length");
    }

    private int calloc(int bytes, int length) {
        return calloc.apply(Value.i32(bytes), Value.i32(length))[0].asInt();
    }

    private void free(int pointer) {
        free.apply(Value.i32(pointer));
    }

    private void pmBufferInit(int pointer) {
        pmBufferInit.apply(Value.i32(pointer));
    }

    private int pmBufferValue(int pointer) {
        return pmBufferValue.apply(Value.i32(pointer))[0].asInt();
    }

    private int pmBufferLength(int pointer) {
        return pmBufferLength.apply(Value.i32(pointer))[0].asInt();
    }

    private void pmSerializeParse(int resultPointer, int sourcePointer, int sourceLength, int optionsPointer) {
        pmSerializeParse.apply(Value.i32(resultPointer), Value.i32(sourcePointer), Value.i32(sourceLength), Value.i32(optionsPointer));
    }

    public byte[] parse(byte[] source, byte[] options) {
        this.sourcePointer = calloc(1, source.length);
        this.optionsPointer = calloc(1, options.length);
        this.resultPointer = calloc(pmBufferSizeof.apply()[0].asInt(), 1);

        pmBufferInit(resultPointer);
        prism.getMemory().write(optionsPointer, options);
        prism.getMemory().write(sourcePointer, source);

        pmSerializeParse(resultPointer, sourcePointer, source.length, optionsPointer);

        byte[] result = prism.getMemory().readBytes(pmBufferValue(resultPointer), pmBufferLength(resultPointer));

        return result;
    }

    @Override
    public void close() {
        if (sourcePointer != -1) {
            free(sourcePointer);
            sourcePointer = -1;
        }
        if (optionsPointer != -1) {
            free(optionsPointer);
            optionsPointer = -1;
        }
        if (resultPointer != -1) {
            free(resultPointer);
            resultPointer = -1;
        }
    }
}
