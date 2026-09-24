# HOTFOX V9 — SCREEN-BY-SCREEN EXECUTION CHECKLIST

Use this checklist during implementation and again before final V9 reporting.

A screen is NOT complete because it compiles. A screen is complete only after implementation, emulator capture, comparison, and explicit review.

---

## Global gates before touching screens

- [ ] Current branch = `cursor/hotfox-ui-pixel-lock-rebuild`
- [ ] Starting HEAD recorded
- [ ] V8 evidence preserved
- [ ] V9 art-director review read
- [ ] V9 master prompt read
- [ ] Approved reference boards available
- [ ] ru-RU capture path confirmed
- [ ] `src/debug` fixture containment confirmed
- [ ] production VPN/security code not selected for visual refactor
- [ ] planet source/component audited
- [ ] transparent fox source audited

## Global visual laws

- [ ] Near-black warm-violet background consistent
- [ ] Cream primary typography, not harsh pure white
- [ ] Muted copy readable
- [ ] Orange restrained and contextual
- [ ] Green only success/health semantics
- [ ] One coherent rounded-outline icon family
- [ ] No generic Material defaults visible as final design
- [ ] No glass/neon/cyberpunk/gamer treatment
- [ ] No giant-card zoo
- [ ] No raw stock-Android preference-list look
- [ ] No content hidden under nav/CTA
- [ ] No unintended scrollbar in approval captures
- [ ] No mixed-language ru-RU UI

## Planet global laws

Planet required on 01/02/05/06/07/09; 08 inherits 05.

For each planet screen:
- [ ] Fox and planet are separate layers
- [ ] Planet has filled dark body
- [ ] One principal copper/orange rim
- [ ] No hard second parallel rim
- [ ] Rim soft/faded at endpoints
- [ ] Fox occludes horizon naturally
- [ ] No matte rectangle around fox
- [ ] No stars
- [ ] No orbit rings
- [ ] No radar circles
- [ ] No satellites
- [ ] No sci-fi poster styling
- [ ] Planet does not collide with title/body/CTA
- [ ] Planet supports composition rather than becoming a second hero

---

# 01 — Splash

- [ ] No normal header/nav
- [ ] Planet behind fox retained
- [ ] Planet reads atmospheric, not like a wire arc
- [ ] Fox scale/crop matches reference feeling
- [ ] Hero and HotFox wordmark feel like one composition
- [ ] Tagline is quiet and intentional
- [ ] Loading bar is thin/subordinate
- [ ] Large empty zones feel intentional
- [ ] No production fake delay added
- [ ] Debug harness can hold frame for capture
- [ ] Actual V9 screenshot captured
- [ ] Side-by-side generated
- [ ] Overlay generated
- [ ] Diff generated
- [ ] Manual score recorded

# 02 — Onboarding Connect

- [ ] Two-line Russian title intentional
- [ ] Body width/line breaks intentional
- [ ] Planet retained and refined
- [ ] Fox does not feel inserted as a standalone image
- [ ] Hero mass integrates with copy and actions
- [ ] Primary CTA correct weight
- [ ] Secondary `Купить доступ` readable and deliberate
- [ ] No fake subscription/access state
- [ ] Actual V9 screenshot captured
- [ ] Comparison set generated
- [ ] Manual score recorded

# 03 — AUTO

- [ ] Concept NOT redesigned
- [ ] No planet
- [ ] No globe/orbit system introduced
- [ ] Route lines optically consistent
- [ ] Node sizes balanced
- [ ] Selected path restrained orange
- [ ] Title wrap exact
- [ ] CTA alignment exact
- [ ] Secondary action spacing exact
- [ ] No regression from V8
- [ ] Actual + comparison set complete

# 04 — Ready

- [ ] Concept NOT redesigned
- [ ] No planet
- [ ] Server/check illustration preserved
- [ ] No radar/orbit circles
- [ ] Title/body spacing refined
- [ ] Permission note readable but quiet
- [ ] CTA visually closes page
- [ ] Real Android permission flow preserved
- [ ] No fake permission result
- [ ] Actual + comparison set complete

# 05 — Disconnected Home

- [ ] Planet retained/refined
- [ ] Title strong but controlled
- [ ] Subtitle clearly secondary
- [ ] Hero crop/scale reference-aware
- [ ] `Нет разрешения VPN` truthful and productized
- [ ] Connect CTA primary but not oversized
- [ ] Four-row option group lighter than generic settings card
- [ ] Icon column aligned
- [ ] Right value column aligned
- [ ] Separators thin and consistent
- [ ] Smart subtitle fully readable
- [ ] Helper notice quiet/editorial
- [ ] Gear optical size correct
- [ ] Bottom-nav active item matches exact reference
- [ ] No content overlap
- [ ] Actual + comparison set complete

# 06 — Connecting

- [ ] Planet retained as static background only
- [ ] No circular progress/orbit around fox
- [ ] Progress rail is separate from hero
- [ ] Active/pending stages visually clear
- [ ] `Выбираем сервер…` active treatment restrained
- [ ] Lower settings visually de-emphasized
- [ ] Stop control neutral and controlled
- [ ] No destructive styling unless semantics demand it
- [ ] Real cancel/disconnect behavior preserved
- [ ] Actual + comparison set complete

# 07 — Protected

- [ ] Planet retained/refined
- [ ] `Защищено` uses soft green only
- [ ] Timer supports status
- [ ] Hero calmer than disconnected page
- [ ] Download/upload columns aligned
- [ ] 0MB state quiet
- [ ] User → HotFox → Internet route readable
- [ ] Disconnect action neutral
- [ ] Option group aligned with 05 system
- [ ] Debug protected fixture contains no contradictory permission warning
- [ ] Production CONNECTED gate untouched
- [ ] Bottom-nav active label exact to reference
- [ ] Actual + comparison set complete

# 08 — Add Connection Sheet

- [ ] Real 05 page remains behind sheet
- [ ] Fox+planet background remains coherent
- [ ] Scrim darkens without muddying orange hue
- [ ] Handle centered/subtle
- [ ] Title not oversized
- [ ] Close icon visible size optically correct
- [ ] Close hit target >=48dp
- [ ] Rows consistent height
- [ ] Leading icons aligned
- [ ] Titles aligned
- [ ] Chevrons aligned
- [ ] Radius/border feel HotFox, not stock Material
- [ ] Back/drag/close behavior works
- [ ] Accessibility focus preserved
- [ ] Actual + comparison set complete

# 09 — HTTPS Subscription

- [ ] Form is primary
- [ ] Planet retained but lower contrast than home
- [ ] Fox+planet supports trust/privacy, not decoration
- [ ] Header/back alignment exact
- [ ] Title/supporting copy grouped tightly
- [ ] URL field native semantics preserved
- [ ] Paste action integrated
- [ ] Focus/error state available
- [ ] Helper/privacy note aligned
- [ ] CTA contextual and not oversized
- [ ] Blank lower field reduced
- [ ] No secret URL in screenshot/log/report
- [ ] No fake successful subscription
- [ ] Actual + comparison set complete

# 10 — Servers

- [ ] No planet
- [ ] Header + gear exact
- [ ] Title spacing exact
- [ ] Filter labels readable
- [ ] No awkward wrapping
- [ ] `Мои серверы` treatment exact
- [ ] Underline thin/restrained
- [ ] AUTO row title not unnecessarily truncated
- [ ] Flag column aligned
- [ ] City/country hierarchy consistent
- [ ] Ping column aligned
- [ ] Ping neutral unless reference proves otherwise
- [ ] Chevron column aligned
- [ ] Separators consistent
- [ ] Debug server fixture remains debug-only
- [ ] Production catalog truthful
- [ ] Bottom nav active Servers
- [ ] Actual + comparison set complete

# 11 — Server Details

- [ ] No planet
- [ ] ru-RU visible copy audited
- [ ] Back + HotFox header exact
- [ ] Server identity block deliberate
- [ ] Status row aligned
- [ ] Load row aligned
- [ ] Routing row aligned
- [ ] Shadow row aligned
- [ ] Auto-select row aligned
- [ ] Leading icon optical size consistent
- [ ] Right value column readable/consistent
- [ ] No giant card added just to fill space
- [ ] Explanatory note correct measure
- [ ] Disabled/select action readable
- [ ] Fixture values safe/debug-only
- [ ] No secrets
- [ ] Actual + comparison set complete

# 12 — Subscription

- [ ] No planet
- [ ] Root header exact
- [ ] Plan identity strong
- [ ] Entitlement/status surface premium but restrained
- [ ] Expiry value aligned
- [ ] Device/server values aligned
- [ ] Primary management action clear
- [ ] Secondary renew action quieter
- [ ] HTTPS subscription action placed intentionally
- [ ] Legal/helper copy readable
- [ ] Lower space feels deliberate
- [ ] No fake entitlement/payment in production
- [ ] Debug state remains debug-only
- [ ] Actual + comparison set complete

# 13 — Settings

- [ ] No planet
- [ ] Header exact
- [ ] Title/body footprint compact
- [ ] Section eyebrows consistent
- [ ] Row heights consistent
- [ ] Icon visual sizes optically equal
- [ ] Chevron column exact
- [ ] Hairlines/group spacing provide structure
- [ ] No heavy card zoo
- [ ] No raw stock-Android list feel
- [ ] No scrollbar in accepted capture
- [ ] Bottom nav exact
- [ ] All row navigation callbacks preserved
- [ ] Actual + comparison set complete

# 14 — Smart Routing

- [ ] No planet
- [ ] Entire visible approval capture consistently ru-RU
- [ ] No stray `Smart Routing` title if localized product copy should be Russian
- [ ] No stray `Custom rules` row if localization exists
- [ ] Top-level mode clearly primary
- [ ] Apps/rules navigation clear
- [ ] DNS group clear
- [ ] LAN switch integrated
- [ ] IPv6 group clear
- [ ] Custom/domain rules clear
- [ ] Reconnect behavior clear
- [ ] Right values aligned
- [ ] CTA connected to control flow
- [ ] Supporting note quiet
- [ ] Real routing/DNS/LAN/IPv6 semantics preserved
- [ ] Actual + comparison set complete

# 15 — Apps & Rules

- [ ] No planet
- [ ] Header exact
- [ ] Title wrap intentional
- [ ] Intro copy concise
- [ ] Segmented control exact geometry
- [ ] Selected segment orange restrained
- [ ] Search field exact height/radius/icon
- [ ] App icons consistent and recognizable in debug fixture
- [ ] App row height consistent
- [ ] Title/package/category hierarchy clear
- [ ] Right selection column exact
- [ ] Checked state restrained
- [ ] Unchecked outline consistent
- [ ] No scrollbar in approval capture
- [ ] No content clipped by save action
- [ ] `Пользовательские правила` summary integrated
- [ ] `Выбрано` summary integrated
- [ ] Save CTA properly weighted
- [ ] Production installed apps remain real
- [ ] Debug fixtures remain debug-only
- [ ] Search/IME/accessibility preserved
- [ ] Actual + comparison set complete

# 16 — Autopilot

- [ ] No planet
- [ ] Header/title/body compact
- [ ] Master switch clearly primary
- [ ] Wi-Fi rule grouping clear
- [ ] Mobile rule grouping clear
- [ ] Pause behavior clear
- [ ] Captive portal/activation behavior clear if present
- [ ] Right values aligned
- [ ] Helper/status copy integrated
- [ ] CTA belongs to flow
- [ ] No giant dead lower field
- [ ] Persisted production behavior unchanged
- [ ] Actual + comparison set complete

# 17 — Shadow

- [ ] No planet
- [ ] No fox
- [ ] No orbit
- [ ] No circular bands around shield
- [ ] Topology visually intentional
- [ ] Nodes purposeful
- [ ] No symmetric starburst
- [ ] No path awkwardly stabbing shield
- [ ] Shield stroke refined
- [ ] Orange edge restrained
- [ ] Body copy measured/narrow
- [ ] Status row aligned
- [ ] Mode row aligned
- [ ] Activation row aligned if present
- [ ] Compatibility row aligned if present
- [ ] Details row aligned if present
- [ ] CTA contextual, not giant onboarding button
- [ ] Real Shadow semantics preserved
- [ ] Visible copy consistently ru-RU
- [ ] Actual + comparison set complete

# 18 — Always-on VPN

- [ ] No planet
- [ ] No fox
- [ ] Header exact
- [ ] Shield scale/position refined
- [ ] Always-on row aligned
- [ ] Block-without-VPN row aligned
- [ ] Right values truthful/readable
- [ ] Android Settings handoff CTA contextual
- [ ] Helper note measured correctly
- [ ] Bottom nav presence matches exact reference
- [ ] No fake Android lockdown status
- [ ] Actual + comparison set complete

---

# V9 localization audit final gate

- [ ] 01 ru-RU
- [ ] 02 ru-RU
- [ ] 03 ru-RU
- [ ] 04 ru-RU
- [ ] 05 ru-RU
- [ ] 06 ru-RU
- [ ] 07 ru-RU
- [ ] 08 ru-RU
- [ ] 09 ru-RU
- [ ] 10 ru-RU
- [ ] 11 ru-RU
- [ ] 12 ru-RU
- [ ] 13 ru-RU
- [ ] 14 ru-RU with no accidental English utility labels
- [ ] 15 ru-RU
- [ ] 16 ru-RU
- [ ] 17 ru-RU
- [ ] 18 ru-RU
- [ ] Intentional English brand/protocol exceptions documented

# Engineering final gate

- [ ] `:app:assemblePlaystoreDebug` PASS
- [ ] `:app:testPlaystoreDebugUnitTest` PASS
- [ ] `static_check_2_2_0.py` PASS
- [ ] `verification/ui_rebuild/check_no_fixture_leak_v51.py` PASS
- [ ] lint executed if environment supports it; result documented
- [ ] No production fixture leak
- [ ] No fake production CONNECTED
- [ ] No fake production servers
- [ ] No fake production subscription/payment
- [ ] No fake production traffic/latency
- [ ] VPN datapath unchanged
- [ ] DNS/IPv6 fail-closed semantics unchanged
- [ ] routing semantics unchanged
- [ ] Shadow semantics unchanged
- [ ] Autopilot semantics unchanged

# Evidence final gate

- [ ] actual_v9/01..18 exist
- [ ] side_by_side/01..18 exist
- [ ] overlay/01..18 exist
- [ ] diff/01..18 exist
- [ ] V9 pixel results CSV exists
- [ ] V9 art-director score CSV exists
- [ ] V9 locale proof exists
- [ ] V9 planet composition audit exists
- [ ] V9 final report exists
- [ ] V9 contact sheet generated
- [ ] 03/04 anchor regression explicitly reviewed
- [ ] remaining deviations reported honestly

# Final status law

- [ ] Do not merge from this task
- [ ] Do not claim pixel-perfect without evidence
- [ ] Do not claim RELEASE READY from UI work
- [ ] Physical/runtime VPN E2E status reported honestly
