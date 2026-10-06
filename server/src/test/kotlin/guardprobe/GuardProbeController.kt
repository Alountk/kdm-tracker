package guardprobe

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Test-only route registered from [io.kdmtracker.auth.BearerFilterTest] via `@Import`. It lives
 * outside the `io.kdmtracker` component-scan root so it is not picked up by every `@SpringBootTest`,
 * and it deliberately performs no auth check of its own: if the bearer filter lets the request
 * through, this returns 200, which proves the filter — not a controller guard — is what protects
 * newly added routes under the `/api/v1` API prefix.
 */
@RestController
class GuardProbeController {
    @GetMapping("/api/v1/guard-probe")
    fun probe(): Map<String, String> = mapOf("ok" to "true")
}
