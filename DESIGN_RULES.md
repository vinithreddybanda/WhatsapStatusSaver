# Design rules

## Product character
WhatsApStatusSaver is a private, image-first media utility. The media is the interface. Chrome should disappear until the user needs it.

## Visual anchor
- Primary: Apple-style liquid glass, adapted to Android edge-to-edge behavior.
- Secondary: quiet editorial minimalism. Monochrome surfaces, one restrained lime accent, generous image area.
- Dark mode: near-black green charcoal with pale foreground and soft lime.
- Light mode: warm grey-white with deep charcoal foreground and muted olive accent.
- Typography: system sans, three roles only: 24sp display, 14sp body, 12sp caption.
- Shapes: 15dp media corners, capsule dock, 18dp search field, 30dp action panel.

## Hard bans
- No Material 3 widgets, app bars, cards, chips, buttons, navigation bars, bottom sheets or progress indicators.
- No decorative section labels, counts, timestamps under thumbnails, VIDEO badges, redundant subtitles, feature copy or explanatory paragraphs.
- No visible text under bottom navigation icons.
- No button labels in the media action panel. Icon-only actions must retain accessibility descriptions.
- No borders around media tiles, gradients, neon glow, shadows on every element, or glass on every surface.
- No animation loops, shimmer loops, heavy list stagger or per-item entrance animations.

## Liquid-glass surfaces
- Use io.github.kyant0:backdrop:1.0.6.
- True backdrop sampling is reserved for the floating navigation dock and transient media action panel.
- The media grid must remain an ordinary cached image layer. Never put blur, lens or backdrop sampling on individual thumbnails or the entire scrolling grid.
- Use a subtle translucent tint, a thin highlight and restrained blur/refraction. If the effect is unsupported on an OS version, retain a translucent surface fallback.
- Keep the dock compact, floating, icon-only and consistent across tabs.

## Interaction and motion
- Press feedback: scale down slightly with an interruptible medium-bouncy spring.
- Tab selection: a single capsule glides with a low-stiffness spring; the pager follows the selected tab.
- Media action panel: enters from the bottom, keeps the selected media visible, and offers only Save/Delete, Share and WhatsApp.
- A media tap opens it; a long press opens its action panel.
- Search is hidden until requested and expands into a single compact field.
- Do not animate static content for decoration. Do not delay actions while an animation runs.
- Respect the system animator-duration scale and avoid large movement when reduced motion is enabled.

## Screen rules
- Gallery: full-bleed staggered media grid with natural aspect ratios. No captions, times, labels or per-tile menu buttons.
- Navigation: four equal icon-only destinations: All, Photos, Videos, Saved. Accessibility descriptions carry the names.
- Search: one field, no extra search page or filter chips.
- Permission: one concise explanation and one clear action. No repeated copy.
- Empty/loading: quiet and compact. Skeleton blocks match the grid; no spinner or motivational text.
- Action panel: selected thumbnail plus three icon-only actions. No title, timestamp, ACTIONS heading or close button.

## Engineering constraints
- Keep storage scans, image bounds reads and file operations off the main thread.
- Keep Coil requests cached, downsampled and without thumbnail crossfades.
- Preserve stable lazy-grid keys and intrinsic media ratios.
- Use shared design tokens from Color.kt, Type.kt and Theme.kt.
- Keep status bar transparent and visible; respect navigation and display cutout insets.
- Validate debug and F-Droid builds plus unit tests before claiming completion.
- Physical-device testing is required for actual frame pacing, gestures, permission handoff and OEM system-bar behavior.
