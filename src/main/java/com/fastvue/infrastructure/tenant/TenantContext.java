package com.fastvue.infrastructure.tenant;

import java.util.function.Supplier;

/** Request-scoped tenant identity used by the SQL tenant interceptor. */
public final class TenantContext {

    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, boolean superAdmin) {
        CURRENT.set(new State(tenantId, superAdmin));
    }

    public static Long tenantId() {
        State state = CURRENT.get();
        return state == null ? null : state.tenantId();
    }

    public static boolean isSuperAdmin() {
        State state = CURRENT.get();
        return state != null && state.superAdmin();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static <T> T runAs(Long tenantId, boolean superAdmin, Supplier<T> action) {
        State previous = CURRENT.get();
        try {
            set(tenantId, superAdmin);
            return action.get();
        } finally {
            if (previous == null) {
                clear();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void runAs(Long tenantId, boolean superAdmin, Runnable action) {
        runAs(tenantId, superAdmin, () -> {
            action.run();
            return null;
        });
    }

    private record State(Long tenantId, boolean superAdmin) {
    }
}
