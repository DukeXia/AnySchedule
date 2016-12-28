package cn.ictgu;

import cn.ictgu.anytask.WebInitRunner;
import cn.ictgu.config.ZookeeperProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AnySchedule
 * Created by Silence on 2016/12/19.
 */
@SpringBootApplication
@EnableConfigurationProperties({ZookeeperProperties.class})
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  //应用启动时，直接初始化AnyScheduleManagerFactory，连接Zookeeper
  @Bean
  public WebInitRunner init(){
    return new WebInitRunner();
  }

}
