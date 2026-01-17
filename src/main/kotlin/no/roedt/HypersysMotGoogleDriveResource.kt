package no.roedt

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/integrer")
class HypersysMotGoogleDriveResource(
    val hypersysService: HypersysService,
    val googleDriveService: GoogleDriveService
) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun integrer(): Map<String, List<String?>> {
//        return hypersysService.hentFraHypersys()

        giTilgangTilMappe()

        return mapOf()
    }

    private fun giTilgangTilMappe() {
        val service = googleDriveService.kopleMotGoogleDrive()

        service.files().list().execute().files.forEach { file ->
            service.permissions().create(file.id, googleDriveService.giTilgang("raudtosloteknisk@gmail.com")).execute()
        }
    }
}