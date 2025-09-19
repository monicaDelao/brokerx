package com.brokerx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class AppApplication {
  // 
  private static final Logger log = LoggerFactory.getLogger(AppApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(AppApplication.class, args);
    // 
    log.info("Pipeline test: nouvelle version déployée ✅");
  }
}
