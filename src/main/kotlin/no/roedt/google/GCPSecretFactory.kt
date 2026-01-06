package no.roedt.google

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.Dependent
import org.eclipse.microprofile.config.inject.ConfigProperty

@Dependent
class GCPSecretFactory(
    @ConfigProperty(name = "secretManagerProjectId", defaultValue = "")
    var secretManagerProjectId: String,
) {
    private lateinit var client: SecretManagerServiceClient

    @PostConstruct
    fun setup() {
        client = SecretManagerServiceClient.create()
    }


    fun getHypersysClientId() = getSecretFromSecretManager(GCPSecretManagerKey.hypersysClientId)

    fun getHypersysClientSecret() = getSecretFromSecretManager(GCPSecretManagerKey.hypersysClientSecret)

    private fun getSecretFromSecretManager(secretName: GCPSecretManagerKey): String {
        val secretVersionName = SecretVersionName.of(secretManagerProjectId, secretName.name, "latest")
        return client.accessSecretVersion(secretVersionName).payload.data.toStringUtf8()
    }
}

private enum class GCPSecretManagerKey {
    privatekey,
    frontendTokenKey,
    hypersysBrukerId,
    hypersysBrukerSecret,
    hypersysClientId,
    hypersysClientSecret,
    frontendSystembruker,
    frontendSystembrukerPassord,
    encryptionKey,
}