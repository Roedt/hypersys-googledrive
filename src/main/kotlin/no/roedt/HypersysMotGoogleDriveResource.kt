package no.roedt

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import no.roedt.google.GoogleDriveService
import no.roedt.hypersys.EkteHypersysService
import no.roedt.hypersys.HypersysService
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
@Path("/")
class HypersysMotGoogleDriveResource(
    val hypersysService: HypersysService,
    val googleDriveService: GoogleDriveService,
    @ConfigProperty(name = "google.drive.rotmappenavn") val rotmappenavn: String,
) {
    @Path("/integrer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun integrer() {
        val fraHypersys: Map<String, List<String?>> = hypersysService.hentFraHypersys("Rødt Oslo")

        if (hypersysService is EkteHypersysService) {
            println("Gjer intenting mot ekte hypersys for no, returnerer")
            return
        } else {
            println("Bruker fake hypersys. Held fram.")
        }

        if (fraHypersys.size > 1 && fraHypersys.keys.firstOrNull()?.startsWith("Testlag") == false) {
            println("Skal ikkje køyre på ordentleg per no")
            throw IllegalStateException("Forventa ikkje ekte hypersysdata")
        }

        googleDriveService.giTilgangTilMappe(fraHypersys, rotmappenavn)
    }

    @Path("/backup")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun backup() {
        googleDriveService.backup(rotmappenavn)
    }
}