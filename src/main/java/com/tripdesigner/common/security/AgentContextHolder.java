package com.tripdesigner.common.security;

import com.tripdesigner.ai.trip.agent.AgentContext;

public class AgentContextHolder {
    private static final ThreadLocal<AgentContext> threadLocal = new ThreadLocal<>();

    public static void set(AgentContext context) {
        threadLocal.set(context);
    }

    public static AgentContext get() {
        return threadLocal.get();
    }

    public static void clear() {
        threadLocal.remove();
    }
}