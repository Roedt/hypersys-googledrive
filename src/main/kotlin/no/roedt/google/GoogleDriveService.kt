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
                .setFields("files(id, name, mimeType, permissions)")
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

        lagMapper(mapper)
        lagreUnderfiler(mapper, service)
    }

    private fun lagMapper(mapper: MutableList<Pair<File, String>>) =
        mapper.forEach { (_, path) -> Paths.get("backup/$path").createDirectories() }

    private fun lagreUnderfiler(mapper: MutableList<Pair<File, String>>, service: Drive) =
        mapper.forEach { mappe ->
            val underfiler = finnFiler(service, "mimeType != '$TYPE_MAPPE' and '${mappe.first.id}' in parents")
            underfiler.forEach { fil -> lagreFil(fil, mappe, service) }
        }

    private fun lagreFil(fil: File, mappe: Pair<File, String>, service: Drive) {
        filtypemapping[fil.mimeType]?.let { (filtype, mimetype) ->
            FileOutputStream("backup/${mappe.second}/${fil.name}.$filtype").use { stream ->
                service.files().export(fil.id, mimetype).executeMediaAndDownloadTo(stream)
            }
        }
    }

    private fun nesteUndernivaa(foreldrenivaa: List<Pair<File, String>>, service: Drive): List<Pair<File, String>> =
        foreldrenivaa.flatMap { nivaa1 ->
            val undernivaa = finnUndermapper(service, nivaa1.first).map { it to nivaa1.second + "/" + it.name }
            undernivaa + nesteUndernivaa(undernivaa, service)
        }
}

val filtypemapping = mapOf(
    "application/octet-stream" to null, // veit ikkje heilt kva formatet er her
    "application/vnd.google-apps.document" to Pair("odt", "application/vnd.oasis.opendocument.text"),
    "application/vnd.google-apps.spreadsheet" to Pair("ods", "application/vnd.oasis.opendocument.spreadsheet"),
    "application/vnd.google-apps.presentation" to Pair("odp", "application/vnd.oasis.opendocument.presentation"),
    "application/vnd.google-apps.photo" to Pair("png", "image/png"),
)