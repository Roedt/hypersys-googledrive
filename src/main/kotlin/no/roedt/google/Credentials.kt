package no.roedt.google

import com.google.api.services.drive.DriveScopes
import com.google.auth.oauth2.GoogleCredentials
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.arc.profile.UnlessBuildProfile
import jakarta.enterprise.context.Dependent
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.file.Paths
import kotlin.io.path.inputStream


interface GoogleCredentialsFactory {
    fun createCredentials(): GoogleCredentials
}

@Dependent
@UnlessBuildProfile("dev")
class GoogleCredentialsDefaultsFactory : GoogleCredentialsFactory {
    override fun createCredentials(): GoogleCredentials = GoogleCredentials.getApplicationDefault()
}

@Dependent
@IfBuildProfile("dev")
class LocalCredentialsFactory(@ConfigProperty(name = "google.cloud.service-account-location") val location: String) : GoogleCredentialsFactory {
    override fun createCredentials(): GoogleCredentials =
        GoogleCredentials
            .fromStream(Paths.get(location).inputStream())
            .createScoped(DriveScopes.all())
}