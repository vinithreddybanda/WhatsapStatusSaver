# Design system

## Direction
An image-first private gallery with an editorial, tactile visual identity. The photographs are the content; interface chrome stays quiet. Use a warm paper palette in light mode and deep graphite in dark mode. Both themes share identical contrast hierarchy, spacing, geometry and motion.

## Materials
- One sampled glass layer at a time. The dock samples the moving gallery; the action state blurs the gallery once and places a denser translucent sheet above it.
- No decorative outlines, bright rims, repeated borders or glass-on-glass stacks.
- Glass is made from backdrop blur, low-opacity tonal tint, a soft vertical light gradient and a restrained optical highlight. It must never read as a frosted rectangle with a white stroke.
- Dark glass uses charcoal translucency, not white overlays. Keep edge contrast low so the surface melts into the background.
- On Android 12, use the backdrop blur plus gradient/highlight approximation. Do not claim shader refraction on API 31.

## Type
- Editorial display: bold italic serif for the main screen title and a few high-value moments.
- Functional copy: neutral sans serif with compact line-height and consistent weight.
- No decorative overlines, fake metadata or redundant labels.

## Icons
- Lucide outline vectors only. No Material icons, emoji glyphs, mixed icon families or filled generic system symbols.
- Keep optical size between 18–22dp and use one consistent stroke language.

## Motion
- Motion should explain state, not decorate it.
- Use interruptible springs with restrained damping for the dock indicator and gesture-following surfaces.
- Use short eased fades and vertical travel for modal transitions. No scale-pop on the whole screen, no rubbery bounce on long press, and no repeated shimmer loops.
- Press feedback stays inside the clipped media surface so it never exposes a bright underlay edge.
- Prefer draw-phase transforms for continuous motion and avoid relayout on every animation frame.
- Keep gestures direct: swipe between tabs, drag the dock indicator, pull the action sheet down to dismiss.

## Screen
- Full-bleed staggered media grid with natural image ratios, cached/downsampled thumbnails and stable keys.
- Compact floating icon-only navigation for All, Photos, Videos and Saved.
- A feathered top fade blends the status-bar/cutout region into the gallery rather than creating a hard toolbar edge.
- The action sheet retains the selected thumbnail and the three core actions.
- Search and reload controls stay out of the primary surface.
- Local device storage only. No remote media service or backend.

## Accessibility and performance
- Every icon-only action has a meaningful content description.
- Respect system light/dark icon contrast and edge-to-edge insets.
- Avoid unnecessary recomposition, unbounded blur and per-thumbnail shader work.
- Validate unit tests, GitHub debug APK and F-Droid release APK. Device testing is still required for OEM gesture behavior and frame pacing.
