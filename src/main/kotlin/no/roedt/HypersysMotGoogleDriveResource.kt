package no.roedt

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import no.roedt.google.GCPSecretFactory
import no.roedt.hypersys.HypersysRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient
import kotlin.io.encoding.Base64

@Path("/integrer")
class HypersysMotGoogleDriveResource(
    @RestClient val hypersysKlient: HypersysRestClient,
    val secretFactory: GCPSecretFactory
) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun integrer(): List<Any?> {
        val id = secretFactory.getHypersysClientId()
        val secret = secretFactory.getHypersysClientSecret()
        val creds = Base64.encode("$id:$secret".toByteArray())
        val token = hypersysKlient.tokenSystem(base64Credentials = "Basic $creds")

        val alleLag = hypersysKlient.hentAlleLokallag("Bearer ${token["access_token"]}")

        val ro = alleLag.single { (it as Map<String, *>)["name"] == "Rødt Oslo" }

        val orgId = ((ro) as Map<*, *>)["id"].toString()
        val mittLag = hypersysKlient.hentLag(token = "Bearer ${token["access_token"]}", orgId = orgId)

        val organs = (mittLag as Map<*, *>)["organs"] as List<*>

        val organsFraHS = hypersysKlient.hentAlleOrgan(token = "Bearer ${token["access_token"]}", orgId = orgId)

        val alleOrganIOslo = (((organsFraHS as Map<*,*>)["organs"]) as List<Map<*,*>>)

        val detaljerOsloorgan = alleOrganIOslo.filter { it["organ_type"] == "Fylkesstyre" }
            .map { hypersysKlient.hentOrgan(token = "Bearer ${token["access_token"]}", orgId = orgId, organId = it["id"]!!.toString()) }
            .map { it as Map<*,*> }
            .single { (it["members_list"] as List<*>).isNotEmpty() }

        val emailerIOslostyret = (detaljerOsloorgan["members_list"] as List<Map<String, *>>).map { it["email"] }



        return emailerIOslostyret
    }
}