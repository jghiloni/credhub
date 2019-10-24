package org.cloudfoundry.credhub.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.cloudfoundry.credhub.config.EncryptionKeyMetadata;
import org.cloudfoundry.credhub.config.EncryptionKeysConfiguration;
import org.cloudfoundry.credhub.data.EncryptionKeyCanaryDataService;
import org.cloudfoundry.credhub.entities.EncryptedValue;
import org.cloudfoundry.credhub.entities.EncryptionKeyCanary;
import org.cloudfoundry.credhub.services.EncryptionKey;
import org.cloudfoundry.credhub.services.EncryptionProviderFactory;
import org.cloudfoundry.credhub.services.InternalEncryptionService;
import org.cloudfoundry.credhub.services.KeyProxy;
import org.cloudfoundry.credhub.services.PasswordEncryptionService;
import org.cloudfoundry.credhub.services.grpc.DecryptRequest;
import org.cloudfoundry.credhub.services.grpc.DecryptResponse;
import org.cloudfoundry.credhub.services.grpc.EncryptRequest;
import org.cloudfoundry.credhub.services.grpc.EncryptResponse;
import org.cloudfoundry.credhub.services.grpc.VersionRequest;
import org.cloudfoundry.credhub.services.grpc.VersionResponse;
import v1beta1.KeyManagementServiceGrpc;


@Service
public class KmsService extends KeyManagementServiceGrpc.KeyManagementServiceImplBase{

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

    private final static String VERSION = "v1beta1";

    private String encryptionKey = "kms-encryption-key";
    private String encryptionKeyPassword = "some-secret-key";

    KmsService(InternalEncryptionService service) {
        this.internalEncryptionService = service;
    }

    @Override
    public void version(VersionRequest request, StreamObserver<VersionResponse> responseObserver) {
        VersionResponse response = VersionResponse.newBuilder().setVersion(VERSION).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void decrypt(DecryptRequest request, StreamObserver<DecryptResponse> responseObserver) {
        ByteString cipher = request.getCipher();

        EncryptionKeyCanary encryptionKeyCanary =  encryptionKeyCanaryDataService.findAll().stream()
                .findFirst()
                .orElseThrow(()-> new RuntimeException("no canary data service"));

        String decryptedResponse;
        try {
            decryptedResponse = internalEncryptionService.decrypt(getEncryptionKey1(), cipher.toByteArray(), encryptionKeyCanary.getNonce());
        } catch (Exception e) {
            decryptedResponse = "";
        }

        DecryptResponse response = DecryptResponse.newBuilder().setPlain( ByteString.copyFromUtf8(decryptedResponse)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void encrypt(EncryptRequest request, StreamObserver<EncryptResponse> responseObserver) {
        ByteString plain = request.getPlain();

        EncryptedValue cipher;
        try {
            cipher = internalEncryptionService.encrypt(getEncryptionKey1(), plain.toString());
        } catch (Exception e) {
            cipher = new EncryptedValue();
        }

        EncryptResponse response = EncryptResponse.newBuilder().setCipher(ByteString.copyFrom(cipher.getEncryptedValue())).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

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


//    private String encryptionKey = "kms-encryption-key";
//    private String encryptionKeyPassword = "some-secret-key";
//
//    private EncryptionKey getEncryptionKey1() {
//        EncryptionKeyMetadata encryptionKeyMetadata = new EncryptionKeyMetadata();
//        encryptionKeyMetadata.setActive(true);
//        encryptionKeyMetadata.setEncryptionKeyName(encryptionKey);
//        encryptionKeyMetadata.setEncryptionPassword(encryptionKeyPassword);
//
//        KeyProxy keyProxy = internalEncryptionService.createKeyProxy(encryptionKeyMetadata);
//
//        return new EncryptionKey(
//                internalEncryptionService,
//                null,
//                keyProxy.getKey(),
//                encryptionKeyMetadata.getEncryptionKeyName());
//    }
//
//    public EncryptResponse encrypt1(EncryptRequest request) throws Exception {
//
//        String value = request.getPlaintext().toString();
//
//        EncryptedValue encryptedValue = internalEncryptionService.encrypt(getEncryptionKey1(), value);
//
//        return generateEncryptedResponse(encryptedValue);
//    }
//
//    public DecryptResponse decrypt1(DecryptRequest request) throws Exception {
//        byte[] encryptedValue = request.getCiphertext().toByteArray();
//        EncryptionKeyCanary encryptionKeyCanary =  encryptionKeyCanaryDataService.findAll().stream()
//                .findFirst()
//                .orElseThrow(()-> new RuntimeException("no canary data service"));
//
//        String decryptedValue = internalEncryptionService.decrypt(getEncryptionKey1(), encryptedValue, encryptionKeyCanary.getNonce());
//
//        return generateDecryptResponse(decryptedValue);
//    }
//
//    @Override
//    public void encrypt(EncryptRequest request) throws Exception {
//        EncryptionKey key = getEncryptionKey();
//        String value = request.getPlaintext().toString();
//
//        EncryptedValue encryptedValue = internalEncryptionService.encrypt(key, value);
//
//        return generateEncryptedResponse(encryptedValue);
//    }
//
//    @Override
//    public void decrypt(DecryptRequest request) throws Exception {
//        byte[] encryptedValue = request.getCiphertext().toByteArray();
//        EncryptionKey key = getEncryptionKey();
//
//        EncryptionKeyCanary encryptionKeyCanary =  encryptionKeyCanaryDataService.findAll().stream()
//                .findFirst()
//                .orElseThrow(()-> new RuntimeException("no canary data service"));
//
//        String decryptedResponse = internalEncryptionService.decrypt(key, encryptedValue, encryptionKeyCanary.getNonce());
//
//        return generateDecryptResponse(decryptedResponse);
//    }
//
//    private EncryptResponse generateEncryptedResponse(EncryptedValue encryptedValue) {
//        ByteString cipher = ByteString.copyFrom(encryptedValue.getEncryptedValue());
//        return EncryptResponse.newBuilder()
//                .setCiphertext(cipher)
//                .build();
//    }
//
//    private DecryptResponse generateDecryptResponse(String decryptedResponse) {
//        return DecryptResponse.newBuilder()
//                .setPlaintext(ByteString.copyFrom(decryptedResponse.getBytes()))
//                .build();
//    }
//
//    private EncryptionKey getEncryptionKey() throws Exception {
//        List<EncryptionKeyProvider> providers = encryptionKeysConfiguration.getProviders();
//        EncryptionKeyProvider provider = providers.stream()
//                .filter(p -> p.getProviderType().equals(ProviderType.INTERNAL))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("no internal provider specified"));
//
//        EncryptionKeyMetadata keyMetadata = provider.getKeys().stream()
//                .filter(k -> k.isActive().equals(true))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("no active key set"));
//
//        EncryptionProvider encryptionService = encryptionProviderFactory.getEncryptionService(provider);
//        KeyProxy keyProxy = encryptionService.createKeyProxy(keyMetadata);
//
//        return new EncryptionKey(encryptionService, null, keyProxy.getKey(), keyMetadata.getEncryptionKeyName());
//    }


}
