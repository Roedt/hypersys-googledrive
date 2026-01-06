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
    fun integrer(): Map<String, String> {
        val id = secretFactory.getHypersysClientId()
        val secret = secretFactory.getHypersysClientSecret()
        val creds = Base64.encode("$id:$secret".toByteArray())
        val token = hypersysKlient.tokenSystem(base64Credentials = "Basic $creds")


        return token
    }
}