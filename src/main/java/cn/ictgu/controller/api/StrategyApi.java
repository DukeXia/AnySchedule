package cn.ictgu.controller.api;

import cn.ictgu.strategy.ScheduleStrategy;
import cn.ictgu.strategy.AnyScheduleManagerFactory;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Strategy Api
 * Created by Silence on 2016/12/23.
 */
@RestController
@RequestMapping("/strategy")
public class StrategyApi {

  @Autowired
  private AnyScheduleManagerFactory factory;

  @RequestMapping("/stop")
  public String stop(HttpServletRequest request){
    String strategyName = request.getParameter("strategyName");
    String message = "success";
    try {
      factory.getScheduleStrategyManager().pause(strategyName);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/resume")
  public String resume(HttpServletRequest request){
    String strategyName = request.getParameter("strategyName");
    String message = "success";
    try {
      factory.getScheduleStrategyManager().resume(strategyName);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/insert")
  public String create(HttpServletRequest request){
    String strategyName = request.getParameter("strategyName");
    String message = "success";
    ScheduleStrategy scheduleStrategy = new ScheduleStrategy();
    scheduleStrategy.setStrategyName(strategyName);
    scheduleStrategy.setKind(ScheduleStrategy.Kind.valueOf(request.getParameter("kind")));
    scheduleStrategy.setTaskName(request.getParameter("taskName"));
    scheduleStrategy.setTaskParameter(request.getParameter("taskParameter"));
    scheduleStrategy.setNumOfSingleServer(request.getParameter("numOfSingleServer") == null ? 0 : Integer.valueOf(request.getParameter("numOfSingleServer")));
    scheduleStrategy.setAssignNum(request.getParameter("assignNum") == null ? 0 : Integer.valueOf(request.getParameter("assignNum")));
    scheduleStrategy.setIPList(request.getParameterValues("ips") == null ? new String[0] : request.getParameter("ips").split(","));
    try {
      factory.getScheduleStrategyManager().createScheduleStrategy(scheduleStrategy);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/update")
  public String update(HttpServletRequest request){
    String strategyName = request.getParameter("strategyName");
    String message = "success";
    ScheduleStrategy scheduleStrategy = new ScheduleStrategy();
    scheduleStrategy.setStrategyName(strategyName);
    scheduleStrategy.setKind(ScheduleStrategy.Kind.valueOf(request.getParameter("kind")));
    scheduleStrategy.setTaskName(request.getParameter("taskName"));
    scheduleStrategy.setTaskParameter(request.getParameter("taskParameter"));
    scheduleStrategy.setNumOfSingleServer(request.getParameter("numOfSingleServer") == null ? 0 : Integer.valueOf(request.getParameter("numOfSingleServer")));
    scheduleStrategy.setAssignNum(request.getParameter("assignNum") == null ? 0 : Integer.valueOf(request.getParameter("assignNum")));
    scheduleStrategy.setIPList(request.getParameterValues("ips") == null ? new String[0] : request.getParameter("ips").split(","));
    try {
      factory.getScheduleStrategyManager().updateScheduleStrategy(scheduleStrategy);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/delete")
  public String delete(HttpServletRequest request){
    String strategyName = request.getParameter("strategyName");
    String message = "success";
    try {
      factory.getScheduleStrategyManager().deleteMachineStrategy(strategyName);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }
}
