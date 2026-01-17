package no.roedt

import io.quarkus.arc.properties.IfBuildProperty
import jakarta.enterprise.context.Dependent
import no.roedt.hypersys.GyldigSystemToken
import no.roedt.hypersys.HypersysRestClient
import no.roedt.hypersys.externalModel.Organisasjonsledd
import org.eclipse.microprofile.rest.client.inject.RestClient
import kotlin.io.encoding.Base64

interface HypersysService {
    fun hentFraHypersys(): Map<String, List<String?>>
}

@Dependent
@IfBuildProperty(name = "hypersys.ekte", stringValue = "true")
class EkteHypersysService(
    @RestClient val hypersysKlient: HypersysRestClient,
    val secretFactory: SecretFactory,
) : HypersysService {
    override fun hentFraHypersys(): Map<String, List<String?>> {
        val bearerToken = "Bearer ${hentBearerToken().access_token}"

        val alleLag = hypersysKlient.hentAlleLokallag(bearerToken)

        val lagOgEposter = hypersysKlient.hentAlleLokallag(bearerToken)
            .filter { it.parent == alleLag.single { l -> l.name == "Rødt Oslo" }.id }
            .associate { it.name to finnEposter(bearerToken, it) }
        // TODO: Den tomme return-en her er for å sikre at vi ikkje ved eit uhell faktisk lager ekte data
        return mapOf()
    }

    private fun finnEposter(
        bearerToken: String,
        organisasjonsledd: Organisasjonsledd,
    ): List<String?> {
        val organsFraHS = hypersysKlient.hentAlleOrgan(token = bearerToken, orgId = organisasjonsledd.id.toString())

        val detaljerOsloorgan = organsFraHS["organs"]!!
            .filter { it.organ_type == "Lagsstyre" }
            .map {
                hypersysKlient.hentOrgan(
                    token = bearerToken,
                    orgId = it.id.toString(),
                    organId = it.id.toString()
                )
            }
            .singleOrNull { it.members.isNotEmpty() }

        val eposter = detaljerOsloorgan?.members?.map { m -> m.email }?.distinct() ?: listOf()
        return eposter
    }

    private fun hentBearerToken(): GyldigSystemToken {
        val id = secretFactory.getHypersysClientId()
        val secret = secretFactory.getHypersysClientSecret()
        return hypersysKlient.tokenSystem(base64Credentials = "Basic ${Base64.encode("$id:$secret".toByteArray())}")
    }
}

@Dependent
@IfBuildProperty(name = "hypersys.ekte", stringValue = "false")
class FakeHypersysService : HypersysService {
    override fun hentFraHypersys(): Map<String, List<String?>> = mapOf("Testlag2" to listOf("raudtosloteknisk@gmail.com"))

}