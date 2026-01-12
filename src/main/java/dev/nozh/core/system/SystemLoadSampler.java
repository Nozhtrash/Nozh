package dev.nozh.core.system;

import java.lang.management.ManagementFactory;
import java.util.OptionalDouble;

/**
 * Lightweight, defensive sampler for OS-level CPU load.
 *
 * <p>Uses {@code com.sun.management.OperatingSystemMXBean} when available, but avoids
 * hard dependency by using reflection-safe casts only when the class exists at runtime.</p>
 */
public final class SystemLoadSampler {

    private final Object osBean;

    public SystemLoadSampler() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    /**
     * @return process CPU load in range [0..1] when available.
     */
    public OptionalDouble getProcessCpuLoad() {
        return invokeDouble("getProcessCpuLoad");
    }

    /**
     * @return system CPU load in range [0..1] when available.
     */
    public OptionalDouble getSystemCpuLoad() {
        return invokeDouble("getSystemCpuLoad");
    }

    /**
     * @return system load average when available.
     */
    public OptionalDouble getSystemLoadAverage() {
        return invokeDouble("getSystemLoadAverage");
    }

    private OptionalDouble invokeDouble(String method) {
        try {
            var m = osBean.getClass().getMethod(method);
            Object v = m.invoke(osBean);
            if (!(v instanceof Double d)) {
                return OptionalDouble.empty();
            }
            if (!Double.isFinite(d) || d < 0.0) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(d);
        } catch (Throwable ignored) {
            return OptionalDouble.empty();
        }
    }
}
