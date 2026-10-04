// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Completion boundary between HTTP requests and the server's chat state. */
public final class ChatSession {
    private final Executor owner;
    private final IdentityHashMap<EntityChatData, Request> requests = new IdentityHashMap<>();
    private volatile boolean open = true;

    public ChatSession(Executor owner) {
        this.owner = owner;
    }

    public boolean isOpen() {
        return open;
    }

    /** May be called by a timer; ownership is checked again after the executor handoff. */
    public void execute(Runnable task) {
        if (open) {
            owner.execute(() -> {
                if (open) {
                    task.run();
                }
            });
        }
    }

    /** Admission, cancellation and close are called only by the owning server thread. */
    public Request tryBegin(EntityChatData data, BooleanSupplier currentState,
                            BooleanSupplier currentContext, Runnable settlePending) {
        if (!open || !currentState.getAsBoolean() || !currentContext.getAsBoolean()) {
            return null;
        }
        Request previous = requests.get(data);
        if (previous != null) {
            if (previous.currentState.getAsBoolean() && previous.currentContext.getAsBoolean()) {
                return null;
            }
            previous.cancel();
        }
        Request request = new Request(data, currentState, currentContext, settlePending);
        requests.put(data, request);
        return request;
    }

    public void cancel(EntityChatData data) {
        Request request = requests.get(data);
        if (request != null) {
            request.cancel();
        }
    }

    public void close() {
        open = false;
        for (Request request : new ArrayList<>(requests.values())) {
            request.cancel();
        }
    }

    /** Dispatch boundary shared by request triggers and their rate-limit policy. */
    public static boolean dispatch(Supplier<Request> admission, BooleanSupplier policy, Consumer<Request> action) {
        Request request = admission.get();
        if (request == null) {
            return false;
        }
        try {
            if (!policy.getAsBoolean()) {
                request.cancel();
                return false;
            }
            action.accept(request);
            return true;
        } catch (RuntimeException | Error failure) {
            request.cancel();
            throw failure;
        }
    }

    public static <T> void handoff(Executor owner, CompletableFuture<T> response, BiConsumer<T, Throwable> completion) {
        response.whenComplete((result, failure) -> owner.execute(() -> completion.accept(result, failure)));
    }

    public final class Request {
        private final EntityChatData data;
        private final BooleanSupplier currentState;
        private final BooleanSupplier currentContext;
        private final Runnable settlePending;

        private Request(EntityChatData data, BooleanSupplier currentState,
                        BooleanSupplier currentContext, Runnable settlePending) {
            this.data = data;
            this.currentState = currentState;
            this.currentContext = currentContext;
            this.settlePending = settlePending;
        }

        /** Checked only on the server owner before accepting already-admitted preparation. */
        public boolean isCurrent(EntityChatData state) {
            return state == data && open && requests.get(data) == this
                    && currentState.getAsBoolean() && currentContext.getAsBoolean();
        }

        public <T> void whenComplete(CompletableFuture<T> response, BiConsumer<T, Throwable> completion) {
            handoff(owner, response, (result, failure) -> {
                if (requests.get(data) != this) {
                    return;
                }
                try {
                    if (open && currentState.getAsBoolean() && currentContext.getAsBoolean()) {
                        completion.accept(result, failure);
                    }
                } finally {
                    cancel();
                }
            });
        }

        public void cancel() {
            if (requests.remove(data, this) && currentState.getAsBoolean()) {
                settlePending.run();
            }
        }
    }
}
