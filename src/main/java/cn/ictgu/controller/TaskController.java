package cn.ictgu.controller;

import cn.ictgu.strategy.AnyScheduleManagerFactory;
import cn.ictgu.taskmanager.ScheduleServer;
import cn.ictgu.taskmanager.ScheduleTaskItem;
import cn.ictgu.taskmanager.ScheduleTaskType;
import cn.ictgu.taskmanager.ScheduleTaskTypeRunningInfo;
import lombok.extern.log4j.Log4j;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Silence on 2016/12/22.
 */
@Controller
@RequestMapping("/task")
@Log4j
public class TaskController {

  @Autowired
  private AnyScheduleManagerFactory factory;

  @RequestMapping("/manage")
  public String taskManage(Model model) throws Exception {
    List<ScheduleTaskType> taskTypes = factory.getScheduleDataManager().getAllTaskTypeBaseInfo();
    model.addAttribute("taskTypes", taskTypes);
    return "task/index";
  }

  @RequestMapping("/run")
  public String taskRunningInfo(HttpServletRequest request, Model model) throws Exception {
    String baseTaskType = request.getParameter("baseTaskType");
    List<ScheduleTaskTypeRunningInfo> taskTypeRunningInfoList = factory.getScheduleDataManager()
                                                                       .getAllTaskTypeRunningInfo(baseTaskType);
    List<ScheduleServer> serverList = new ArrayList<>();
    List<ScheduleTaskItem> taskItemList = new ArrayList<>();
    if (taskTypeRunningInfoList.size() > 0) {
      for (ScheduleTaskTypeRunningInfo runningInfo : taskTypeRunningInfoList) {
        List<ScheduleServer> servers = factory.getScheduleDataManager().selectAllValidScheduleServer(runningInfo.getTaskType());
        serverList.addAll(servers);
        List<ScheduleTaskItem> items = factory.getScheduleDataManager().loadAllTaskItem(runningInfo.getTaskType());
        taskItemList.addAll(items);
      }
    }
    model.addAttribute("serverList", serverList);
    model.addAttribute("taskItemList", taskItemList);
    return "task/run";
  }

}
