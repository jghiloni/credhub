package org.cloudfoundry.credhub.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.kms.v1.DecryptRequest;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptRequest;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.protobuf.ByteString;
import org.cloudfoundry.credhub.config.EncryptionKeyMetadata;
import org.cloudfoundry.credhub.config.EncryptionKeyProvider;
import org.cloudfoundry.credhub.config.EncryptionKeysConfiguration;
import org.cloudfoundry.credhub.config.ProviderType;
import org.cloudfoundry.credhub.data.EncryptionKeyCanaryDataService;
import org.cloudfoundry.credhub.entities.EncryptedValue;
import org.cloudfoundry.credhub.entities.EncryptionKeyCanary;
import org.cloudfoundry.credhub.services.EncryptionKey;
import org.cloudfoundry.credhub.services.EncryptionProvider;
import org.cloudfoundry.credhub.services.EncryptionProviderFactory;
import org.cloudfoundry.credhub.services.InternalEncryptionService;
import org.cloudfoundry.credhub.services.KeyProxy;
import org.cloudfoundry.credhub.services.PasswordEncryptionService;

@Service
public class KmsService {

    @Autowired
    InternalEncryptionService internalEncryptionService;
    @Autowired
    EncryptionKeysConfiguration encryptionKeysConfiguration;
    @Autowired
    private EncryptionProviderFactory encryptionProviderFactory;
    @Autowired
    private EncryptionKeyCanaryDataService encryptionKeyCanaryDataService;

    @Autowired
    PasswordEncryptionService passwordEncryptionService;

    private String encryptionKey = "kms-encryption-key";
    private String encryptionKeyPassword = "some-secret-key";

    private EncryptionKey getEncryptionKey1() {
        EncryptionKeyMetadata encryptionKeyMetadata = new EncryptionKeyMetadata();
        encryptionKeyMetadata.setActive(true);
        encryptionKeyMetadata.setEncryptionKeyName(encryptionKey);
        encryptionKeyMetadata.setEncryptionPassword(encryptionKeyPassword);

        KeyProxy keyProxy = internalEncryptionService.createKeyProxy(encryptionKeyMetadata);

        return new EncryptionKey(
                internalEncryptionService,
                null,
                keyProxy.getKey(),
                encryptionKeyMetadata.getEncryptionKeyName());
    }

    public EncryptResponse encrypt1(EncryptRequest request) throws Exception {

        String value = request.getPlaintext().toString();

        EncryptedValue encryptedValue = internalEncryptionService.encrypt(getEncryptionKey1(), value);

        return generateEncryptedResponse(encryptedValue);
    }

    public DecryptResponse decrypt1(DecryptRequest request) throws Exception {
        byte[] encryptedValue = request.getCiphertext().toByteArray();
        EncryptionKeyCanary encryptionKeyCanary =  encryptionKeyCanaryDataService.findAll().stream()
                .findFirst()
                .orElseThrow(()-> new RuntimeException("no canary data service"));

        String decryptedValue = internalEncryptionService.decrypt(getEncryptionKey1(), encryptedValue, encryptionKeyCanary.getNonce());

        return generateDecryptResponse(decryptedValue);
    }

    public EncryptResponse encrypt(EncryptRequest request) throws Exception {
        EncryptionKey key = getEncryptionKey();
        String value = request.getPlaintext().toString();

        EncryptedValue encryptedValue = internalEncryptionService.encrypt(key, value);

        return generateEncryptedResponse(encryptedValue);
    }

    public DecryptResponse decrypt(DecryptRequest request) throws Exception {
        byte[] encryptedValue = request.getCiphertext().toByteArray();
        EncryptionKey key = getEncryptionKey();

        EncryptionKeyCanary encryptionKeyCanary =  encryptionKeyCanaryDataService.findAll().stream()
                .findFirst()
                .orElseThrow(()-> new RuntimeException("no canary data service"));

        String decryptedResponse = internalEncryptionService.decrypt(key, encryptedValue, encryptionKeyCanary.getNonce());

        return generateDecryptResponse(decryptedResponse);
    }

    private EncryptResponse generateEncryptedResponse(EncryptedValue encryptedValue) {
        ByteString cipher = ByteString.copyFrom(encryptedValue.getEncryptedValue());
        return EncryptResponse.newBuilder()
                .setCiphertext(cipher)
                .build();
    }

    private DecryptResponse generateDecryptResponse(String decryptedResponse) {
        return DecryptResponse.newBuilder()
                .setPlaintext(ByteString.copyFrom(decryptedResponse.getBytes()))
                .build();
    }

    private EncryptionKey getEncryptionKey() throws Exception {
        List<EncryptionKeyProvider> providers = encryptionKeysConfiguration.getProviders();
        EncryptionKeyProvider provider = providers.stream()
                .filter(p -> p.getProviderType().equals(ProviderType.INTERNAL))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no internal provider specified"));

        EncryptionKeyMetadata keyMetadata = provider.getKeys().stream()
                .filter(k -> k.isActive().equals(true))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no active key set"));

        EncryptionProvider encryptionService = encryptionProviderFactory.getEncryptionService(provider);
        KeyProxy keyProxy = encryptionService.createKeyProxy(keyMetadata);

        return new EncryptionKey(encryptionService, null, keyProxy.getKey(), keyMetadata.getEncryptionKeyName());
    }


}
