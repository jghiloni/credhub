package org.cloudfoundry.credhub.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import org.cloudfoundry.credhub.CredhubTestApp;
import org.cloudfoundry.credhub.utils.DatabaseProfileResolver;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.cloudfoundry.credhub.helpers.RequestHelper.generateRsa;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredhubTestApp.class)
@Transactional
public class RsaGenerateTest {
  private static final String CREDENTIAL_NAME = "/set_credential";

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @Before
  public void setup() {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(webApplicationContext)
      .apply(springSecurity())
      .build();
  }

  @Test
  public void credentialNotOverwrittenWhenModeIsSetToConvergeAndParametersAreTheSame() throws Exception {
    final String firstResponse = generateRsa(mockMvc, CREDENTIAL_NAME, false, 2048);
    final String originalPassword = (new JSONObject(firstResponse)).getString("value");

    final String secondResponse = generateRsa(mockMvc, CREDENTIAL_NAME, false, 2048);
    final String samePassword = (new JSONObject(secondResponse)).getString("value");

    assertThat(originalPassword, equalTo(samePassword));
  }

  @Test
  public void credentialNotOverwrittenWhenModeIsSetToConvergeAndParametersAreTheSameAndAreTheDefault() throws Exception {
    final String firstResponse = generateRsa(mockMvc, CREDENTIAL_NAME, true, null);
    final String originalPassword = (new JSONObject(firstResponse)).getString("value");

    final String secondResponse = generateRsa(mockMvc, CREDENTIAL_NAME, false, null);
    final String samePassword = (new JSONObject(secondResponse)).getString("value");

    assertThat(originalPassword, equalTo(samePassword));
  }

  @Test
  public void credentialOverwrittenWhenModeIsSetToConvergeAndParametersNotTheSame() throws Exception {
    final String firstResponse = generateRsa(mockMvc, CREDENTIAL_NAME, true, 4096);
    final String originalPassword = (new JSONObject(firstResponse)).getString("value");

    final String secondResponse = generateRsa(mockMvc, CREDENTIAL_NAME, false, 2048);
    final String updatedPassword = (new JSONObject(secondResponse)).getString("value");

    assertThat(originalPassword, not(equalTo(updatedPassword)));
  }
}
