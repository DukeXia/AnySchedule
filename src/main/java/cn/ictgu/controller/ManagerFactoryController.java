package cn.ictgu.controller;

import cn.ictgu.strategy.ManagerFactoryInfo;
import cn.ictgu.strategy.AnyScheduleManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 *
 * Created by Silence on 2016/12/22.
 */
@Controller
public class ManagerFactoryController {

  @Autowired
  private AnyScheduleManagerFactory factory;

  @RequestMapping("/")
  public String factoryManage(Model model) throws Exception{
    List<ManagerFactoryInfo> list =  factory.getScheduleStrategyManager().loadAllManagerFactoryInfo();
    model.addAttribute("managerFactoryInfoList", list);
    return "factory/index";
  }

}
