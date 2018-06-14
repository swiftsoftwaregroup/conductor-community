package com.netflix.conductor.client.grpc;

import com.google.common.base.Preconditions;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskExecLog;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.grpc.TaskServiceGrpc;
import com.netflix.conductor.grpc.TaskServicePb;
import com.netflix.conductor.proto.TaskPb;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskClient extends ClientBase {
    private TaskServiceGrpc.TaskServiceBlockingStub stub;

    public TaskClient(String address, int port) {
        super(address, port);
        this.stub = TaskServiceGrpc.newBlockingStub(this.channel);
    }

    /**
     * Perform a poll for a task of a specific task type.
     *
     * @param taskType The taskType to poll for
     * @param domain   The domain of the task type
     * @param workerId Name of the client worker. Used for logging.
     * @return Task waiting to be executed.
     */
    public Task pollTask(String taskType, String workerId, String domain) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskType), "Task type cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(domain), "Domain cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(workerId), "Worker id cannot be blank");

        TaskPb.Task task = stub.poll(
                TaskServicePb.PollRequest.newBuilder()
                .setTaskType(taskType)
                .setWorkerId(workerId)
                .setDomain(domain)
                .build()
        );
        return protoMapper.fromProto(task);
    }

    /**
     * Retrieve pending tasks by type
     *
     * @param taskType Type of task
     * @param startKey id of the task from where to return the results. NULL to start from the beginning.
     * @param count    number of tasks to retrieve
     * @return Returns the list of PENDING tasks by type, starting with a given task Id.
     */
    public List<Task> getPendingTasksByType(String taskType, Optional<String> startKey, Optional<Integer> count) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskType), "Task type cannot be blank");
        // TODO
        return null;
    }

    /**
     * Retrieve pending task identified by reference name for a workflow
     *
     * @param workflowId        Workflow instance id
     * @param taskReferenceName reference name of the task
     * @return Returns the pending workflow task identified by the reference name
     */
    public Task getPendingTaskForWorkflow(String workflowId, String taskReferenceName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(workflowId), "Workflow id cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(taskReferenceName), "Task reference name cannot be blank");

        TaskPb.Task task = stub.getPendingTaskForWorkflow(
                TaskServicePb.PendingTaskRequest.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskRefName(taskReferenceName)
                        .build()
        );
        return protoMapper.fromProto(task);
    }

    /**
     * Updates the result of a task execution.
     *
     * @param taskResult TaskResults to be updated.
     */
    public void updateTask(TaskResult taskResult) {
        Preconditions.checkNotNull(taskResult, "Task result cannot be null");
        stub.updateTask(protoMapper.toProto(taskResult));
    }

    /**
     * Ack for the task poll.
     *
     * @param taskId   Id of the task to be polled
     * @param workerId user identified worker.
     * @return true if the task was found with the given ID and acknowledged. False otherwise. If the server returns false, the client should NOT attempt to ack again.
     */
    public boolean ack(String taskId, String workerId) {
        // TODO: Optional<workerId>
        Preconditions.checkArgument(StringUtils.isNotBlank(taskId), "Task id cannot be blank");
        return stub.ackTask(
                TaskServicePb.AckTaskRequest.newBuilder()
                        .setTaskId(taskId)
                        .setWorkerId(workerId)
                        .build()
        ).getAck();
    }

    /**
     * Log execution messages for a task.
     *
     * @param taskId     id of the task
     * @param logMessage the message to be logged
     */
    public void logMessageForTask(String taskId, String logMessage) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskId), "Task id cannot be blank");
        stub.addLog(
                TaskServicePb.AddLogRequest.newBuilder()
                        .setTaskId(taskId)
                        .setLog(logMessage)
                        .build()
        );
    }

    /**
     * Fetch execution logs for a task.
     *
     * @param taskId id of the task.
     */
    public List<TaskExecLog> getTaskLogs(String taskId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskId), "Task id cannot be blank");
        return stub.getTaskLogs(
                TaskServicePb.TaskId.newBuilder().setTaskId(taskId).build()
        ).getLogsList()
                .stream()
                .map(protoMapper::fromProto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve information about the task
     *
     * @param taskId ID of the task
     * @return Task details
     */
    public Task getTaskDetails(String taskId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskId), "Task id cannot be blank");
        return protoMapper.fromProto(
                stub.getTask(TaskServicePb.TaskId.newBuilder().setTaskId(taskId).build())
        );
    }

    /**
     * Removes a task from a taskType queue
     *
     * @param taskType the taskType to identify the queue
     * @param taskId   the id of the task to be removed
     */
    public void removeTaskFromQueue(String taskType, String taskId) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskType), "Task type cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(taskId), "Task id cannot be blank");
        stub.removeTaskFromQueue(
                TaskServicePb.RemoveTaskRequest.newBuilder()
                        .setTaskType(taskType)
                        .setTaskId(taskId)
                        .build()
        );
    }

    public int getQueueSizeForTask(String taskType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskType), "Task type cannot be blank");

        TaskServicePb.QueueSizesResponse sizes = stub.getQueueSizesForTasks(
                TaskServicePb.QueueSizesRequest.newBuilder()
                        .addTaskTypes(taskType)
                        .build()
        );

        return sizes.getQueueForTaskOrDefault(taskType, 0);
    }
}
