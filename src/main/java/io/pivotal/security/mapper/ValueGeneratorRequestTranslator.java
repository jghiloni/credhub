package io.pivotal.security.mapper;

import com.jayway.jsonpath.DocumentContext;
import io.pivotal.security.controller.v1.StringSecretParameters;
import io.pivotal.security.entity.NamedValueSecret;
import io.pivotal.security.generator.SecretGenerator;
import io.pivotal.security.view.StringSecret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import javax.validation.ValidationException;

@Component
public class ValueGeneratorRequestTranslator implements RequestTranslator<NamedValueSecret>, SecretGeneratorRequestTranslator<StringSecretParameters> {

  @Autowired
  SecretGenerator<StringSecretParameters, StringSecret> stringSecretGenerator;

  @Override
  public StringSecretParameters validRequestParameters(DocumentContext parsed) throws ValidationException {
    StringSecretParameters secretParameters = new StringSecretParameters();
    String secretType = parsed.read("$.type", String.class);
    secretParameters.setType(secretType);
    Optional.ofNullable(parsed.read("$.parameters.length", Integer.class))
        .ifPresent(secretParameters::setLength);
    Optional.ofNullable(parsed.read("$.parameters.exclude_lower", Boolean.class))
        .ifPresent(secretParameters::setExcludeLower);
    Optional.ofNullable(parsed.read("$.parameters.exclude_upper", Boolean.class))
        .ifPresent(secretParameters::setExcludeUpper);
    Optional.ofNullable(parsed.read("$.parameters.exclude_number", Boolean.class))
        .ifPresent(secretParameters::setExcludeNumber);
    Optional.ofNullable(parsed.read("$.parameters.exclude_special", Boolean.class))
        .ifPresent(secretParameters::setExcludeSpecial);

    if (!secretParameters.isValid()) {
      throw new ValidationException("error.excludes_all_charsets");
    }
    return secretParameters;
  }

  @Override
  public void populateEntityFromJson(NamedValueSecret entity, DocumentContext documentContext) {
    StringSecretParameters requestParameters = validRequestParameters(documentContext);
    StringSecret secret = stringSecretGenerator.generateSecret(requestParameters);
    entity.setValue(secret.getValue());
  }
}
