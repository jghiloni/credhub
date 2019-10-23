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
  private final PasswordGenerator passwordGenerator;
  private List<CharacterRule> characterRules = new ArrayList<>();


  public PasswordGeneratorService() {
    this.passwordGenerator = new PasswordGenerator();
    characterRules.add(new CharacterRule(EnglishCharacterData.Digit));
    characterRules.add(new CharacterRule(EnglishCharacterData.UpperCase));
    characterRules.add(new CharacterRule(EnglishCharacterData.LowerCase));
    System.out.println("*****************PasswordGeneratorService Created***************");
  }

  @Override
  public void generate(GenerateRequest request, StreamObserver<GenerateResponse> responseObserver) {
//    System.out.println("Received Generate Request for length of " + request.getLength());
    String generatedValue = passwordGenerator.generatePassword(Integer.parseInt(request.getLength()), characterRules);
    System.out.println("Generated value " + generatedValue);
    GenerateResponse response = GenerateResponse.newBuilder().setValue(generatedValue).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
