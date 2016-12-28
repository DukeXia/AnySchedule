package cn.ictgu.controller;

import cn.ictgu.strategy.ScheduleStrategy;
import cn.ictgu.strategy.ScheduleStrategyRuntime;
import cn.ictgu.strategy.AnyScheduleManagerFactory;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by Silence on 2016/12/22.
 */
@Controller
@RequestMapping("/strategy")
public class StrategyController {

  @Autowired
  private AnyScheduleManagerFactory factory;

  @RequestMapping("/manage")
  public String strategyManage(Model model) throws Exception{
    List<ScheduleStrategy> scheduleStrategyList =  factory.getScheduleStrategyManager().loadAllScheduleStrategy();
    model.addAttribute("scheduleStrategyList", scheduleStrategyList);
    return "strategy/index";
  }

  @RequestMapping("/detail")
  public String detail(HttpServletRequest request, Model model) throws Exception{
    String strategyName = request.getParameter("strategyName");
    String uuid = request.getParameter("uuid");
    List<ScheduleStrategyRuntime> runtimeList = new ArrayList<>();
    if (StringUtils.isNotEmpty(strategyName) && strategyName.trim().length() > 0){
      runtimeList = factory.getScheduleStrategyManager().loadAllScheduleStrategyRuntimeByStrategyName(strategyName);
    }else if(StringUtils.isNotEmpty(uuid) && uuid.trim().length() > 0){
      runtimeList = factory.getScheduleStrategyManager().loadAllScheduleStrategyRuntimeByUUID(uuid);
    }
    model.addAttribute("runtimeList", runtimeList);
    return "strategy/detail";
  }

}
