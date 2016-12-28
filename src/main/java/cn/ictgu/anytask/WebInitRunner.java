package cn.ictgu.anytask;

import cn.ictgu.strategy.AnyScheduleManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

/**
 * Web应用启动时的初始化操作
 * Created by Silence on 2016/12/22.
 */
public class WebInitRunner implements CommandLineRunner {

  @Autowired
  private AnyScheduleManagerFactory factory;

  @Override
  public void run(String... strings) throws Exception {
    factory.init();
  }

}
