package io.kdmtracker.sync

import io.kdmtracker.auth.CampaignEntity
import io.kdmtracker.auth.CampaignMemberEntity
import io.kdmtracker.auth.CampaignMemberRepository
import io.kdmtracker.auth.CampaignRepository
import io.kdmtracker.auth.UserEntity
import io.kdmtracker.auth.UserRepository
import io.kdmtracker.core.domain.Ulid
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Dev-only seed that makes the sync demo work end to end without manual setup.
 *
 * It creates a `demo` account and a campaign whose id is **exactly** the one the Android app seeds
 * locally (`Ulid.generate(0L) { 0 }`), then makes `demo` its `OWNER`. Because the app already pushes
 * to that fixed campaign id, signing in as `demo` and tapping *Sync* is enough to see pull/push
 * against the hub — no app-side configuration needed.
 *
 * Runs only under the `dev` profile (which also relaxes [io.kdmtracker.auth.JwtSecretGuard], so the
 * committed dev secret is accepted). Idempotent: re-running creates nothing twice.
 */
@Component
@Profile("dev")
class DevDataSeeder(
    private val users: UserRepository,
    private val campaigns: CampaignRepository,
    private val members: CampaignMemberRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val user =
            users.findByUsername(DEV_USERNAME)
                ?: users.save(
                    UserEntity(
                        username = DEV_USERNAME,
                        passwordHash =
                            requireNotNull(passwordEncoder.encode(DEV_PASSWORD)) {
                                "password encoder returned no hash"
                            },
                    ),
                )

        val campaignId = Ulid.generate(0L) { 0 }.value
        campaigns.findById(campaignId).orElseGet {
            campaigns.save(
                CampaignEntity(id = campaignId, name = "Demo (dev)", inviteCode = DEV_INVITE_CODE),
            )
        }

        val userId = requireNotNull(user.id)
        if (!members.existsByCampaignIdAndUserId(campaignId, userId)) {
            members.save(CampaignMemberEntity(campaignId = campaignId, userId = userId, role = OWNER_ROLE))
        }
    }

    companion object {
        const val DEV_USERNAME = "demo"
        const val DEV_PASSWORD = "demo-1234"
        const val DEV_INVITE_CODE = "DEMO-DEV"
        const val OWNER_ROLE = "OWNER"
    }
}
