package org.cloudfoundry.credhub.credential;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.credhub.ErrorMessages;
import org.cloudfoundry.credhub.utils.CertificateReader;
import org.cloudfoundry.credhub.utils.EmptyStringToNull;
import org.cloudfoundry.credhub.validators.MutuallyExclusive;
import org.cloudfoundry.credhub.validators.RequireAnyOf;
import org.cloudfoundry.credhub.validators.RequireCertificateMatchesPrivateKey;
import org.cloudfoundry.credhub.validators.RequireCertificateSignedByCA;
import org.cloudfoundry.credhub.validators.RequireValidCA;
import org.cloudfoundry.credhub.validators.RequireValidCertificate;
import org.cloudfoundry.credhub.validators.ValidCertificateLength;

@RequireAnyOf(message = ErrorMessages.MISSING_CERTIFICATE_CREDENTIALS, fields = {"ca", "certificate", "privateKey", })
@MutuallyExclusive(message = ErrorMessages.MIXED_CA_NAME_AND_CA, fields = {"ca", "caName", })
@ValidCertificateLength(message = ErrorMessages.INVALID_CERTIFICATE_LENGTH, fields = {"certificate", "ca", })
@RequireValidCertificate(message = ErrorMessages.INVALID_CERTIFICATE_VALUE, fields = {"certificate", })
@RequireCertificateSignedByCA(message = ErrorMessages.CERTIFICATE_WAS_NOT_SIGNED_BY_CA, fields = {"ca", })
@RequireCertificateMatchesPrivateKey(message = ErrorMessages.MISMATCHED_CERTIFICATE_AND_PRIVATE_KEY, fields = {"certificate", "privateKey", })
@RequireValidCA(message = ErrorMessages.INVALID_CA_VALUE, fields = {"ca", })
public class CertificateCredentialValue implements CredentialValue {

  @JsonDeserialize(using = EmptyStringToNull.class)
  private String ca;
  @JsonDeserialize(using = EmptyStringToNull.class)
  private String certificate;
  @JsonDeserialize(using = EmptyStringToNull.class)
  private String privateKey;
  @JsonDeserialize(using = EmptyStringToNull.class)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String caName;

  private boolean transitional;
  private boolean certificateAuthority;
  private boolean selfSigned;

  @SuppressWarnings("unused")
  public CertificateCredentialValue() {
    super();
  }

  // TODO: remove this implementation. actually maybe not

  public CertificateCredentialValue(
    final String ca,
    final String certificate,
    final String privateKey,
    final String caName) {
    this(ca, certificate, privateKey, caName, false, false, false);
  }
  public CertificateCredentialValue(
    final String ca,
    final String certificate,
    final String privateKey,
    final String caName,
    final boolean certificateAuthority,
    final boolean selfSigned) {
    this(ca, certificate, privateKey, caName, certificateAuthority, selfSigned, false);
  }

  public CertificateCredentialValue(
    final String ca,
    final String certificate,
    final String privateKey,
    final String caName,
    final boolean certificateAuthority,
    final boolean selfSigned,
    final boolean transitional) {

    super();
    this.ca = ca;
    this.certificate = certificate;
    this.privateKey = privateKey;
    this.transitional = transitional;
    this.certificateAuthority = certificateAuthority;
    this.selfSigned = selfSigned;
    setCaName(caName);
  }

  public String getCa() {
    return ca;
  }

  public void setCa(final String ca) {
    this.ca = ca;
  }

  public String getCertificate() {
    return certificate;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getCaName() {
    return caName;
  }

  public void setCaName(final String caName) {
    this.caName = StringUtils.prependIfMissing(caName, "/");
  }

  public boolean isTransitional() {
    return transitional;
  }

  public void setTransitional(final boolean transitional) {
    this.transitional = transitional;
  }

  public Instant getExpiryDate() {
    return new CertificateReader(certificate).getNotAfter();
  }

  public boolean isCertificateAuthority() {
    return certificateAuthority;
  }

  public void setCertificateAuthority(boolean certificateAuthority) {
    this.certificateAuthority = certificateAuthority;
  }

  public void setSelfSigned(boolean selfSigned) {
    this.selfSigned = selfSigned;
  }

  public boolean isSelfSigned() {
    return selfSigned;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CertificateCredentialValue that = (CertificateCredentialValue) o;
    return transitional == that.transitional &&
      certificateAuthority == that.certificateAuthority &&
      selfSigned == that.selfSigned &&
      Objects.equals(ca, that.ca) &&
      Objects.equals(certificate, that.certificate) &&
      Objects.equals(privateKey, that.privateKey) &&
      Objects.equals(caName, that.caName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ca, certificate, privateKey, caName, transitional, certificateAuthority, selfSigned);
  }
}
