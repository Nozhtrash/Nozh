package dev.nozh.core.testing;

import java.io.IOException;
import java.nio.file.Path;

public final class ChaosTestCli {
    private ChaosTestCli() {
    }

    public static void main(String[] args) throws IOException {
        Path outputDir = Path.of("build/reports/chaos");
        boolean failOnError = true;
        for (int i = 0; i < args.length; i++) {
            if ("--output".equals(args[i]) && i + 1 < args.length) {
                outputDir = Path.of(args[i + 1]);
                i++;
            } else if ("--no-fail".equals(args[i])) {
                failOnError = false;
            }
        }
        ChaosTestReport report = ChaosTestRunner.runAll();
        ChaosTestReportWriter.writeJson(outputDir, report);
        ChaosTestReportWriter.writeCsv(outputDir, report);
        if (failOnError && !report.allPassed()) {
            System.exit(1);
        }
    }
}
