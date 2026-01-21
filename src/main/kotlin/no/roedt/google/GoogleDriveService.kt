package no.roedt.google

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import jakarta.enterprise.context.Dependent
import java.io.FileOutputStream
import java.nio.file.Paths
import kotlin.io.path.createDirectories

@Dependent
class GoogleDriveService(val credentialsFactory: GoogleCredentialsFactory) {
    companion object {
        const val TYPE_MAPPE = "application/vnd.google-apps.folder"
    }

    fun giTilgangTilMappe(lagOgFolk: Map<String, List<String?>>, rotmappenavn: String) {
        println("Bruker credentials-factory $credentialsFactory")

        val service = kopleMotGoogleDrive()
        val rotmappeId = finnRotmappe(service, rotmappenavn)
        val undermapper = finnUndermapper(service, rotmappeId)

        Tilgangsstyrer.giTilgang(service, lagOgFolk, undermapper, rotmappeId)
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
        .setQ("mimeType = '$TYPE_MAPPE' and name = '$rotmappenavn'")
        .execute()
        .filter { (((it.value as? File)))?.parents == null }
        ["files"] as List<*>)
        .single() as File

    private fun finnUndermapper(service: Drive, forelder: File): List<File> =
        finnFiler(service, "mimeType = '$TYPE_MAPPE' and '${forelder.id}' in parents")

    private fun finnFiler(service: Drive, filter: String ): List<File> {
        var pageToken: String? = null
        val files = mutableListOf<File>()
        do {
            val result = service.files().list()
                .setFields("files(id, name, mimeType)")
                .setPageToken(pageToken)
                .setQ(filter).execute()
            files.addAll(result.files.filterNotNull())
            pageToken = result.nextPageToken
        } while (pageToken != null)
        return files
    }

    fun backup(rotmappenavn: String) {
        val service = kopleMotGoogleDrive()

        val mapper = mutableListOf(finnRotmappe(service, rotmappenavn) to rotmappenavn)
        mapper.addAll(nesteUndernivaa(mapper, service))

        mapper.forEach { (_, path) ->  Paths.get("backup/$path").createDirectories() }

        mapper.forEach { mappe ->
            val underfiler = finnFiler(service, "mimeType != '$TYPE_MAPPE' and '${mappe.first.id}' in parents")
            underfiler.forEach { fil -> lagreFil(fil, mappe, service) }
        }
    }

    private fun lagreFil(
        fil: File,
        mappe: Pair<File, String>,
        service: Drive
    ) {
        if (fil.mimeType != "application/octet-stream") {
            FileOutputStream("backup/${mappe.second}/${fil.name}.pdf").use { stream ->
                service.files().export(fil.id, "application/pdf").executeMediaAndDownloadTo(stream)
            }
        }
    }

    private fun nesteUndernivaa(foreldrenivaa: List<Pair<File, String>>, service: Drive): List<Pair<File, String>> =
        foreldrenivaa.flatMap { nivaa1 ->
            val undernivaa = finnUndermapper(service, nivaa1.first).map { it to nivaa1.second + "/" + it.name }
            undernivaa + nesteUndernivaa(undernivaa, service)
        }
}