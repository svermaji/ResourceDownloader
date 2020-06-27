package com.sv.downloader;

/**
 *
 */
public class ResourceDownLoaderException extends RuntimeException {

    public ResourceDownLoaderException() {
        this("Unknown error occurred.");
    }

    public ResourceDownLoaderException(String msg) {
        super(msg);
    }

    public ResourceDownLoaderException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
