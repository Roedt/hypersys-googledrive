package no.roedt

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
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun integrer() {
        val fraHypersys: Map<String, List<String?>> = hypersysService.hentFraHypersys()

        if (fraHypersys.size != 1 && !fraHypersys.keys.first().startsWith("Testlag")) {
            println("Skal ikkje køyre på ordentleg per no")
            throw IllegalStateException("Forventa ikkje ekte hypersysdata")
        }

        googleDriveService.giTilgangTilMappe(fraHypersys, rotmappenavn)
    }
}