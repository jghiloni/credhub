package org.cloudfoundry.credhub.services;

import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;
import org.cloudfoundry.credhub.services.grpc.GenerateRequest;
import org.cloudfoundry.credhub.services.grpc.GenerateResponse;
import org.cloudfoundry.credhub.services.grpc.PasswordGeneratorServiceGrpc;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

public class PasswordGeneratorService extends PasswordGeneratorServiceGrpc.PasswordGeneratorServiceImplBase {
  PasswordGenerator passwordGenerator;
  private List<CharacterRule> characterRules = new ArrayList<>();


  public PasswordGeneratorService(PasswordGenerator passwordGenerator) {
    this.passwordGenerator = passwordGenerator;
    characterRules.add(new CharacterRule(EnglishCharacterData.Digit));
    characterRules.add(new CharacterRule(EnglishCharacterData.UpperCase));
    characterRules.add(new CharacterRule(EnglishCharacterData.LowerCase));
  }

  @Override
  public void generate(GenerateRequest request, StreamObserver<GenerateResponse> responseObserver) {
    String generatedValue = passwordGenerator.generatePassword(Integer.parseInt(request.getLength()), characterRules);
    GenerateResponse response = GenerateResponse.newBuilder().setValue(generatedValue).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
