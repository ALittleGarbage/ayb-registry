package com.ayb.registry.common.exception;

/**
 * 自定义异常
 *
 * @author ayb
 * @date 2023/7/13
 */
public class AybRegistryException extends RuntimeException {

    public AybRegistryException(String message) {
        super(message);
    }

    public AybRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public static AybRegistryException cast(String message) {
        throw new AybRegistryException(message);
    }
}
