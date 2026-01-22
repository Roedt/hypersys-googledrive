package no.roedt.google

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import no.roedt.google.GoogleDriveService.Companion.TYPE_MAPPE
import kotlin.collections.component1
import kotlin.collections.component2

object Tilgangsstyrer {
    fun giTilgang(service: Drive, lagOgFolk: Map<String, List<String?>>, undermapper: List<File>, rotmappeId: File) {

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

    private fun lagMappe(service: Drive, lag: String, rotmappeId: File) =
        service.files().create(File().also {
            it.name = lag
            it.parents = listOf(rotmappeId.id)
            it.mimeType = TYPE_MAPPE
        }).execute()

    private fun giTilgangTilMappe(service: Drive, file: File, person: String) =
        if (!file.permissions.map { it.emailAddress }.contains(person)) {
            service.permissions()
                .create(file.id, giTilgang(person)).execute()
        } else {
            println("Personen har allereie tilgang til mappe ${file.name}")
        }

    private fun giTilgang(epost: String): Permission = Permission().also {
        it.emailAddress = epost
        it.kind = "drive#permission"
        it.role = "writer"
        it.type = "user"
    }

}