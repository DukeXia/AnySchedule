package cn.ictgu.controller.api;

import cn.ictgu.strategy.AnyScheduleManagerFactory;
import cn.ictgu.taskmanager.ScheduleTaskType;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task Api
 * Created by Silence on 2016/12/23.
 */
@RestController
@RequestMapping("/task")
public class TaskApi {

  @Autowired
  private AnyScheduleManagerFactory factory;

  @RequestMapping("/update")
  public String update(HttpServletRequest request){
    String baseTaskType = request.getParameter("baseTaskType");
    String message = "success";
    ScheduleTaskType scheduleTaskType = new ScheduleTaskType();
    scheduleTaskType.setBaseTaskType(baseTaskType);
    scheduleTaskType.setDealBeanName(request.getParameter("dealBeanName"));
    scheduleTaskType.setHeartBeatRate(StringUtils.isEmpty(request.getParameter("heartBeatRate"))?0:Long.valueOf(request.getParameter("heartBeatRate")));
    scheduleTaskType.setJudgeDeadInterval(StringUtils.isEmpty(request.getParameter("judgeDeadInterval"))?0:Long.valueOf(request.getParameter("judgeDeadInterval")));
    scheduleTaskType.setThreadNumber(StringUtils.isEmpty(request.getParameter("threadNumber"))?0:Integer.valueOf(request.getParameter("threadNumber")));
    scheduleTaskType.setFetchDataNumber(StringUtils.isEmpty(request.getParameter("fetchDataNumber"))?0:Integer.valueOf(request.getParameter("fetchDataNumber")));
    scheduleTaskType.setExecuteNumber(StringUtils.isEmpty(request.getParameter("executeNumber"))?0:Integer.valueOf(request.getParameter("executeNumber")));
    scheduleTaskType.setSleepTimeNoData(StringUtils.isEmpty(request.getParameter("sleepTimeNoData"))?0:Integer.valueOf(request.getParameter("sleepTimeNoData")));
    scheduleTaskType.setSleepTimeInterval(StringUtils.isEmpty(request.getParameter("sleepTimeInterval"))?0:Integer.valueOf(request.getParameter("sleepTimeInterval")));
    scheduleTaskType.setMaxTaskItemsOfOneThreadGroup(StringUtils.isEmpty(request.getParameter("maxTaskItemsOfOneThreadGroup"))?0:Integer.valueOf(request.getParameter("maxTaskItemsOfOneThreadGroup")));
    scheduleTaskType.setProcessorType(request.getParameter("processorType"));
    scheduleTaskType.setPermitRunStartTime(request.getParameter("permitRunStartTime"));
    scheduleTaskType.setPermitRunEndTime(request.getParameter("permitRunEndTime"));
    scheduleTaskType.setTaskParameter(request.getParameter("taskParameter"));
    scheduleTaskType.setTaskItems(ScheduleTaskType.splitTaskItem(request.getParameter("taskItems")));
    try {
      factory.getScheduleDataManager().updateBaseTaskType(scheduleTaskType);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }


  @RequestMapping("/insert")
  public String insert(HttpServletRequest request){
    String baseTaskType = request.getParameter("baseTaskType");
    String message = "success";
    ScheduleTaskType scheduleTaskType = new ScheduleTaskType();
    scheduleTaskType.setBaseTaskType(baseTaskType);
    scheduleTaskType.setDealBeanName(request.getParameter("dealBeanName"));
    scheduleTaskType.setHeartBeatRate(StringUtils.isEmpty(request.getParameter("heartBeatRate"))?0:Long.valueOf(request.getParameter("heartBeatRate")));
    scheduleTaskType.setJudgeDeadInterval(StringUtils.isEmpty(request.getParameter("judgeDeadInterval"))?0:Long.valueOf(request.getParameter("judgeDeadInterval")));
    scheduleTaskType.setThreadNumber(StringUtils.isEmpty(request.getParameter("threadNumber"))?0:Integer.valueOf(request.getParameter("threadNumber")));
    scheduleTaskType.setFetchDataNumber(StringUtils.isEmpty(request.getParameter("fetchDataNumber"))?0:Integer.valueOf(request.getParameter("fetchDataNumber")));
    scheduleTaskType.setExecuteNumber(StringUtils.isEmpty(request.getParameter("executeNumber"))?0:Integer.valueOf(request.getParameter("executeNumber")));
    scheduleTaskType.setSleepTimeNoData(StringUtils.isEmpty(request.getParameter("sleepTimeNoData"))?0:Integer.valueOf(request.getParameter("sleepTimeNoData")));
    scheduleTaskType.setSleepTimeInterval(StringUtils.isEmpty(request.getParameter("sleepTimeInterval"))?0:Integer.valueOf(request.getParameter("sleepTimeInterval")));
    scheduleTaskType.setMaxTaskItemsOfOneThreadGroup(StringUtils.isEmpty(request.getParameter("maxTaskItemsOfOneThreadGroup"))?0:Integer.valueOf(request.getParameter("maxTaskItemsOfOneThreadGroup")));
    scheduleTaskType.setProcessorType(request.getParameter("processorType"));
    scheduleTaskType.setPermitRunStartTime(request.getParameter("permitRunStartTime"));
    scheduleTaskType.setPermitRunEndTime(request.getParameter("permitRunEndTime"));
    scheduleTaskType.setTaskParameter(request.getParameter("taskParameter"));
    scheduleTaskType.setTaskItems(ScheduleTaskType.splitTaskItem(request.getParameter("taskItems")));
    System.err.println(scheduleTaskType.toString());
    try {
      factory.getScheduleDataManager().createBaseTaskType(scheduleTaskType);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/clear")
  public String clear(HttpServletRequest request){
    String baseTaskType = request.getParameter("baseTaskType");
    String message = "success";
    try {
      factory.getScheduleDataManager().clearTaskType(baseTaskType);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/delete")
  public String delete(HttpServletRequest request){
    String baseTaskType = request.getParameter("baseTaskType");
    String message = "success";
    try {
      factory.getScheduleDataManager().deleteTaskType(baseTaskType);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/stop")
  public String stop(HttpServletRequest request){
    String baseTaskType = request.getParameter("baseTaskType");
    String message = "success";
    try {
      factory.getScheduleDataManager().pauseAllServer(baseTaskType);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/resume")
  public String resume(HttpServletRequest request){
    String baseTaskType = request.getParameter("baseTaskType");
    String message = "success";
    try {
      factory.getScheduleDataManager().resumeAllServer(baseTaskType);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

}

