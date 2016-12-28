package cn.ictgu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Zookeeper Controller
 * Created by Silence on 2016/12/24.
 */
@Controller
public class ZookeeperController {

  @RequestMapping("/zookeeper")
  public String zookeeper() {
    return "zookeeper";
  }

}
