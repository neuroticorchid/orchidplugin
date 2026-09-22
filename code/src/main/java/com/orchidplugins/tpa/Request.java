package com.orchidplugins.tpa;

import java.util.UUID;

public class Request {

    public enum Type {
        TPA, TPAHERE
    }

    private final UUID requester;
    private final UUID target;
    private final Type type;
    private final long expiresAt;

    public Request(UUID requester, UUID target, Type type, long ttlMillis) {
        this.requester = requester;
        this.target = target;
        this.type = type;
        this.expiresAt = System.currentTimeMillis() + ttlMillis;
    }

    public UUID getRequester() {
        return requester;
    }

    public UUID getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}