package com.javarush.jira.bugtracking.task;

import com.javarush.jira.bugtracking.Handlers;
import com.javarush.jira.bugtracking.task.to.ActivityTo;
import com.javarush.jira.common.error.DataConflictException;
import com.javarush.jira.login.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.javarush.jira.bugtracking.task.TaskUtil.getLatestValue;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final TaskRepository taskRepository;

    private final Handlers.ActivityHandler handler;

    private static void checkBelong(HasAuthorId activity) {
        if (activity.getAuthorId() != AuthUser.authId()) {
            throw new DataConflictException("Activity " + activity.getId() + " doesn't belong to " + AuthUser.get());
        }
    }

    @Transactional
    public Activity create(ActivityTo activityTo) {
        checkBelong(activityTo);
        Task task = taskRepository.getExisted(activityTo.getTaskId());
        if (activityTo.getStatusCode() != null) {
            task.checkAndSetStatusCode(activityTo.getStatusCode());
        }
        if (activityTo.getTypeCode() != null) {
            task.setTypeCode(activityTo.getTypeCode());
        }
        return handler.createFromTo(activityTo);
    }

    @Transactional
    public void update(ActivityTo activityTo, long id) {
        checkBelong(handler.getRepository().getExisted(activityTo.getId()));
        handler.updateFromTo(activityTo, id);
        updateTaskIfRequired(activityTo.getTaskId(), activityTo.getStatusCode(), activityTo.getTypeCode());
    }

    @Transactional
    public void delete(long id) {
        Activity activity = handler.getRepository().getExisted(id);
        checkBelong(activity);
        handler.delete(activity.id());
        updateTaskIfRequired(activity.getTaskId(), activity.getStatusCode(), activity.getTypeCode());
    }

    private void updateTaskIfRequired(long taskId, String activityStatus, String activityType) {
        if (activityStatus != null || activityType != null) {
            Task task = taskRepository.getExisted(taskId);
            List<Activity> activities = handler.getRepository().findAllByTaskIdOrderByUpdatedDesc(task.id());
            if (activityStatus != null) {
                String latestStatus = getLatestValue(activities, Activity::getStatusCode);
                if (latestStatus == null) {
                    throw new DataConflictException("Primary activity cannot be delete or update with null values");
                }
                task.setStatusCode(latestStatus);
            }
            if (activityType != null) {
                String latestType = getLatestValue(activities, Activity::getTypeCode);
                if (latestType == null) {
                    throw new DataConflictException("Primary activity cannot be delete or update with null values");
                }
                task.setTypeCode(latestType);
            }
        }
    }

    public String howLongTaskInProgress(long taskId) {
        Activity activityInProgress = handler.getRepository().findByTaskIdAndStatusCode(taskId, "in_progress");
        Activity activityReadyForReview = handler.getRepository().findByTaskIdAndStatusCode(taskId, "ready_for_review");
        if (activityInProgress != null && activityReadyForReview != null) {
            LocalDateTime taskStart = activityInProgress.getTaskStart();
            LocalDateTime developmentCompletion = activityReadyForReview.getDevelopmentCompletion();
            return timeCounting(taskStart, developmentCompletion);
        }
        return "The task does not exist or it is in progress";
    }

    public String howLongTaskInTesting(long taskId) {
        Activity activityReadyForReview = handler.getRepository().findByTaskIdAndStatusCode(taskId, "ready_for_review");
        Activity activityDone = handler.getRepository().findByTaskIdAndStatusCode(taskId, "done");
        if(activityReadyForReview != null && activityDone != null) {
            LocalDateTime developmentCompletion = activityReadyForReview.getDevelopmentCompletion();
            LocalDateTime testingCompletion = activityDone.getDevelopmentCompletion();
            return timeCounting(developmentCompletion, testingCompletion);
        }
        return "The task does not exist or it is in the testing stage";
    }

    private String timeCounting (LocalDateTime start, LocalDateTime end) {
        long startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        long resultMillis = endMillis - startMillis;
        System.out.println(resultMillis);

        LocalDateTime result = Instant.ofEpochMilli(resultMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
        return "Month:" + (result.getMonth().getValue()-1) + ", Day:" + (result.getDayOfMonth()-1) + ", Hour:" + result.getHour() + ", Minute:" + result.getMinute();
    }
}
