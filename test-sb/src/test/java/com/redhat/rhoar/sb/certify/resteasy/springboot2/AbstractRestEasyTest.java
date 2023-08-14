package com.redhat.rhoar.sb.certify.resteasy.springboot2;

import com.redhat.rhoar.sb.TestParent;
import cz.xtf.client.Http;
import groovy.util.logging.Slf4j;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public abstract class AbstractRestEasyTest extends TestParent {
  protected static final String APP_NAME = "resteasy-sample-app";
  protected static String appUrl;

  @Test
  public void helloWorldTest() throws Exception {
    String response = httpGetResponse(appUrl);
    Assertions.assertThat(response).isEqualTo("Hello world!");
  }

  @Test
  public void echoTest() throws Exception {
    String echoData = "random string to echo";

    String responseBody = Http.post(appUrl)
      .data("text="+echoData,ContentType.APPLICATION_FORM_URLENCODED)
      .trustAll()
      .execute()
      .response();

    Assertions.assertThat(responseBody).isEqualTo("echo:"+echoData);
  }
}
