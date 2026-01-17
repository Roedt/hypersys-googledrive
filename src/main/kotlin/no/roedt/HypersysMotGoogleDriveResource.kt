package no.roedt

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.config.inject.ConfigProperty

@Path("/integrer")
class HypersysMotGoogleDriveResource(
    val hypersysService: HypersysService,
    val googleDriveService: GoogleDriveService,
    @ConfigProperty(name = "google.drive.rotmappenavn") val rotmappenavn: String,
) {
    private val typeMappe = "application/vnd.google-apps.folder"

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun integrer() {
        val fraHypersys: Map<String, List<String?>> = hypersysService.hentFraHypersys()

        if (fraHypersys.size != 1 && !fraHypersys.keys.first().startsWith("Testlag")) {
            println("Skal ikkje køyre på ordentleg per no")
            throw IllegalStateException("Forventa ikkje ekte hypersysdata")
        }

        giTilgangTilMappe(fraHypersys)
    }

    private fun giTilgangTilMappe(lagOgFolk: Map<String, List<String?>>) {
        val service = googleDriveService.kopleMotGoogleDrive()

        val rotmappeId = finnRotmappe(service)

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

    private fun finnRotmappe(service: Drive): File = (service.files().list()
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

    private fun finnUndermapper(service: Drive, forelder: File): List<File> = service.files().list()
        .setFields("files(id, name)")
        .setQ("mimeType = '$typeMappe' and '${forelder.id}' in parents").execute()
        .files
        .filterNotNull()

    private fun giTilgangTilMappe(service: Drive, file: File, person: String) =
        service.permissions()
            .create(file.id, googleDriveService.giTilgang(person)).execute()
}