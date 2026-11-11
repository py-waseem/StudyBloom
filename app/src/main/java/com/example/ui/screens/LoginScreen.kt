package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppNameStyle
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.HeadlineStyle
import com.example.ui.theme.LoginCardBackground
import com.example.ui.theme.LoginDeepPlum
import com.example.ui.theme.LoginGuestGreen
import com.example.ui.theme.LoginPureWhite
import com.example.ui.theme.LoginSoftPurple
import com.example.ui.theme.LoginWarmCream
import com.example.ui.theme.SignInActionStyle
import com.example.ui.theme.SignInPromptStyle
import com.example.ui.theme.SubtitleStyle

/**
 * Reusable PillButton matching the exact visual style:
 * - 52dp height, fully rounded pill shape (CircleShape)
 * - Minimum 48dp interactive touch target for accessibility
 * - White medium-weight text centered
 */
@Composable
fun PillButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescriptionText: String = text
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .semantics {
                role = Role.Button
                contentDescription = contentDescriptionText
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val innerTag = if (text.contains("Guest", ignoreCase = true)) {
            "welcome_continue_as_guest_button"
        } else {
            "welcome_create_account_button"
        }
        Text(
            text = text,
            style = ButtonTextStyle,
            modifier = Modifier.testTag(innerTag)
        )
    }
}

/**
 * Reusable SignInCard matching the exact visual style:
 * - Rounded rectangle with corner radius 14dp
 * - Background #F0F0F0, inner padding 12dp, no elevation
 * - Clickable with accessibility role and description
 */
@Composable
fun SignInCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LoginCardBackground)
            .semantics {
                role = Role.Button
                contentDescription = "Already have an account? Sign In"
            }
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Already have an account?",
            style = SignInPromptStyle
        )
        Text(
            text = "Sign In",
            style = SignInActionStyle,
            modifier = Modifier.testTag("welcome_sign_in_button")
        )
    }
}

/**
 * Login / Welcome screen:
 * - Top warm cream (#F8F3E1) background with sharp cut at the vertical center of "Create an Account"
 * - Bottom white (#FFFFFF) background
 * - Centered vertical layout with responsive weights
 */
@Composable
fun LoginScreen(
    onContinueAsGuest: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var splitY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val cutY = if (splitY > 0f) splitY else size.height * 0.72f
                // Top area: Warm cream #F8F3E1
                drawRect(
                    color = LoginWarmCream,
                    size = Size(size.width, cutY)
                )
                // Bottom area: Pure white #FFFFFF
                drawRect(
                    color = LoginPureWhite,
                    topLeft = Offset(0f, cutY),
                    size = Size(size.width, size.height - cutY)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Space at the top, about 22% of screen height
            Spacer(modifier = Modifier.weight(0.22f))

            // 2. App name row: the flower emoji 🌸 followed by "StudyBloom" with 8dp gap
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🌸",
                    fontSize = 36.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "StudyBloom",
                    style = AppNameStyle
                )
            }

            // 3. Space, about 8% of screen height
            Spacer(modifier = Modifier.weight(0.08f))

            // 4. Headline, two lines, centered: "Your learning" / "journey begins here"
            Text(
                text = "Your learning\njourney begins here",
                style = HeadlineStyle
            )

            // 5. Subtitle, 8dp below the headline, centered: "Your study journey starts here."
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your study journey starts here.",
                style = SubtitleStyle
            )

            // 6. Space, about 6% of screen height
            Spacer(modifier = Modifier.weight(0.06f))

            // 7. Button 1: "Continue as Guest" (Green #2E7D4F, pill shape, height 52dp)
            PillButton(
                text = "Continue as Guest",
                backgroundColor = LoginGuestGreen,
                onClick = onContinueAsGuest,
                modifier = Modifier.testTag("continue_as_guest_button"),
                contentDescriptionText = "Continue as Guest button"
            )

            // 8. Button 2, 12dp below: "Create an Account" (Soft Purple #9B7FC9)
            Spacer(modifier = Modifier.height(12.dp))
            PillButton(
                text = "Create an Account",
                backgroundColor = LoginSoftPurple,
                onClick = onCreateAccount,
                modifier = Modifier
                    .testTag("create_account_button")
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        splitY = position.y + (coordinates.size.height / 2f)
                    },
                contentDescriptionText = "Create an Account button"
            )

            // 9. Sign-in card, 16dp below: rounded rectangle (#F0F0F0, 14dp radius, 12dp padding)
            Spacer(modifier = Modifier.height(16.dp))
            SignInCard(
                onClick = onSignIn,
                modifier = Modifier.testTag("sign_in_card")
            )

            // 10. Bottom padding 24dp above the navigation bar
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
