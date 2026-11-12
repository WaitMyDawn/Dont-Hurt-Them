package yagen.waitmydawn.dont_hurt_them.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Nested synchronous block updates inherit a cause; scheduled ticks explicitly restore their block's recorded cause. */
public final class CauseContext {
    private static final ThreadLocal<List<UUID>> STACK = ThreadLocal.withInitial(ArrayList::new);
    public static UUID current() {
        List<UUID> stack = STACK.get();
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }
    public static void push(UUID owner) { STACK.get().add(owner); }
    public static void pop() {
        List<UUID> stack = STACK.get();
        if (!stack.isEmpty()) stack.remove(stack.size() - 1);
        if (stack.isEmpty()) STACK.remove();
    }
    private CauseContext() {}
}
