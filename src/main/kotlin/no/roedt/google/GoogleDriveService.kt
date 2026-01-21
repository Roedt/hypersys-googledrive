package no.roedt.google

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.arc.profile.UnlessBuildProfile
import jakarta.enterprise.context.Dependent
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.file.Paths
import kotlin.io.path.inputStream

@Dependent
class GoogleDriveService(val credentialsFactory: GoogleCredentialsFactory) {
    private val typeMappe = "application/vnd.google-apps.folder"

    fun giTilgangTilMappe(lagOgFolk: Map<String, List<String?>>, rotmappenavn: String) {
        println("Bruker credentials-factory $credentialsFactory")

        val service = kopleMotGoogleDrive()

        val rotmappeId = finnRotmappe(service, rotmappenavn)

        val undermapper = finnUndermapper(service, rotmappeId)

        lagOgFolk.forEach { (lag, folk) ->
            val lagetsMappe = undermapper.singleOrNull { it.name == lag }
            if (lagetsMappe == null) {
                lagMappe(service, lag, rotmappeId)
            }
            if (lagetsMappe != null) {
                folk.filterNotNull().forEach { person ->
                    giTilgangTilMappe(service, lagetsMappe, person)
                }
            }
        }
    }

    private fun kopleMotGoogleDrive(): Drive = Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        HttpCredentialsAdapter(credentialsFactory.createCredentials())
    )
        .setApplicationName("hypersys-googledrive")
        .build()

    private fun finnRotmappe(service: Drive, rotmappenavn: String): File = (service.files().list()
        .setFields("files(id, parents)")
        .setQ("mimeType = '$typeMappe' and name = '$rotmappenavn'")
        .execute()
        .filter { (((it.value as? File)))?.parents == null }
        ["files"] as List<*>)
        .single() as File

    private fun lagMappe(service: Drive, lag: String, rotmappeId: File) =
        service.files().create(File().also {
            it.name = lag
            it.parents = listOf(rotmappeId.id)
            it.mimeType = typeMappe
        }).execute()

    private fun finnUndermapper(service: Drive, forelder: File): List<File> {
        var pageToken: String? = null
        val files = mutableListOf<File>()
        do {
            val result = service.files().list()
                .setFields("files(id, name)")
                .setPageToken(pageToken)
                .setQ("mimeType = '$typeMappe' and '${forelder.id}' in parents").execute()
            files.addAll(result.files.filterNotNull())
            pageToken = result.nextPageToken
        } while (pageToken != null)
        return files
    }

    private fun giTilgangTilMappe(service: Drive, file: File, person: String) =
        service.permissions()
            .create(file.id, giTilgang(person)).execute()

    private fun giTilgang(epost: String): Permission = Permission().also {
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
@IfBuildProfile("dev")
class LocalCredentialsFactory(@ConfigProperty(name = "google.cloud.service-account-location") val location: String) : GoogleCredentialsFactory {
    override fun createCredentials(): GoogleCredentials =
        GoogleCredentials
            .fromStream(Paths.get(location).inputStream())
            .createScoped(DriveScopes.all())
}