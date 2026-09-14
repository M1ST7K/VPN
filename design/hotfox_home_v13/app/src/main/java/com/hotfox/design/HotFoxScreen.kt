package com.hotfox.design

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class HotFoxPhase { Disconnected, Connecting, Connected }
enum class HotFoxTab { Home, Servers, Subscription, Settings }
enum class HotFoxArtwork { UnifiedScene, SeparateLayers }
enum class HotFoxIcon {
    Power, Shield, ShieldCheck, Crown, Globe, Route, Lock, Timer,
    Download, Upload, Servers, Settings, Home, Chevron, Stop, Dots
}

/** IDs are supplied by the host app; this file does not assume its R namespace. */
data class HotFoxAssets(
    @DrawableRes val heroScene: Int,
    @DrawableRes val planetBackground: Int,
    @DrawableRes val fox: Int,
    @DrawableRes val brandMark: Int,
    @DrawableRes val germanyFlag: Int,
    val icons: Map<HotFoxIcon, Int>,
)

/** All connection, timer, subscription and traffic data belong to the host app. */
data class HotFoxUiState(
    val phase: HotFoxPhase,
    val serverTitle: String = "Автовыбор",
    val serverSubtitle: String = "Лучший сервер для вас",
    @DrawableRes val serverFlag: Int? = null,
    val latencyMs: Int? = null,
    val elapsed: String = "00:00:00",
    val downloaded: String = "0 МБ",
    val uploaded: String = "0 МБ",
    val connectingStep: String = "Проверяем соединение",
    val shadowEnabled: Boolean = false,
    val smartLabel: String = "Весь трафик",
    val subscriptionLabel: String = "Управление доступом",
    val isPro: Boolean = true,
    val selectedTab: HotFoxTab = HotFoxTab.Home,
)

object HotFoxColors {
    val Background = Color(0xFF0B0B10)
    val Card = Color(0xEE121319)
    val Text = Color(0xFFF6F6F8)
    val Muted = Color(0xFFABAABB)
    val Orange = Color(0xFFFF813B)
    val Green = Color(0xFF39DE83)
    val Hairline = Color(0xFF292A34)
}

/**
 * Native reference implementation of the approved 853 x 1844 design.
 * Place edge-to-edge at the Activity root; this component applies safeDrawing
 * once and uses real system bars. Do not add Scaffold padding a second time.
 * On short displays the composition scrolls. At larger font sizes the host
 * should use its adaptive layout while keeping these assets and design tokens.
 * Requires Compose foundation + ui, without Material icons or network loaders.
 */
@Composable
fun HotFoxScreen(
    state: HotFoxUiState,
    assets: HotFoxAssets,
    onPrimaryAction: () -> Unit,
    onServers: () -> Unit,
    onSubscription: () -> Unit,
    onShadowChange: (Boolean) -> Unit,
    onSmart: () -> Unit,
    onNavigate: (HotFoxTab) -> Unit,
    modifier: Modifier = Modifier,
    artwork: HotFoxArtwork = HotFoxArtwork.UnifiedScene,
) {
    BoxWithConstraints(
        modifier.fillMaxSize().background(HotFoxColors.Background).safeDrawingPadding()
    ) {
        val s = maxWidth.value / 853f
        val designHeight = 1710f * s
        val height = maxOf(designHeight, maxHeight.value)
        val extra = (height - designHeight) / s
        // The reference's system bar areas are excluded from the content origin.
        fun at(x: Float, y: Float, w: Float, h: Float) = Modifier
            .offset((x * s).dp, ((y - 80f) * s).dp).size((w * s).dp, (h * s).dp)
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(height.dp).clipToBounds()) {
                val background = if (artwork == HotFoxArtwork.UnifiedScene) assets.heroScene else assets.planetBackground
                Image(
                    painterResource(background), contentDescription = null,
                    modifier = at(0f, 0f, 853f, 1844f),
                    contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter,
                )
                if (artwork == HotFoxArtwork.SeparateLayers) {
                    Image(painterResource(assets.fox), null, at(100f, 390f, 600f, 455f), contentScale = ContentScale.Fit)
                }

                Row(at(47f, 96f, 400f, 76f), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(assets.brandMark), null, Modifier.size((64 * s).dp, (72 * s).dp))
                    Spacer(Modifier.width((18 * s).dp))
                    Txt("Hot", 48f, s, weight = FontWeight.Bold)
                    Txt("Fox", 48f, s, HotFoxColors.Orange, FontWeight.Bold)
                }
                if (state.isPro) {
                    Row(
                        at(666f, 101f, 147f, 66f).clip(RoundedCornerShape((33 * s).dp))
                            .border((1.5f * s).dp, HotFoxColors.Hairline, RoundedCornerShape((33 * s).dp))
                            .clickable(role = Role.Button, onClick = onSubscription),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Glyph(assets, HotFoxIcon.Crown, 34f, s, HotFoxColors.Orange)
                        Spacer(Modifier.width((12 * s).dp)); Txt("PRO", 30f, s, HotFoxColors.Orange)
                    }
                }

                Status(state.phase, assets, s, at(295f, 188f, 264f, 66f))
                val title = when (state.phase) {
                    HotFoxPhase.Disconnected -> "Не защищено"
                    HotFoxPhase.Connecting -> "Подключаем…"
                    HotFoxPhase.Connected -> "Вы защищены"
                }
                Box(at(30f, 276f, 793f, 65f), contentAlignment = Alignment.Center) {
                    Txt(title, 58f, s, weight = FontWeight.Bold, align = TextAlign.Center)
                }
                Row(at(30f, 348f, 793f, 43f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (state.phase == HotFoxPhase.Connected) {
                        Glyph(assets, HotFoxIcon.Timer, 33f, s, HotFoxColors.Muted)
                        Spacer(Modifier.width((14 * s).dp))
                    }
                    Txt(when (state.phase) {
                        HotFoxPhase.Disconnected -> "Подключитесь, чтобы защитить трафик"
                        HotFoxPhase.Connecting -> "Создаём защищённое соединение"
                        HotFoxPhase.Connected -> "В сети ${state.elapsed}"
                    }, 29f, s, HotFoxColors.Muted, align = TextAlign.Center)
                }

                Box(at(70f, 823f, 713f, 77f), contentAlignment = Alignment.Center) {
                    when (state.phase) {
                        HotFoxPhase.Disconnected -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Glyph(assets, HotFoxIcon.Lock, 40f, s, HotFoxColors.Muted)
                            Spacer(Modifier.width((22 * s).dp)); Txt("Одно касание до защиты", 26f, s, HotFoxColors.Muted)
                        }
                        HotFoxPhase.Connecting -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Txt(state.connectingStep, 26f, s, HotFoxColors.Muted)
                            Spacer(Modifier.height((16 * s).dp)); LoadingLine(s)
                        }
                        HotFoxPhase.Connected -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Traffic(assets, HotFoxIcon.Download, "Загружено", state.downloaded, HotFoxColors.Green, s)
                            Spacer(Modifier.width((42 * s).dp))
                            Box(Modifier.width((1.5f * s).dp).height((45 * s).dp).background(HotFoxColors.Hairline))
                            Spacer(Modifier.width((42 * s).dp))
                            Traffic(assets, HotFoxIcon.Upload, "Отправлено", state.uploaded, HotFoxColors.Orange, s)
                        }
                    }
                }
                PrimaryButton(state.phase, assets, s, at(44f, 916f, 765f, 114f), onPrimaryAction)

                Card(at(34f, 1056f, 785f, 208f), s, onClick = onServers) {
                    Column(Modifier.fillMaxSize().padding((27 * s).dp)) {
                        Txt("С Е Р В Е Р", 19f, s, HotFoxColors.Muted)
                        Spacer(Modifier.height((23 * s).dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size((104 * s).dp).clip(RoundedCornerShape((22 * s).dp)).background(Color(0xFF2C221B)), contentAlignment = Alignment.Center) {
                                if (state.serverFlag != null) Image(painterResource(state.serverFlag), null, Modifier.size((64 * s).dp, (48 * s).dp))
                                else Glyph(assets, HotFoxIcon.Globe, 62f, s, HotFoxColors.Orange)
                            }
                            Spacer(Modifier.width((28 * s).dp))
                            Column(Modifier.weight(1f)) {
                                Txt(state.serverTitle, 32f, s, weight = FontWeight.SemiBold)
                                Spacer(Modifier.height((8 * s).dp)); Txt(state.serverSubtitle, 25f, s, HotFoxColors.Muted)
                            }
                            val green = state.phase == HotFoxPhase.Connected && state.latencyMs != null
                            Box(Modifier.clip(RoundedCornerShape((30 * s).dp)).background(if (green) Color(0xFF142D20) else Color(0xFF202129)).padding(horizontal = (20 * s).dp, vertical = (13 * s).dp)) {
                                Txt(state.latencyMs?.let { "$it мс" } ?: "AUTO", 24f, s, if (green) HotFoxColors.Green else HotFoxColors.Muted)
                            }
                            Spacer(Modifier.width((24 * s).dp)); Glyph(assets, HotFoxIcon.Chevron, 23f, s)
                        }
                    }
                }

                Card(at(34f, 1286f, 383f, 150f).toggleable(state.shadowEnabled, role = Role.Switch, onValueChange = onShadowChange), s) {
                    Row(Modifier.fillMaxSize().padding((26 * s).dp), verticalAlignment = Alignment.CenterVertically) {
                        Glyph(assets, HotFoxIcon.Shield, 55f, s)
                        Spacer(Modifier.width((22 * s).dp))
                        Column(Modifier.weight(1f)) {
                            Txt("Shadow", 31f, s, weight = FontWeight.SemiBold)
                            Spacer(Modifier.height((9 * s).dp)); Txt("Доп. защита", 24f, s, HotFoxColors.Muted)
                        }
                        Toggle(state.shadowEnabled, s)
                    }
                }
                Card(at(437f, 1286f, 382f, 150f), s, onClick = onSmart) {
                    Row(Modifier.fillMaxSize().padding((27 * s).dp), verticalAlignment = Alignment.CenterVertically) {
                        Glyph(assets, HotFoxIcon.Route, 58f, s)
                        Spacer(Modifier.width((24 * s).dp))
                        Column(Modifier.weight(1f)) {
                            Txt("Smart", 31f, s, weight = FontWeight.SemiBold)
                            Spacer(Modifier.height((9 * s).dp)); Txt(state.smartLabel, 24f, s, HotFoxColors.Muted)
                        }
                        Glyph(assets, HotFoxIcon.Chevron, 23f, s, HotFoxColors.Muted)
                    }
                }
                Card(at(34f, 1457f, 785f, 150f), s, warm = true, onClick = onSubscription) {
                    Row(Modifier.fillMaxSize().padding((26 * s).dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size((104 * s).dp).clip(RoundedCornerShape((22 * s).dp)).background(Color(0xFF382419)), contentAlignment = Alignment.Center) {
                            Glyph(assets, HotFoxIcon.Crown, 59f, s, HotFoxColors.Orange)
                        }
                        Spacer(Modifier.width((30 * s).dp))
                        Column(Modifier.weight(1f)) {
                            Txt("Подписка", 32f, s, weight = FontWeight.SemiBold)
                            Spacer(Modifier.height((8 * s).dp)); Txt(state.subscriptionLabel, 25f, s, HotFoxColors.Muted)
                        }
                        Glyph(assets, HotFoxIcon.Chevron, 23f, s)
                    }
                }

                Row(at(0f, 1644f + extra, 853f, 145f).background(Color(0xC90B0B10)).padding(horizontal = (20 * s).dp, vertical = (13 * s).dp)) {
                    val tabs = listOf(Triple(HotFoxTab.Home, HotFoxIcon.Home, "Главная"), Triple(HotFoxTab.Servers, HotFoxIcon.Servers, "Серверы"), Triple(HotFoxTab.Subscription, HotFoxIcon.Crown, "Подписка"), Triple(HotFoxTab.Settings, HotFoxIcon.Settings, "Настройки"))
                    tabs.forEach { (tab, icon, label) ->
                        val selected = state.selectedTab == tab
                        Box(Modifier.weight(1f).fillMaxHeight()
                            .selectable(selected, role = Role.Tab, onClick = { onNavigate(tab) }), contentAlignment = Alignment.Center) {
                            Column(Modifier.width((140 * s).dp).fillMaxHeight().clip(RoundedCornerShape((24 * s).dp))
                                .background(if (selected) Color(0xFF201710) else Color.Transparent),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Glyph(assets, icon, 43f, s, if (selected) HotFoxColors.Orange else HotFoxColors.Muted)
                                Spacer(Modifier.height((16 * s).dp)); Txt(label, 23f, s, if (selected) HotFoxColors.Orange else HotFoxColors.Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Txt(text: String, px: Float, s: Float, color: Color = HotFoxColors.Text, weight: FontWeight = FontWeight.Normal, align: TextAlign = TextAlign.Start) {
    BasicText(text, style = TextStyle(color = color, fontFamily = FontFamily.SansSerif, fontWeight = weight,
        fontSize = (px * s).sp, lineHeight = (px * 1.16f * s).sp, textAlign = align,
        platformStyle = PlatformTextStyle(includeFontPadding = false)))
}

@Composable
private fun Glyph(a: HotFoxAssets, icon: HotFoxIcon, px: Float, s: Float, tint: Color = HotFoxColors.Text) {
    Image(painterResource(requireNotNull(a.icons[icon]) { "Missing PNG for $icon" }), null,
        Modifier.size((px * s).dp), colorFilter = ColorFilter.tint(tint), contentScale = ContentScale.Fit)
}

@Composable
private fun Status(phase: HotFoxPhase, assets: HotFoxAssets, s: Float, modifier: Modifier) {
    val active = phase == HotFoxPhase.Connected
    val shape = RoundedCornerShape((33 * s).dp)
    Row(modifier.clip(shape).background(if (active) Color(0xD9123021) else Color(0xDD202027))
        .border((1.5f * s).dp, if (active) HotFoxColors.Green.copy(alpha = .75f) else Color(0xFF62616D), shape),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Glyph(assets, when (phase) { HotFoxPhase.Disconnected -> HotFoxIcon.Shield; HotFoxPhase.Connecting -> HotFoxIcon.Dots; HotFoxPhase.Connected -> HotFoxIcon.ShieldCheck },
            if (phase == HotFoxPhase.Connecting) 51f else 34f, s,
            if (active) HotFoxColors.Green else if (phase == HotFoxPhase.Connecting) HotFoxColors.Orange else HotFoxColors.Muted)
        Spacer(Modifier.width((15 * s).dp))
        Txt(when (phase) { HotFoxPhase.Disconnected -> "VPN выключен"; HotFoxPhase.Connecting -> "Подключение"; HotFoxPhase.Connected -> "VPN активен" }, 25f, s)
    }
}

@Composable
private fun Card(modifier: Modifier, s: Float, warm: Boolean = false, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape((28 * s).dp)
    val brush = Brush.horizontalGradient(if (warm) listOf(Color(0xED251A13), Color(0xF0151317)) else listOf(HotFoxColors.Card, HotFoxColors.Card))
    Box(modifier.clip(shape).background(brush)
        .border((1.5f * s).dp, if (warm) Color(0xFF60351D) else HotFoxColors.Hairline, shape)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier), content = content)
}

@Composable
private fun PrimaryButton(phase: HotFoxPhase, assets: HotFoxAssets, s: Float, modifier: Modifier, action: () -> Unit) {
    val shape = RoundedCornerShape((57 * s).dp)
    val disconnected = phase == HotFoxPhase.Disconnected
    val connecting = phase == HotFoxPhase.Connecting
    val fill = if (disconnected) listOf(Color(0xFFFF984F), Color(0xFFFF7028)) else if (connecting) listOf(Color(0xF5111218),Color(0xF5111218)) else listOf(Color(0xFF23252F),Color(0xFF191A22))
    val tint = if (disconnected) Color(0xFF0E0B08) else if (connecting) HotFoxColors.Orange else HotFoxColors.Text
    Row(modifier.clip(shape).background(Brush.horizontalGradient(fill))
        .then(if (disconnected) Modifier else Modifier.border((if (connecting) 2.5f*s else 1.5f*s).dp, if (connecting) HotFoxColors.Orange else Color(0xFF676B7E), shape))
        .clickable(role = Role.Button, onClick = action), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Glyph(assets, if (connecting) HotFoxIcon.Stop else HotFoxIcon.Power, 43f, s, tint)
        Spacer(Modifier.width((32 * s).dp)); Txt(if (disconnected) "Подключить" else if (connecting) "Отменить" else "Отключить", 40f, s, tint, FontWeight.SemiBold)
    }
}

@Composable
private fun Traffic(assets: HotFoxAssets, icon: HotFoxIcon, label: String, value: String, color: Color, s: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Glyph(assets, icon, 40f, s, color); Spacer(Modifier.width((20 * s).dp))
        Column { Txt(label, 22f, s, HotFoxColors.Muted); Spacer(Modifier.height((4 * s).dp)); Txt(value, 29f, s, weight = FontWeight.SemiBold) }
    }
}

@Composable
private fun Toggle(on: Boolean, s: Float) {
    Box(Modifier.size((76 * s).dp, (44 * s).dp).clip(RoundedCornerShape((22 * s).dp))
        .background(if (on) HotFoxColors.Orange else Color(0xFF2A2B33)).clearAndSetSemantics { }) {
        Box(Modifier.offset(((if (on) 38f else 6f) * s).dp, (6 * s).dp).size((32 * s).dp)
            .clip(RoundedCornerShape(50)).background(HotFoxColors.Text))
    }
}

@Composable
private fun LoadingLine(s: Float) {
    val transition = rememberInfiniteTransition(label = "connection-progress")
    val t by transition.animateFloat(-.35f, 1f, animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "segment-position")
    Box(Modifier.size((352 * s).dp, (8 * s).dp).clip(RoundedCornerShape(50)).background(HotFoxColors.Hairline)) {
        Box(Modifier.offset((352 * s * t).dp).size((123 * s).dp, (8 * s).dp).clip(RoundedCornerShape(50)).background(HotFoxColors.Orange))
    }
}
