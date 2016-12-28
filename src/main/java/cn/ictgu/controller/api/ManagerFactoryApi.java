package cn.ictgu.controller.api;

import cn.ictgu.strategy.AnyScheduleManagerFactory;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 机器管理API
 * Created by Silence on 2016/12/23.
 */
@RestController
@RequestMapping("/factory")
public class ManagerFactoryApi {

  @Autowired
  private AnyScheduleManagerFactory factory;

  @RequestMapping("/stop")
  public String stop(HttpServletRequest request){
    String uuid = request.getParameter("uuid");
    String message = "success";
    try {
      factory.getScheduleStrategyManager().updateManagerFactoryInfo(uuid, false);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

  @RequestMapping("/start")
  public String start(HttpServletRequest request){
    String uuid = request.getParameter("uuid");
    String message = "success";
    try {
      factory.getScheduleStrategyManager().updateManagerFactoryInfo(uuid, true);
    }catch (Throwable e){
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

}
