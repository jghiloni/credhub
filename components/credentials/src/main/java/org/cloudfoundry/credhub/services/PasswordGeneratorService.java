package org.cloudfoundry.credhub.services;

import io.grpc.stub.StreamObserver;
import org.cloudfoundry.credhub.credential.StringCredentialValue;
import org.cloudfoundry.credhub.generators.PasswordCredentialGenerator;
import org.cloudfoundry.credhub.requests.StringGenerationParameters;
import org.cloudfoundry.credhub.services.grpc.GenerateRequest;
import org.cloudfoundry.credhub.services.grpc.GenerateResponse;
import org.cloudfoundry.credhub.services.grpc.PasswordGeneratorServiceGrpc;

public class PasswordGeneratorService extends PasswordGeneratorServiceGrpc.PasswordGeneratorServiceImplBase {
  private PasswordCredentialGenerator passwordCredentialGenerator;


  public PasswordGeneratorService(PasswordCredentialGenerator passwordCredentialGenerator) {
    this.passwordCredentialGenerator = passwordCredentialGenerator;
    System.out.println("*****************PasswordGeneratorService Created***************");
  }

  @Override
  public void generate(GenerateRequest request, StreamObserver<GenerateResponse> responseObserver) {
    System.out.println("Received Generate Request for length of " + request.getLength());
    StringGenerationParameters generationParameters = new StringGenerationParameters();
    generationParameters.setLength(Integer.parseInt(request.getLength()));
    StringCredentialValue generatedValue = passwordCredentialGenerator.generateCredential(generationParameters);
    String stringGeneratedValue = generatedValue.getStringCredential();
    System.out.println("Generated value " + stringGeneratedValue);
    GenerateResponse response = GenerateResponse.newBuilder().setValue(stringGeneratedValue).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
