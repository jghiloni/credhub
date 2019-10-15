package org.cloudfoundry.credhub.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.cloud.kms.v1.EncryptRequest;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import org.cloudfoundry.credhub.services.InternalEncryptionService;


public class KmsController {

  @Autowired
  InternalEncryptionService internalEncryptionService;

  public EncryptResponse encrypt(EncryptRequest request) throws IOException {
    KeyManagementServiceClient client = KeyManagementServiceClient.create();

      return client.encrypt(request);

    return null;
  }
}
