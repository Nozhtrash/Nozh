package dev.nozh.core.system;

import java.lang.management.ManagementFactory;
import java.util.OptionalDouble;

/**
 * Lightweight, defensive sampler for OS-level CPU load.
 */
public final class SystemLoadSampler {

    private final Object osBean;

    public SystemLoadSampler() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    public OptionalDouble getProcessCpuLoad() {
        return invokeDouble("getProcessCpuLoad");
    }

    public OptionalDouble getSystemCpuLoad() {
        return invokeDouble("getSystemCpuLoad");
    }

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
        } catch (ReflectiveOperationException | RuntimeException e) {\r\n            // Method not available on this JVM implementation
            return OptionalDouble.empty();
        }
    }
}
