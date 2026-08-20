package dev.csyhorizon.chatbe.domain.report.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class SignatureVerifier {

    // 송신자의 공개키, 서명 대상 원본(암호문), 첨부된 서명을 통해 위조 여부를 판단
    public boolean verifySignature(String publicKeyBase64, String originalData, String signatureBase64) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            
            // 프론트엔드에서 주로 사용하는 ECDSA 또는 RSA에 맞춤 (여기서는 예시로 ECDSA)
            KeyFactory keyFactory = KeyFactory.getInstance("EC"); 
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(originalData.getBytes());

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return sig.verify(signatureBytes);

        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }
}
