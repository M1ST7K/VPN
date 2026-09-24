# Asset generation prompts

Built-in image_gen was used for reconstruction and initial extraction. The fox was subsequently extracted from the approved screenshot with local alpha processing explicitly authorized by the user. Rejected checkerboard generations are not shipped.

## Scene reconstruction

Use case: background-extraction. This is a production asset extraction from the provided HotFox app mockup, not a new design. Output ONE plain PNG image, full bleed, keep EXACT original tall canvas dimensions/aspect ratio and exact composition coordinates. Reconstruct the complete background artwork underneath all UI while preserving existing visible artwork with the highest possible identity and positional fidelity.
REMOVE ALL UI: status bar, time, signal wifi battery, logo and fox logo mark, HotFox wordmark, PRO badge, status chip, titles and subtitle, lock and helper line, button, cards, every icon, all text, nav bar, separators, gesture bar. No letters or interface remnants anywhere.
KEEP ONLY the same cinematic dark fox and orange planet artwork with near-black background. Fox must have identical size, head shape, expression, matte fur texture, upward-right gaze, orange rim light, and natural dark fading shoulders as input. Planet same horizon and surface. Fox face approximately x=300..615 y=410..730 in input's 853x1844 grid, fading shoulders by y=840. Planet arc remains around y=170..650. Everything beneath y=900 is an even near-black #0B0B10 suitable as background for app controls, naturally fading from artwork above. Do not fill empty lower space with new art.
No outline/stroke border on any object, no rings around fox, no new glows, no extra planets, no frame, no shadow box, no captions, no watermark, no checkerboard. Opaque background PNG is intended here. This will be directly used as one full-screen background layer at a fixed 853:1844 aspect ratio, so exact composition matters.

## Planet background

Use case: background-extraction. Extract the PLANET BACKGROUND LAYER from this HotFox screen as a production PNG. ONE tall full-bleed PNG with EXACT same original canvas aspect ratio, matching composition positions.
Keep the existing huge burnt-orange eclipse planet at the same location, same scale, realistic textured surface and thin incandescent atmospheric edge in the upper right. Preserve near-black #0B0B10 space and the subtle warm atmosphere. The planetary horizon occupies approximately y=170..650 in the original 853x1844 canvas. Fade the planet's lower surface naturally into black towards y=840; all lower half near-black.
REMOVE the entire fox character and naturally reconstruct the planetary surface/dark atmosphere underneath it.
REMOVE ABSOLUTELY EVERY UI ELEMENT: status bar, all time/numbers/letters, HotFox logo and mark, PRO, pill, title, subtitle, locks, button, cards, small icons, dividers, navigation, gesture bar. No writing or controls remain.
Do not crop or enlarge planet. No circles, orbitals, strokes, artificial object outlines, sticker border, rectangular border, frame, mockup, checkerboard or watermark. The actual physical warm atmosphere on the planetary horizon stays, not an artificial outline. Opaque near-black background is correct for this background layer. Preserve the original artwork's exact appearance, not a new painting.

## Initial extraction

Use case: background-extraction / transparent production cutout.
Extract ONLY the cinematic dark FOX portrait from the supplied HotFox mockup into a genuine transparent RGBA PNG. Keep the exact full original tall canvas dimensions/aspect ratio and original fox placement and scale, so it overlays the original scene 1:1. The canvas is transparent everywhere except the fox and its fine fur. In the reference 853x1844 grid, fox occupies roughly x=140..650, y=410..840. Do NOT center the fox in the full canvas; leave all its transparent padding intact.
Preserve the precise fox face, ears, upward-right gaze, eye, natural fur, copper-orange lit muzzle/chest, black matte fur, identity, pose and scale. Preserve the gradual soft feathered disappearance of its shoulders and lower neck: express that through ALPHA transparency at the lower/left edges, not painted opaque black fog. Keep black fur itself opaque where appropriate, do not erase fur just because it is dark.
Remove all planet, sky, haze/background, UI, text, logo, badge, button, cards, nav, helper icon, lines and every other object. This is one clean fox overlay asset only.
Absolutely no white/black sticker stroke, no outline added to cutout, no circle/ring/halo/orbits/shield, no border or frame, no drop shadow or glowing surrounding cloud. No opaque background at all, NO checkerboard drawn into pixels. True transparent alpha. Preserve existing natural orange highlights within fur only. PNG ready for direct Android Image rendering.

## Transparency retry

Create a TRUE TRANSPARENT PNG cutout of ONLY the fox from the provided reference. This is an Android production asset, not a preview of transparency. The file MUST have an actual alpha channel with alpha=0 in the entire background. Do not render any checkerboard, squares, grid, gray/white background, white fog or opaque pixels pretending to be transparent.

Reference: the black fox portrait in the upper middle of the app screenshot. Preserve that EXACT fox, same ears, muzzle, expression, black fur, warm copper highlights, natural lower-shoulder fade. Output one clean fox head-and-shoulders cutout, closely cropped with a modest empty transparent margin, roughly square image. The lower fur dissolves through semitransparent alpha. No planet, no text, no UI, no ornaments, no added outline/sticker border, no rings, no drop shadow, no external glow. Natural highlights within fur only. Need real RGBA transparency, not a transparency visualization. Background transparent.
