package pl.pekao.zesportal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pl.pekao.zesportal.entity.Task;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.repository.TaskRepository;
import pl.pekao.zesportal.service.task.TaskExecutor;
import pl.pekao.zesportal.service.task.TaskExecutorRegistry;
import pl.pekao.zesportal.service.task.TaskProgressHolder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    
    private final TaskRepository taskRepository;
    private final TaskTemplateService taskTemplateService;
    private final TaskExecutorRegistry executorRegistry;
    private final TaskProgressHolder progressHolder;
    private final TransactionTemplate transactionTemplate;
    private ThreadPoolExecutor immediateExecutor;
    private ThreadPoolExecutor queueExecutor;
    private final BlockingQueue<Runnable> immediateQueue = new PriorityBlockingQueue<>();
    private final BlockingQueue<Runnable> normalQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread queueProcessorThread;

    public TaskService(TaskRepository taskRepository, TaskTemplateService taskTemplateService,
                       TaskExecutorRegistry executorRegistry, TaskProgressHolder progressHolder,
                       TransactionTemplate transactionTemplate) {
        this.taskRepository = taskRepository;
        this.taskTemplateService = taskTemplateService;
        this.executorRegistry = executorRegistry;
        this.progressHolder = progressHolder;
        this.transactionTemplate = transactionTemplate;
    }

    @PostConstruct
    public void init() {
        // Executor dla zadań priorytetowych (natychmiastowych)
        immediateExecutor = new ThreadPoolExecutor(
            2, 5, 60L, TimeUnit.SECONDS,
            immediateQueue
        );
        
        // Executor dla zadań w kolejce
        queueExecutor = new ThreadPoolExecutor(
            1, 3, 60L, TimeUnit.SECONDS,
            normalQueue
        );
        
        // Wątek przetwarzający kolejkę
        queueProcessorThread = new Thread(this::processQueue);
        queueProcessorThread.setDaemon(true);
        queueProcessorThread.start();
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (immediateExecutor != null) {
            immediateExecutor.shutdown();
        }
        if (queueExecutor != null) {
            queueExecutor.shutdown();
        }
        if (queueProcessorThread != null) {
            queueProcessorThread.interrupt();
        }
    }

    @Transactional
    public Task createTask(String name, String description, Task.TaskPriority priority) {
        return createTask(name, description, priority, null, null);
    }

    @Transactional
    public Task createTask(String name, String description, Task.TaskPriority priority, Long templateId) {
        return createTask(name, description, priority, templateId, null);
    }

    @Transactional
    public Task createTask(String name, String description, Task.TaskPriority priority, Long templateId, String config) {
        Task task = new Task();
        task.setName(name);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus(Task.TaskStatus.PENDING);
        if (templateId != null) {
            taskTemplateService.findById(templateId).ifPresent(task::setTaskTemplate);
        }
        if (config != null && !config.isBlank()) {
            task.setConfig(config);
        }
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> findAll() {
        return taskRepository.findAllWithTemplate();
    }

    @Transactional(readOnly = true)
    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Task> findByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public void executeTaskImmediately(Task task) {
        task.setPriority(Task.TaskPriority.IMMEDIATE);
        task.setStatus(Task.TaskStatus.RUNNING);
        task.setStartedAt(Instant.now());
        taskRepository.save(task);
        
        immediateExecutor.execute(() -> executeTask(task));
    }

    public void addTaskToQueue(Task task) {
        task.setStatus(Task.TaskStatus.PENDING);
        taskRepository.save(task);
        
        // Zadanie zostanie przetworzone przez queueProcessorThread
        synchronized (normalQueue) {
            normalQueue.notify();
        }
    }

    private void processQueue() {
        while (running.get()) {
            try {
                Task task = getNextPendingTask();
                if (task != null) {
                    // Aktualizuj status w transakcji
                    Task finalTask = task;
                    transactionTemplate.execute(status -> {
                        Task updatedTask = taskRepository.findById(finalTask.getId()).orElse(finalTask);
                        updatedTask.setStatus(Task.TaskStatus.RUNNING);
                        updatedTask.setStartedAt(Instant.now());
                        taskRepository.save(updatedTask);
                        return updatedTask;
                    });
                    
                    // Pobierz zaktualizowane zadanie
                    Task runningTask = taskRepository.findById(task.getId()).orElse(task);
                    queueExecutor.execute(() -> executeTask(runningTask));
                } else {
                    synchronized (normalQueue) {
                        normalQueue.wait(1000); // Czekaj 1 sekundę lub aż pojawi się zadanie
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private Task getNextPendingTask() {
        return transactionTemplate.execute(status -> {
            List<Task> pendingTasks = taskRepository.findByStatusOrderByCreatedAtAsc(Task.TaskStatus.PENDING);
            if (pendingTasks.isEmpty()) {
                return null;
            }
            
            // Sortuj według priorytetu: IMMEDIATE > HIGH > NORMAL > LOW
            return pendingTasks.stream()
                .sorted((t1, t2) -> {
                    int p1 = getPriorityValue(t1.getPriority());
                    int p2 = getPriorityValue(t2.getPriority());
                    return Integer.compare(p2, p1); // Wyższy priorytet pierwszy
                })
                .findFirst()
                .orElse(null);
        });
    }

    private int getPriorityValue(Task.TaskPriority priority) {
        return switch (priority) {
            case IMMEDIATE -> 4;
            case HIGH -> 3;
            case NORMAL -> 2;
            case LOW -> 1;
        };
    }

    private void executeTask(Task task) {
        Task taskToRun = taskRepository.findByIdWithTemplate(task.getId()).orElse(task);
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logger.info("=== Executing Task: {} ===", taskToRun.getName());
            logger.info("Task ID: {}", taskToRun.getId());
            logger.info("Priority: {}", taskToRun.getPriority());
            logger.info("Started at: {}", timestamp);

            TaskTemplate template = taskToRun.getTaskTemplate();
            if (template != null) {
                TaskExecutor executor = executorRegistry.getExecutor(template.getType()).orElse(null);
                if (executor != null) {
                    executor.execute(taskToRun, template);
                } else {
                    logger.warn("Brak executora dla typu szablonu: {}", template.getType());
                    Thread.sleep(1000);
                }
            } else {
                // Zadanie bez szablonu – symulacja
                Thread.sleep(2000);
            }

            // Zapisz status w transakcji
            transactionTemplate.execute(status -> {
                Task updatedTask = taskRepository.findById(taskToRun.getId()).orElse(taskToRun);
                updatedTask.setStatus(Task.TaskStatus.COMPLETED);
                updatedTask.setCompletedAt(Instant.now());
                taskRepository.save(updatedTask);
                return null;
            });
            progressHolder.clear(taskToRun.getId());

            logger.info("Task {} completed successfully", taskToRun.getName());
            logger.info("Completed at: {}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            progressHolder.clear(taskToRun.getId());
            transactionTemplate.execute(status -> {
                Task updatedTask = taskRepository.findById(taskToRun.getId()).orElse(taskToRun);
                updatedTask.setStatus(Task.TaskStatus.FAILED);
                updatedTask.setErrorMessage("Task was interrupted");
                taskRepository.save(updatedTask);
                return null;
            });
            logger.error("Task {} was interrupted", taskToRun.getName(), e);
        } catch (Exception e) {
            progressHolder.clear(taskToRun.getId());
            transactionTemplate.execute(status -> {
                Task updatedTask = taskRepository.findById(taskToRun.getId()).orElse(taskToRun);
                updatedTask.setStatus(Task.TaskStatus.FAILED);
                updatedTask.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                taskRepository.save(updatedTask);
                return null;
            });
            logger.error("Task {} failed", taskToRun.getName(), e);
        }
    }
}
