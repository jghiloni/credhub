package io.pivotal.security.service;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.config.NoExpirationSymmetricKeySecurityConfiguration;
import io.pivotal.security.entity.NamedStringSecret;
import io.pivotal.security.entity.NamedValueSecret;
import io.pivotal.security.entity.OperationAuditRecord;
import io.pivotal.security.fake.FakeAuditRecordRepository;
import io.pivotal.security.fake.FakeSecretRepository;
import io.pivotal.security.fake.FakeTransactionManager;
import io.pivotal.security.util.InstantFactoryBean;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static com.greghaskins.spectrum.Spectrum.*;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Spectrum.class)
@SpringApplicationConfiguration(classes = CredentialManagerApp.class)
@ActiveProfiles({"unit-test", "NoExpirationSymmetricKeySecurityConfiguration"})
public class DatabaseAuditLogServiceTest {

  @Autowired
  @InjectMocks
  DatabaseAuditLogService subject;

  FakeAuditRecordRepository auditRepository;

  FakeSecretRepository secretRepository;

  FakeTransactionManager transactionManager;

  @Mock
  InstantFactoryBean instantFactoryBean;

  @Autowired
  TokenStore tokenStore;

  @Autowired
  ResourceServerTokenServices tokenServices;

  AuditRecordParameters auditRecordParameters;

  private Instant now;

  private ResponseEntity<?> responseEntity;

  {
    wireAndUnwire(this);

    beforeEach(() -> {
      OAuth2Authentication authentication = tokenStore.readAuthentication(NoExpirationSymmetricKeySecurityConfiguration.EXPIRED_SYMMETRIC_KEY_JWT);
      OAuth2AuthenticationDetails mockDetails = mock(OAuth2AuthenticationDetails.class);
      when(mockDetails.getTokenValue()).thenReturn(NoExpirationSymmetricKeySecurityConfiguration.EXPIRED_SYMMETRIC_KEY_JWT);
      authentication.setDetails(mockDetails);

      auditRecordParameters = new AuditRecordParameters("hostName", "requestURI", "127.0.0.1", "1.2.3.4,5.6.7.8", authentication);
      transactionManager = new FakeTransactionManager();
      auditRepository = new FakeAuditRecordRepository(transactionManager);
      secretRepository = new FakeSecretRepository(transactionManager);
      subject.auditRecordRepository = auditRepository;
      subject.transactionManager = transactionManager;

      now = Instant.now();
      when(instantFactoryBean.getObject()).thenReturn(now);
    });

    afterEach(() -> {
      auditRepository.deleteAll();
      secretRepository.deleteAll();
    });

    describe("logging behavior", () -> {
      describe("when the operation succeeds", () -> {
        describe("when the audit succeeds", () -> {
          beforeEach(() -> {
            responseEntity = subject.performWithAuditing("credential_access", auditRecordParameters, () -> {
              final NamedStringSecret secret = secretRepository.save(new NamedValueSecret("key", "value"));
              return new ResponseEntity<>(secret, HttpStatus.OK);
            });
          });

          it("passes the request untouched", () -> {
            assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
          });

          it("logs audit entry", () -> {
            checkAuditRecord(true);
            assertThat(secretRepository.count(), equalTo(1L));
          });
        });

        describe("when the audit fails", () -> {
          beforeEach(() -> {
            auditRepository.failOnSave();

            responseEntity = subject.performWithAuditing("credential_access", auditRecordParameters, () -> {
              final NamedStringSecret secret = secretRepository.save(new NamedValueSecret("key", "value"));
              return new ResponseEntity<>(secret, HttpStatus.OK);
            });
          });

          it("writes nothing to any database", () -> {
            assertThat(auditRepository.count(), equalTo(0L));
            assertThat(secretRepository.count(), equalTo(0L));
          });

          it("returns 500", () -> {
            assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
            assertThat(responseEntity.getBody(), hasJsonPath("$.error", equalTo("The request could not be completed. Please contact your system administrator to resolve this issue.")));
          });
        });
      });

      describe("when the operation fails with an exception", () -> {
        describe("when the audit succeeds", () -> {
          beforeEach(() -> {
            responseEntity = subject.performWithAuditing("credential_access", auditRecordParameters, () -> {
              secretRepository.save(new NamedValueSecret("key", "value"));
              throw new RuntimeException("controller method failed");
            });
          });

          it("leaves the 500 response from the controller alone", () -> {
            assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
          });

          it("logs failed audit entry", () -> {
            checkAuditRecord(false);
            assertThat(secretRepository.count(), equalTo(0L));
          });
        });

        describe("when the audit fails", () -> {
          beforeEach(() -> {
            auditRepository.failOnSave();

            responseEntity = subject.performWithAuditing("credential_access", auditRecordParameters, () -> {
              secretRepository.save(new NamedValueSecret("key", "value"));
              throw new RuntimeException("controller method failed");
            });
          });

          it("rolls back both original and audit repository transactions", () -> {
            assertThat(auditRepository.count(), equalTo(0L));
            assertThat(secretRepository.count(), equalTo(0L));
          });

          it("returns 500 and original error message", () -> {
            assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
            assertThat(responseEntity.getBody(), hasJsonPath("$.error", equalTo("The request could not be completed. Please contact your system administrator to resolve this issue.")));
          });
        });
      });

      describe("when the operation fails with a non 200 status", () -> {
        describe("when the audit succeeds", () -> {
          beforeEach(() -> {
            responseEntity = subject.performWithAuditing("credential_access", auditRecordParameters, () -> {
              secretRepository.save(new NamedValueSecret("key", "value"));
              return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
            });
          });

          it("logs audit entry for failure", () -> {
            checkAuditRecord(false);
            assertThat(secretRepository.count(), equalTo(0L));
          });

          it("returns the non-2xx status code", () -> {
            assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.BAD_GATEWAY));
          });
        });

        describe("when the audit fails", () -> {
          beforeEach(() -> {
            auditRepository.failOnSave();
            responseEntity = subject.performWithAuditing("credential_access", auditRecordParameters, () -> {
              secretRepository.save(new NamedValueSecret("key", "value"));
              return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
            });
          });

          it("rolls back both original and audit repository transactions", () -> {
            assertThat(transactionManager.hasOpenTransaction(), is(false));
            assertThat(auditRepository.count(), equalTo(0L));
            assertThat(secretRepository.count(), equalTo(0L));
          });

          it("returns 500", () -> {
            assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
            assertThat(responseEntity.getBody(), hasJsonPath("$.error", equalTo("The request could not be completed. Please contact your system administrator to resolve this issue.")));
          });
        });

        describe("when audit transaction fails to commit", () -> {
          beforeEach(() -> {
            transactionManager.failOnCommit();
            responseEntity = subject.performWithAuditing("credential_access", auditRecordParameters, () -> {
              secretRepository.save(new NamedValueSecret("key", "value"));
              return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
            });
          });

          it("doesn't rollback transaction", () -> {
            assertThat(transactionManager.hasOpenTransaction(), is(false));
            assertThat(auditRepository.count(), equalTo(0L));
            assertThat(secretRepository.count(), equalTo(0L));
          });

          it("returns 500", () -> {
            assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
            assertThat(responseEntity.getBody(), hasJsonPath("$.error", equalTo("The request could not be completed. Please contact your system administrator to resolve this issue.")));
          });
        });
      });
    });
  }

  private void checkAuditRecord(boolean successFlag) {
    List<OperationAuditRecord> auditRecords = auditRepository.findAll();
    assertThat(auditRecords, hasSize(1));

    OperationAuditRecord actual = auditRecords.get(0);
    assertThat(actual.getNow(), equalTo(now));
    assertThat(actual.getOperation(), equalTo("credential_access"));
    assertThat(actual.getUserId(), equalTo("1cc4972f-184c-4581-987b-85b7d97e909c"));
    assertThat(actual.getUserName(), equalTo("credhub_cli"));
    assertThat(actual.getUaaUrl(), equalTo("https://52.204.49.107:8443/oauth/token"));
    assertThat(actual.getTokenIssued(), equalTo(1469051704L));
    assertThat(actual.getTokenExpires(), equalTo(1469051824L));
    assertThat(actual.getHostName(), equalTo("hostName"));
    assertThat(actual.getPath(), equalTo("requestURI"));
    assertThat(actual.isSuccess(), equalTo(successFlag));
    assertThat(actual.getRequesterIp(), equalTo("127.0.0.1"));
    assertThat(actual.getXForwardedFor(), equalTo("1.2.3.4,5.6.7.8"));
  }
}