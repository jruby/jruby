package org.prism;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class PrismWasmWrapper {
    private final Instance prism;
    private final ExportFunction calloc;
    private final ExportFunction free;
    private final ExportFunction pmSerializeParse;
    private final ExportFunction pmBufferInit;
    private final ExportFunction pmBufferSizeof;
    private final ExportFunction pmBufferValue;
    private final ExportFunction pmBufferLength;

    public PrismWasmWrapper() {
        // Configure Java Platform Logging
        final InputStream inputStream = this.getClass().getResourceAsStream("/logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (final IOException e) {
            Logger.getAnonymousLogger().severe("Could not load default logging.properties file");
            Logger.getAnonymousLogger().severe(e.getMessage());
        }

        this.prism = Module.builder(this.getClass().getResourceAsStream("/prism.wasm")).build().instantiate();

        this.calloc = prism.export("calloc");
        this.free = prism.export("free");
        this.pmSerializeParse = prism.export("pm_serialize_parse");
        this.pmBufferInit = prism.export("pm_buffer_init");
        this.pmBufferSizeof = prism.export("pm_buffer_sizeof");
        this.pmBufferValue = prism.export("pm_buffer_value");
        this.pmBufferLength = prism.export("pm_buffer_length");
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

    public byte[] parse(byte[] source, int sourceLength, byte[] options) {
        int sourcePointer = calloc(1, source.length);
        int optionsPointer = calloc(1, options.length);
        int resultPointer = calloc(pmBufferSizeof.apply()[0].asInt(), 1);

        pmBufferInit(resultPointer);
        prism.memory().write(optionsPointer, options);
        prism.memory().write(sourcePointer, source);

        pmSerializeParse(resultPointer, sourcePointer, sourceLength, optionsPointer);

        byte[] serialized;
        try {
            serialized = prism.memory().readBytes(pmBufferValue(resultPointer), pmBufferLength(resultPointer));
        } finally {
            free(sourcePointer);
            free(optionsPointer);
            free(resultPointer);
        }

        return serialized;
    }
}
