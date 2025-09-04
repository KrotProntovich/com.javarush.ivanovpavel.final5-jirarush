package com.javarush.jira.bugtracking.task;

import com.javarush.jira.bugtracking.Handlers;
import com.javarush.jira.bugtracking.task.to.ActivityTo;
import com.javarush.jira.bugtracking.task.to.TimeTo;
import com.javarush.jira.common.error.DataConflictException;
import com.javarush.jira.login.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
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

    public TimeTo timeSpentCalculation(long taskId, String statusCode1, String statusCode2) {
        List<Activity> activities1 = handler.getRepository().findByTaskIdAndStatusCode(taskId, statusCode1);
        List<Activity> activities2 = handler.getRepository().findByTaskIdAndStatusCode(taskId, statusCode2);
        if(activities1.isEmpty() || activities2.isEmpty() || activities1.size() != activities2.size()) {
           throw new RuntimeException("The task does not exist or the process is not finished");
        }
        List<LocalDateTime> inStatusCode1 = new ArrayList<>();
        List<LocalDateTime> inStatusCode2 = new ArrayList<>();
        switch (statusCode1){
            case "in_progress":
                activities1.forEach(activity -> {inStatusCode1.add(activity.getTaskStart());});
                activities2.forEach(activity -> {inStatusCode2.add(activity.getDevelopmentCompletion());});
                break;
            case "ready_for_review":
                activities1.forEach(activity -> {inStatusCode1.add(activity.getDevelopmentCompletion());});
                activities2.forEach(activity -> {inStatusCode2.add(activity.getTestingCompletion());});
        }
        LocalDateTime min = Collections.min(inStatusCode1);
        LocalDateTime max = Collections.max(inStatusCode2);
        return timeDifferenceCalculation(min, max);
    }

    private TimeTo timeDifferenceCalculation(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        LocalDateTime result = LocalDateTime.ofInstant(Instant.ofEpochSecond(duration.toSeconds()), ZoneId.systemDefault());
        TimeTo timeTo = new TimeTo();
        timeTo.setMonth((result.getMonthValue()-1)+"");
        timeTo.setDay((result.getDayOfMonth()-1)+"");
        timeTo.setTime(result.toLocalTime());
        return timeTo;
    }
}
