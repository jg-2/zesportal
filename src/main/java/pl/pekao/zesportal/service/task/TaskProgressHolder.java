package pl.pekao.zesportal.service.task;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe holder for current task progress. Used by Tuxedo executor and by MainLayout footer.
 */
@Component
public class TaskProgressHolder {

    private final AtomicReference<ProgressSnapshot> current = new AtomicReference<>();

    public void setProgress(long taskId, String taskName, int currentStep, int maxSteps) {
        double value = maxSteps <= 0 ? 0 : Math.min(1.0, (double) (currentStep + 1) / maxSteps);
        current.set(new ProgressSnapshot(taskId, taskName, currentStep, maxSteps, value));
    }

    public void clear(long taskId) {
        current.updateAndGet(snap -> snap != null && snap.taskId == taskId ? null : snap);
    }

    public void clearAny() {
        current.set(null);
    }

    public ProgressSnapshot getCurrent() {
        return current.get();
    }

    public record ProgressSnapshot(long taskId, String taskName, int currentStep, int maxSteps, double progress0To1) {}
}
