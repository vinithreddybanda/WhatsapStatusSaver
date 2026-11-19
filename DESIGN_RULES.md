# Design system

## Direction
A compact, image-first private gallery. Media is the content; navigation and actions form a quiet functional layer. Use warm neutral light surfaces and deep graphite dark surfaces with matching contrast hierarchy. Functional system sans-serif typography only. No decorative calligraphy or editorial serif styling.

## Materials
- Use Liquid Glass selectively for the top header, floating tab bar and transient action dock. These are functional controls, not content cards.
- Header glass extends behind the status-bar inset. Keep system status icons theme-aware and readable over the blurred surface.
- Glass uses backdrop blur, a low-opacity tonal tint, a restrained light gradient and a soft optical highlight. Avoid bright outlines and stacked glass layers.
- The selected-media preview remains a solid, crisp image card. Do not put glass over the image itself.
- Dark glass uses charcoal translucency rather than white overlays.
- On Android 12/API 31, use the supported backdrop blur and gradient approximation. Do not claim full shader refraction.

## Icons and type
- Use one consistent bundled vector icon family. Keep icons bold enough to remain legible at navigation size.
- Use 23dp navigation/action icons and meaningful accessibility descriptions.
- Use one system sans-serif family with a clear size and weight hierarchy. No decorative calligraphy, italic display fonts, emoji glyphs or mixed icon families.

## Motion and depth
- Navigation retains its fluid, gesture-following selection lens.
- Media cards feel solid: natural image ratios, subtle elevation, a small hover lift for pointer devices and a restrained press compression for touch.
- Do not add continuous wobble, aggressive perspective tilt, cursor-tracking glare or bouncy overshoot.
- Long-press opens the action panel with a short, interruptible spring and eased fade.
- Center a large, sharp preview in the action panel. Place Save/Delete, Share and Repost together in a compact glass dock that shares the navigation material language.
- Keep animated transforms in the draw phase where possible; avoid relayout and expensive per-thumbnail shaders.

## Screen
- Full-bleed staggered media grid with natural image ratios, cached/downsampled thumbnails and stable keys.
- Compact floating icon navigation for All, Photos, Videos and Saved.
- Header, navigation and action dock share the same optical material language without making the content itself glass.
- Search and reload controls stay out of the primary surface.
- Local device storage only. No remote media service or backend.

## Accessibility and performance
- Keep status-bar and navigation-bar icon contrast correct in both themes.
- Every icon-only action has a meaningful content description.
- Provide a readable tinted fallback when backdrop blur is unavailable or reduced.
- Avoid unbounded blur and per-thumbnail shader work.
- Validate unit tests, GitHub debug APK and F-Droid release APK. Device testing is required for OEM rendering, pointer hover, gesture behavior and frame pacing.
