package no.roedt

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.Permission
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.arc.profile.UnlessBuildProfile
import jakarta.enterprise.context.Dependent
import java.nio.file.Paths
import kotlin.io.path.inputStream

@Dependent
class GoogleDriveService(val credentialsFactory: GoogleCredentialsFactory) {
    fun kopleMotGoogleDrive(): Drive = Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        HttpCredentialsAdapter(credentialsFactory.createCredentials())
    )
        .setApplicationName("hypersys-googledrive")
        .build()


    fun giTilgang(epost: String): Permission = Permission().also {
        it.emailAddress = epost
        it.kind = "drive#permission"
        it.role = "writer"
        it.type = "user"
    }
}

interface GoogleCredentialsFactory {
    fun createCredentials(): GoogleCredentials
}

@Dependent
@UnlessBuildProfile("dev")
class GoogleCredentialsDefaultsFactory : GoogleCredentialsFactory {
    override fun createCredentials(): GoogleCredentials = GoogleCredentials.getApplicationDefault()
}

@Dependent
@IfBuildProfile("dev)")
class LocalCredentialsFactory : GoogleCredentialsFactory {
    override fun createCredentials(): GoogleCredentials =
        GoogleCredentials
            .fromStream(Paths.get(".service-account.json").inputStream())
            .createScoped(DriveScopes.all())
}